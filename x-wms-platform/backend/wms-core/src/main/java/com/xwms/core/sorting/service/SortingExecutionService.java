package com.xwms.core.sorting.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import com.xwms.base.location.enums.VirtualLocationType;
import com.xwms.core.sorting.entity.SortingGrid;
import com.xwms.core.sorting.entity.SortingWall;
import com.xwms.core.sorting.mapper.SortingGridMapper;
import com.xwms.core.sorting.mapper.SortingWallMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 二次分拣（播种式）执行服务 核心能力： 1. 扫描商品分播到格口 2. 格口数量校验与状态更新 3. 波次分拣完成校验 4. 分拣完成后库存转入发货暂存
 *
 * <p>业务流程： 第一次拣货完成 → 库存转入波次虚拟库位(SORT-WAVE-xxx) → 扫描商品 → 系统指示格口 → 放入格口 →
 * 库存从波次虚拟库位转入格口虚拟库位(SORT-GRID-xxx) → 所有格口完成 → 库存从格口转入发货暂存(STAGE-SHIP-xxx) → 释放播种墙
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SortingExecutionService {

    private final SortingGridMapper sortingGridMapper;
    private final SortingWallMapper sortingWallMapper;
    private final VirtualLocationService virtualLocationService;
    private final SortingExceptionService sortingExceptionService;

    /**
     * 扫描商品，执行分播
     *
     * @param waveNo 波次号
     * @param wallCode 播种墙编码
     * @param gridNo 格口号
     * @param sku 商品SKU
     * @param batchNo 批次
     * @param qty 数量（通常为1，扫码逐件分播）
     * @param warehouse 仓库
     * @return 分播结果
     */
    @Transactional(rollbackFor = Exception.class)
    public SortingResult scanAndSort(
            String waveNo,
            String wallCode,
            String gridNo,
            String sku,
            String batchNo,
            BigDecimal qty,
            String warehouse) {
        // 1. 查询格口
        SortingGrid grid =
                sortingGridMapper.selectOne(
                        new LambdaQueryWrapper<SortingGrid>()
                                .eq(SortingGrid::getWallCode, wallCode)
                                .eq(SortingGrid::getGridNo, gridNo));
        if (grid == null) {
            throw new RuntimeException("格口不存在: " + wallCode + "-" + gridNo);
        }
        if (!"BOUND".equals(grid.getStatus()) && !"SORTING".equals(grid.getStatus())) {
            throw new RuntimeException("格口状态不允许分拣: " + grid.getStatus());
        }

        // 2. 校验数量是否超限
        BigDecimal actual = grid.getActualQty() != null ? grid.getActualQty() : BigDecimal.ZERO;
        BigDecimal expected =
                grid.getExpectedQty() != null ? grid.getExpectedQty() : BigDecimal.ZERO;
        if (actual.add(qty).compareTo(expected) > 0) {
            // 数量超限，记录差异
            sortingExceptionService.handleDifference(
                    waveNo, grid.getBoundOrderNo(), sku, batchNo, "MORE", qty, "格口数量超限", warehouse);
            throw new RuntimeException("格口数量超限，期望" + expected + "，当前" + actual + "，本次" + qty);
        }

        // 3. 库存转移：波次虚拟库位 → 格口虚拟库位
        String waveVirtualLoc = VirtualLocationType.SORTING.generateCode(waveNo);
        virtualLocationService.transfer(
                waveVirtualLoc, grid.getLocationCode(), sku, batchNo, qty, warehouse, waveNo);

        // 4. 更新格口实际数量和状态
        grid.setActualQty(actual.add(qty));
        if (grid.getActualQty().compareTo(BigDecimal.ZERO) > 0) {
            grid.setStatus("SORTING");
        }
        if (grid.getActualQty().compareTo(expected) >= 0) {
            grid.setStatus("COMPLETED");
        }
        sortingGridMapper.updateById(grid);

        // 5. 判断波次是否全部完成
        boolean allCompleted = isWaveSortingComplete(wallCode);
        if (allCompleted) {
            completeWaveSorting(waveNo, wallCode, warehouse);
        }

        return new SortingResult(
                grid.getBoundOrderNo(),
                gridNo,
                grid.getActualQty(),
                grid.getExpectedQty(),
                allCompleted);
    }

    /** 判断波次是否全部完成 */
    public boolean isWaveSortingComplete(String wallCode) {
        Long incompleteCount =
                sortingGridMapper.selectCount(
                        new LambdaQueryWrapper<SortingGrid>()
                                .eq(SortingGrid::getWallCode, wallCode)
                                .in(SortingGrid::getStatus, "BOUND", "SORTING"));
        return incompleteCount != null && incompleteCount == 0;
    }

    /** 波次分拣完成：库存从格口转入发货暂存，释放播种墙 */
    @Transactional(rollbackFor = Exception.class)
    public void completeWaveSorting(String waveNo, String wallCode, String warehouse) {
        List<SortingGrid> grids =
                sortingGridMapper.selectList(
                        new LambdaQueryWrapper<SortingGrid>()
                                .eq(SortingGrid::getWallCode, wallCode)
                                .eq(SortingGrid::getStatus, "COMPLETED"));

        for (SortingGrid grid : grids) {
            // 校验数量一致性
            if (grid.getActualQty() == null
                    || grid.getExpectedQty() == null
                    || grid.getActualQty().compareTo(grid.getExpectedQty()) != 0) {
                log.error(
                        "格口数量不一致，跳过: grid={}, actual={}, expected={}",
                        grid.getGridNo(),
                        grid.getActualQty(),
                        grid.getExpectedQty());
                continue;
            }

            // 库存从格口虚拟库位转入发货暂存虚拟库位
            // 注意：这里需要按SKU和批次分别转移，简化处理为整格口转移
            virtualLocationService.onSortingComplete(
                    grid.getBoundOrderNo(),
                    warehouse,
                    grid.getLocationCode(),
                    grid.getActualQty(),
                    null,
                    null);
        }

        // 释放播种墙
        sortingWallServiceRelease(wallCode);

        log.info("波次分拣完成: wave={}, wall={}, 格口数={}", waveNo, wallCode, grids.size());
    }

    private void sortingWallServiceRelease(String wallCode) {
        SortingWall wall =
                sortingWallMapper.selectOne(
                        new LambdaQueryWrapper<SortingWall>()
                                .eq(SortingWall::getWallCode, wallCode));
        if (wall != null) {
            wall.setStatus("IDLE");
            wall.setCurrentWaveNo(null);
            sortingWallMapper.updateById(wall);
        }
        // 释放格口
        List<SortingGrid> grids =
                sortingGridMapper.selectList(
                        new LambdaQueryWrapper<SortingGrid>()
                                .eq(SortingGrid::getWallCode, wallCode));
        for (SortingGrid grid : grids) {
            grid.setStatus("EMPTY");
            grid.setBoundOrderNo(null);
            grid.setBoundWaveNo(null);
            grid.setActualQty(null);
            grid.setExpectedQty(null);
            sortingGridMapper.updateById(grid);
        }
    }

    /** 分播结果 */
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class SortingResult {
        private String orderNo;
        private String gridNo;
        private BigDecimal actualQty;
        private BigDecimal expectedQty;
        private boolean waveCompleted;
    }
}
