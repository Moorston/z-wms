package com.xwms.core.adjust.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.core.adjust.entity.*;
import com.xwms.core.adjust.mapper.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 库存调整管理核心服务 核心能力: 库存调整单/库存冻结/库存解冻/调整流水 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryAdjustService {
    private final InventoryAdjustMapper adjustMapper;
    private final InventoryAdjustDetailMapper detailMapper;
    private final InventoryAdjustLogMapper logMapper;
    private static final AtomicInteger SEQ = new AtomicInteger(0);
    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    // ============================================================
    // 1. 库存调整单管理
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public InventoryAdjust createAdjust(
            InventoryAdjust adjust, List<InventoryAdjustDetail> details, String operator) {
        adjust.setAdjustNo(generateAdjustNo());
        adjust.setStatus("DRAFT");
        adjust.setCreatedBy(operator);
        if (adjust.getTotalSku() == null) adjust.setTotalSku(details.size());
        if (adjust.getTotalQty() == null) {
            BigDecimal totalQty =
                    details.stream()
                            .map(InventoryAdjustDetail::getAdjustQty)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
            adjust.setTotalQty(totalQty);
        }
        adjustMapper.insert(adjust);
        // 保存明细
        int lineNo = 1;
        for (InventoryAdjustDetail detail : details) {
            detail.setAdjustNo(adjust.getAdjustNo());
            detail.setLineNo(lineNo++);
            detailMapper.insert(detail);
        }
        log.info(
                "创建库存调整单: no={}, type={}, skuCount={}",
                adjust.getAdjustNo(),
                adjust.getAdjustType(),
                details.size());
        return adjust;
    }

    @Transactional(rollbackFor = Exception.class)
    public InventoryAdjust submitAdjust(String adjustNo, String operator) {
        InventoryAdjust adjust = adjustMapper.selectByAdjustNo(adjustNo);
        if (adjust == null) throw new RuntimeException("调整单不存在: " + adjustNo);
        if (!"DRAFT".equals(adjust.getStatus())) {
            throw new RuntimeException("调整单状态不正确: " + adjust.getStatus());
        }
        adjust.setStatus("SUBMITTED");
        adjustMapper.updateById(adjust);
        log.info("提交库存调整单: no={}", adjustNo);
        return adjust;
    }

    @Transactional(rollbackFor = Exception.class)
    public InventoryAdjust approveAdjust(String adjustNo, String operator) {
        InventoryAdjust adjust = adjustMapper.selectByAdjustNo(adjustNo);
        if (adjust == null) throw new RuntimeException("调整单不存在: " + adjustNo);
        if (!"SUBMITTED".equals(adjust.getStatus())) {
            throw new RuntimeException("调整单状态不正确: " + adjust.getStatus());
        }
        adjust.setStatus("APPROVED");
        adjust.setApprovedBy(operator);
        adjust.setApprovedTime(LocalDateTime.now());
        adjustMapper.updateById(adjust);
        log.info("审批库存调整单: no={}", adjustNo);
        return adjust;
    }

    /** 执行库存调整（核心：Oracle原子扣减/增加库存） */
    @Transactional(rollbackFor = Exception.class)
    public InventoryAdjust executeAdjust(String adjustNo, String operator) {
        InventoryAdjust adjust = adjustMapper.selectByAdjustNo(adjustNo);
        if (adjust == null) throw new RuntimeException("调整单不存在: " + adjustNo);
        if (!"APPROVED".equals(adjust.getStatus())) {
            throw new RuntimeException("调整单状态不正确: " + adjust.getStatus());
        }
        List<InventoryAdjustDetail> details = detailMapper.selectByAdjustNo(adjustNo);
        for (InventoryAdjustDetail detail : details) {
            // TODO: 调用库存服务执行原子调整
            // 记录调整流水
            recordAdjustLog(
                    adjustNo,
                    null,
                    adjust.getWarehouseCode(),
                    adjust.getOwnerCode(),
                    detail.getSkuCode(),
                    detail.getBatchNo(),
                    detail.getLocationCode(),
                    "ADJUST",
                    detail.getBeforeQty(),
                    detail.getAdjustQty(),
                    detail.getAfterQty(),
                    null,
                    null,
                    operator,
                    detail.getReason());
        }
        adjust.setStatus("EXECUTED");
        adjust.setExecutedBy(operator);
        adjust.setExecutedTime(LocalDateTime.now());
        adjustMapper.updateById(adjust);
        log.info("执行库存调整单: no={}, detailCount={}", adjustNo, details.size());
        return adjust;
    }

    @Transactional(rollbackFor = Exception.class)
    public InventoryAdjust cancelAdjust(String adjustNo, String operator) {
        InventoryAdjust adjust = adjustMapper.selectByAdjustNo(adjustNo);
        if (adjust == null) throw new RuntimeException("调整单不存在: " + adjustNo);
        if ("EXECUTED".equals(adjust.getStatus())) {
            throw new RuntimeException("已执行的调整单不能取消");
        }
        adjust.setStatus("CANCELLED");
        adjustMapper.updateById(adjust);
        log.info("取消库存调整单: no={}", adjustNo);
        return adjust;
    }

    public Page<InventoryAdjust> pageAdjusts(
            Page<InventoryAdjust> page, String warehouseCode, String adjustType, String status) {
        LambdaQueryWrapper<InventoryAdjust> wrapper = new LambdaQueryWrapper<>();
        if (warehouseCode != null) wrapper.eq(InventoryAdjust::getWarehouseCode, warehouseCode);
        if (adjustType != null) wrapper.eq(InventoryAdjust::getAdjustType, adjustType);
        if (status != null) wrapper.eq(InventoryAdjust::getStatus, status);
        wrapper.orderByDesc(InventoryAdjust::getCreatedTime);
        return adjustMapper.selectPage(page, wrapper);
    }

    public InventoryAdjust getAdjustByNo(String adjustNo) {
        return adjustMapper.selectByAdjustNo(adjustNo);
    }

    public List<InventoryAdjustDetail> getAdjustDetails(String adjustNo) {
        return detailMapper.selectByAdjustNo(adjustNo);
    }

    // ============================================================
    // 3. 库存调整流水
    // ============================================================
    public List<InventoryAdjustLog> getAdjustLogsBySku(
            String skuCode, String batchNo, String locationCode) {
        return logMapper.selectBySkuLocation(skuCode, batchNo, locationCode);
    }

    public List<InventoryAdjustLog> getAdjustLogsByAdjustNo(String adjustNo) {
        return logMapper.selectByAdjustNo(adjustNo);
    }

    // ============================================================
    // 工具方法
    // ============================================================
    private void recordAdjustLog(
            String adjustNo,
            String freezeNo,
            String warehouseCode,
            String ownerCode,
            String skuCode,
            String batchNo,
            String locationCode,
            String actionType,
            BigDecimal beforeQty,
            BigDecimal changeQty,
            BigDecimal afterQty,
            BigDecimal beforeFrozen,
            BigDecimal afterFrozen,
            String operator,
            String remark) {
        InventoryAdjustLog log = new InventoryAdjustLog();
        log.setLogNo(generateLogNo());
        log.setAdjustNo(adjustNo);
        log.setFreezeNo(freezeNo);
        log.setWarehouseCode(warehouseCode);
        log.setOwnerCode(ownerCode);
        log.setSkuCode(skuCode);
        log.setBatchNo(batchNo);
        log.setLocationCode(locationCode);
        log.setActionType(actionType);
        log.setBeforeQty(beforeQty);
        log.setChangeQty(changeQty);
        log.setAfterQty(afterQty);
        log.setBeforeFrozen(beforeFrozen);
        log.setAfterFrozen(afterFrozen);
        log.setOperator(operator);
        log.setActionTime(LocalDateTime.now());
        log.setRemark(remark);
        logMapper.insert(log);
    }

    private String generateAdjustNo() {
        return "ADJ"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }

    private String generateFreezeNo() {
        return "FRZ"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }

    private String generateLogNo() {
        return "LOG"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }
}
