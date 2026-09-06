package com.xwms.core.sorting.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import com.xwms.base.location.entity.Location;
import com.xwms.base.location.enums.VirtualLocationType;
import com.xwms.base.location.mapper.LocationMapper;
import com.xwms.core.sorting.entity.SortingGrid;
import com.xwms.core.sorting.entity.SortingWall;
import com.xwms.core.sorting.mapper.SortingGridMapper;
import com.xwms.core.sorting.mapper.SortingWallMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 播种墙管理服务 核心能力： 1. 播种墙CRUD与格口初始化 2. 波次与播种墙绑定（订单→格口映射） 3. 播种墙状态管理（空闲/分拣中/维护中） 4. 格口释放与复用 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SortingWallService {

    private final SortingWallMapper sortingWallMapper;
    private final SortingGridMapper sortingGridMapper;
    private final LocationMapper locationMapper;
    private final VirtualLocationService virtualLocationService;

    /**
     * 创建播种墙并初始化格口
     *
     * @param wallCode 播种墙编码
     * @param warehouseCode 仓库
     * @param areaCode 库区
     * @param rows 行数
     * @param cols 列数
     */
    @Transactional(rollbackFor = Exception.class)
    public SortingWall createWall(
            String wallCode,
            String wallName,
            String warehouseCode,
            String areaCode,
            int rows,
            int cols) {
        // 1. 创建播种墙
        SortingWall wall = new SortingWall();
        wall.setWallCode(wallCode);
        wall.setWallName(wallName);
        wall.setWarehouseCode(warehouseCode);
        wall.setAreaCode(areaCode);
        wall.setGridRows(rows);
        wall.setGridCols(cols);
        wall.setGridCount(rows * cols);
        wall.setStatus("IDLE");
        sortingWallMapper.insert(wall);

        // 2. 批量创建格口+对应虚拟库位
        int gridIndex = 1;
        for (int r = 1; r <= rows; r++) {
            for (int c = 1; c <= cols; c++) {
                String gridNo = String.format("%02d", gridIndex);
                String virtualLocCode =
                        VirtualLocationType.GRID.generateCode(wallCode + "-" + gridNo);

                // 创建格口虚拟库位
                Location loc = new Location();
                loc.setLocationCode(virtualLocCode);
                loc.setLocationName("播种格口-" + wallCode + "-" + gridNo);
                loc.setWarehouseCode(warehouseCode);
                loc.setAreaCode(areaCode);
                loc.setLocationType("VIRTUAL");
                loc.setStatus("EMPTY");
                loc.setIsVirtual("Y");
                loc.setVirtualType("GRID");
                loc.setSortingWallCode(wallCode);
                loc.setGridNo(gridNo);
                loc.setAutoRelease("Y");
                locationMapper.insert(loc);

                // 创建格口记录
                SortingGrid grid = new SortingGrid();
                grid.setWallCode(wallCode);
                grid.setGridNo(gridNo);
                grid.setLocationCode(virtualLocCode);
                grid.setStatus("EMPTY");
                grid.setRowIndex(r);
                grid.setColIndex(c);
                sortingGridMapper.insert(grid);

                gridIndex++;
            }
        }

        log.info("创建播种墙: code={}, 格口数={}", wallCode, rows * cols);
        return wall;
    }

    /**
     * 波次绑定播种墙：将波次中的订单分配到格口
     *
     * @param waveNo 波次号
     * @param wallCode 播种墙编码
     * @param orderNos 订单列表（按顺序分配格口）
     */
    @Transactional(rollbackFor = Exception.class)
    public List<SortingGrid> bindWaveToWall(String waveNo, String wallCode, List<String> orderNos) {
        SortingWall wall =
                sortingWallMapper.selectOne(
                        new LambdaQueryWrapper<SortingWall>()
                                .eq(SortingWall::getWallCode, wallCode));
        if (wall == null) {
            throw new RuntimeException("播种墙不存在: " + wallCode);
        }
        if (!"IDLE".equals(wall.getStatus())) {
            throw new RuntimeException("播种墙正在使用中: " + wallCode + ", status=" + wall.getStatus());
        }

        // 查询空闲格口
        List<SortingGrid> emptyGrids =
                sortingGridMapper.selectList(
                        new LambdaQueryWrapper<SortingGrid>()
                                .eq(SortingGrid::getWallCode, wallCode)
                                .eq(SortingGrid::getStatus, "EMPTY")
                                .orderByAsc(SortingGrid::getGridNo));

        if (emptyGrids.size() < orderNos.size()) {
            throw new RuntimeException("播种墙格口不足，需要" + orderNos.size() + "个，可用" + emptyGrids.size());
        }

        // 订单与格口绑定
        List<SortingGrid> boundGrids = new ArrayList<>();
        for (int i = 0; i < orderNos.size(); i++) {
            SortingGrid grid = emptyGrids.get(i);
            grid.setBoundOrderNo(orderNos.get(i));
            grid.setBoundWaveNo(waveNo);
            grid.setStatus("BOUND");
            sortingGridMapper.updateById(grid);

            // 虚拟库位绑定订单
            Location loc =
                    locationMapper.selectOne(
                            new LambdaQueryWrapper<Location>()
                                    .eq(Location::getLocationCode, grid.getLocationCode()));
            if (loc != null) {
                loc.setBoundOrderNo(orderNos.get(i));
                loc.setBoundWaveNo(waveNo);
                locationMapper.updateById(loc);
            }

            boundGrids.add(grid);
        }

        // 更新播种墙状态
        wall.setStatus("SORTING");
        wall.setCurrentWaveNo(waveNo);
        sortingWallMapper.updateById(wall);

        log.info("波次绑定播种墙: wave={}, wall={}, 订单数={}", waveNo, wallCode, orderNos.size());
        return boundGrids;
    }

    /** 释放播种墙（波次分拣完成后） */
    @Transactional(rollbackFor = Exception.class)
    public void releaseWall(String wallCode) {
        // 释放所有格口
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

            // 释放虚拟库位
            virtualLocationService.releaseIfEmpty(grid.getLocationCode());
        }

        // 更新播种墙状态
        SortingWall wall =
                sortingWallMapper.selectOne(
                        new LambdaQueryWrapper<SortingWall>()
                                .eq(SortingWall::getWallCode, wallCode));
        if (wall != null) {
            wall.setStatus("IDLE");
            wall.setCurrentWaveNo(null);
            sortingWallMapper.updateById(wall);
        }

        log.info("播种墙已释放: {}", wallCode);
    }

    /** 查询播种墙格口状态 */
    public List<SortingGrid> listGrids(String wallCode) {
        return sortingGridMapper.selectList(
                new LambdaQueryWrapper<SortingGrid>()
                        .eq(SortingGrid::getWallCode, wallCode)
                        .orderByAsc(SortingGrid::getGridNo));
    }
}
