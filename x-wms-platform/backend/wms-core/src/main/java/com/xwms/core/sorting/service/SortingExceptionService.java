package com.xwms.core.sorting.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import com.xwms.base.location.enums.VirtualLocationType;
import com.xwms.core.sorting.entity.SortingDifference;
import com.xwms.core.sorting.mapper.SortingDifferenceMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 分拣异常处理服务 核心能力： 1. 多货/少货/错货/破损差异记录 2. 差异商品存入差异虚拟库位 3. 差异处理（退回存储/报废/补拣） 4. 差异统计与告警 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SortingExceptionService {

    private final SortingDifferenceMapper sortingDifferenceMapper;
    private final VirtualLocationService virtualLocationService;

    /**
     * 处理分拣差异
     *
     * @param waveNo 波次号
     * @param orderNo 订单号（少货/错货时关联）
     * @param sku 商品SKU
     * @param batchNo 批次
     * @param type 差异类型：MORE/LESS/WRONG/DAMAGE
     * @param qty 差异数量
     * @param reason 原因
     * @param warehouse 仓库
     */
    @Transactional(rollbackFor = Exception.class)
    public SortingDifference handleDifference(
            String waveNo,
            String orderNo,
            String sku,
            String batchNo,
            String type,
            BigDecimal qty,
            String reason,
            String warehouse) {
        // 1. 创建差异虚拟库位
        String diffLocCode = VirtualLocationType.DIFFERENCE.generateCode(waveNo);
        virtualLocationService.createVirtualLocation(
                VirtualLocationType.DIFFERENCE, waveNo, warehouse, "DIFFERENCE_AREA");

        // 2. 多货/错货/破损：从波次虚拟库位移到差异虚拟库位
        if ("MORE".equals(type) || "WRONG".equals(type) || "DAMAGE".equals(type)) {
            String waveVirtualLoc = VirtualLocationType.SORTING.generateCode(waveNo);
            try {
                virtualLocationService.transfer(
                        waveVirtualLoc, diffLocCode, sku, batchNo, qty, warehouse, waveNo);
            } catch (Exception e) {
                log.warn("差异商品转移失败（可能波次虚拟库位已无库存）: {}", e.getMessage());
            }
        }

        // 3. 少货：生成补拣任务（由调用方处理，这里只记录）
        // 4. 记录差异
        SortingDifference diff = new SortingDifference();
        diff.setDiffNo(
                "DIFF" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 4));
        diff.setWaveNo(waveNo);
        diff.setOrderNo(orderNo);
        diff.setSku(sku);
        diff.setBatchNo(batchNo);
        diff.setDifferenceType(type);
        diff.setDifferenceQty(qty);
        diff.setStatus("PENDING");
        diff.setReason(reason);
        diff.setDiffLocationCode(diffLocCode);
        sortingDifferenceMapper.insert(diff);

        log.warn(
                "分拣差异记录: wave={}, order={}, sku={}, type={}, qty={}, reason={}",
                waveNo,
                orderNo,
                sku,
                type,
                qty,
                reason);
        return diff;
    }

    /**
     * 解决差异
     *
     * @param diffId 差异ID
     * @param handleMethod 处理方式：RETURN(退回存储)/SCRAP(报废)/REPLENISH(补拣)
     * @param targetLocation 目标库位（退回存储时用）
     * @param warehouse 仓库
     * @param handler 处理人
     */
    @Transactional(rollbackFor = Exception.class)
    public void resolveDifference(
            Long diffId,
            String handleMethod,
            String targetLocation,
            String warehouse,
            String handler) {
        SortingDifference diff = sortingDifferenceMapper.selectById(diffId);
        if (diff == null) {
            throw new RuntimeException("差异记录不存在: " + diffId);
        }
        if (!"PENDING".equals(diff.getStatus())) {
            throw new RuntimeException("差异已处理: " + diffId);
        }

        String diffLocCode = diff.getDiffLocationCode();

        switch (handleMethod) {
            case "RETURN" -> {
                // 退回存储区
                if (targetLocation == null) {
                    throw new RuntimeException("退回存储需要指定目标库位");
                }
                virtualLocationService.transfer(
                        diffLocCode,
                        targetLocation,
                        diff.getSku(),
                        diff.getBatchNo(),
                        diff.getDifferenceQty(),
                        warehouse,
                        diff.getDiffNo());
            }
            case "SCRAP" -> {
                // 报废：从差异虚拟库位扣减，转入系统报废库位
                String scrapLoc = VirtualLocationType.SYSTEM.generateCode("SCRAP");
                virtualLocationService.createVirtualLocation(
                        VirtualLocationType.SYSTEM, "SCRAP", warehouse, "SYSTEM_AREA");
                virtualLocationService.transfer(
                        diffLocCode,
                        scrapLoc,
                        diff.getSku(),
                        diff.getBatchNo(),
                        diff.getDifferenceQty(),
                        warehouse,
                        diff.getDiffNo());
            }
            case "REPLENISH" -> {
                // 补拣：差异商品退回波次虚拟库位，重新分播
                String waveVirtualLoc = VirtualLocationType.SORTING.generateCode(diff.getWaveNo());
                virtualLocationService.transfer(
                        diffLocCode,
                        waveVirtualLoc,
                        diff.getSku(),
                        diff.getBatchNo(),
                        diff.getDifferenceQty(),
                        warehouse,
                        diff.getDiffNo());
            }
            default -> throw new RuntimeException("未知处理方式: " + handleMethod);
        }

        // 更新差异状态
        diff.setStatus("RESOLVED");
        diff.setHandleMethod(handleMethod);
        diff.setHandler(handler);
        diff.setHandledAt(LocalDateTime.now());
        sortingDifferenceMapper.updateById(diff);

        log.info(
                "分拣差异已处理: diffNo={}, method={}, handler={}",
                diff.getDiffNo(),
                handleMethod,
                handler);
    }

    /** 查询待处理差异 */
    public java.util.List<SortingDifference> listPendingDifferences(String waveNo) {
        return sortingDifferenceMapper.selectList(
                new LambdaQueryWrapper<SortingDifference>()
                        .eq(SortingDifference::getStatus, "PENDING")
                        .eq(waveNo != null, SortingDifference::getWaveNo, waveNo)
                        .orderByDesc(SortingDifference::getCreatedAt));
    }
}
