package com.xwms.core.expiry.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.core.expiry.entity.*;
import com.xwms.core.expiry.mapper.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 库存效期管理核心服务 核心能力: 效期规则/效期计算/临期预警/过期处理/FEFO */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryExpiryService {

    private final ExpiryRuleMapper ruleMapper;
    private final ExpiryBatchMapper batchMapper;
    private final ExpiryAlertMapper alertMapper;
    private final ExpiryHandleLogMapper handleLogMapper;

    private static final AtomicInteger SEQ = new AtomicInteger(0);
    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    // ============================================================

    // 1. 效期规则管理
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public ExpiryRule createRule(ExpiryRule rule) {
        rule.setStatus("ACTIVE");
        if (rule.getPriority() == null) rule.setPriority(5);
        if (rule.getFefoEnable() == null) rule.setFefoEnable("Y");
        if (rule.getAutoFreeze() == null) rule.setAutoFreeze("N");
        ruleMapper.insert(rule);
        log.info(
                "创建效期规则: {}={}, shelfLife={}天",
                rule.getRuleCode(),
                rule.getRuleName(),
                rule.getShelfLifeDays());
        return rule;
    }

    public ExpiryRule getRuleByCode(String ruleCode) {
        return ruleMapper.selectByRuleCode(ruleCode);
    }

    public ExpiryRule matchRule(
            String warehouseCode, String ownerCode, String categoryCode, String skuCode) {
        return ruleMapper.matchRule(warehouseCode, ownerCode, categoryCode, skuCode);
    }

    public Page<ExpiryRule> pageRules(Page<ExpiryRule> page, String warehouseCode) {
        LambdaQueryWrapper<ExpiryRule> wrapper = new LambdaQueryWrapper<>();
        if (warehouseCode != null) wrapper.eq(ExpiryRule::getWarehouseCode, warehouseCode);
        wrapper.eq(ExpiryRule::getStatus, "ACTIVE");
        return ruleMapper.selectPage(page, wrapper);
    }

    // ============================================================

    // 2. 效期批次管理
    // ============================================================

    /** 注册效期批次（入库时调用） */
    @Transactional(rollbackFor = Exception.class)
    public ExpiryBatch registerBatch(
            String batchNo,
            String warehouseCode,
            String ownerCode,
            String skuCode,
            String skuName,
            LocalDate productionDate,
            LocalDate expiryDate,
            BigDecimal totalQty,
            String inboundNo,
            String supplierCode,
            String remark) {
        // 检查是否已存在
        ExpiryRule rule = ruleMapper.matchRule(warehouseCode, ownerCode, null, skuCode);
        Integer shelfLifeDays = rule != null ? rule.getShelfLifeDays() : null;

        // 如果没有传过期日期，根据生产日期和保质期计算
        if (expiryDate == null && productionDate != null && shelfLifeDays != null) {
            expiryDate = productionDate.plusDays(shelfLifeDays);
        }

        if (expiryDate == null) {
            throw new RuntimeException("过期日期不能为空，且无法根据生产日期和保质期计算");
        }

        ExpiryBatch batch = new ExpiryBatch();
        batch.setBatchNo(batchNo);
        batch.setWarehouseCode(warehouseCode);
        batch.setOwnerCode(ownerCode);
        batch.setSkuCode(skuCode);
        batch.setSkuName(skuName);
        batch.setProductionDate(productionDate);
        batch.setExpiryDate(expiryDate);
        batch.setShelfLifeDays(shelfLifeDays);
        batch.setTotalQty(totalQty);
        batch.setAvailableQty(totalQty);
        batch.setInboundNo(inboundNo);
        batch.setSupplierCode(supplierCode);
        batch.setRemark(remark);

        // 计算剩余天数和预警级别
        updateExpiryStatus(batch);

        batchMapper.insert(batch);
        log.info(
                "注册效期批次: batch={}, sku={}, expiry={}, remain={}天, level={}",
                batchNo,
                skuCode,
                expiryDate,
                batch.getRemainDays(),
                batch.getWarningLevel());
        return batch;
    }

    /** 更新批次效期状态 */
    private void updateExpiryStatus(ExpiryBatch batch) {
        if (batch.getExpiryDate() == null) return;

        int remainDays = (int) ChronoUnit.DAYS.between(LocalDate.now(), batch.getExpiryDate());
        batch.setRemainDays(remainDays);

        ExpiryRule rule =
                ruleMapper.matchRule(
                        batch.getWarehouseCode(), batch.getOwnerCode(), null, batch.getSkuCode());
        int w1 = rule != null && rule.getWarningDays1() != null ? rule.getWarningDays1() : 30;
        int w2 = rule != null && rule.getWarningDays2() != null ? rule.getWarningDays2() : 14;
        int w3 = rule != null && rule.getWarningDays3() != null ? rule.getWarningDays3() : 7;

        if (remainDays < 0) {
            batch.setWarningLevel("EXPIRED");
            batch.setExpiryStatus("EXPIRED");
        } else if (remainDays <= w3) {
            batch.setWarningLevel("W3");
            batch.setExpiryStatus("NEAR_EXPIRY");
        } else if (remainDays <= w2) {
            batch.setWarningLevel("W2");
            batch.setExpiryStatus("NEAR_EXPIRY");
        } else if (remainDays <= w1) {
            batch.setWarningLevel("W1");
            batch.setExpiryStatus("NEAR_EXPIRY");
        } else {
            batch.setWarningLevel("NORMAL");
            batch.setExpiryStatus("NORMAL");
        }
    }

    /** 批量更新效期状态（定时任务调用） */
    @Transactional(rollbackFor = Exception.class)
    public int batchUpdateExpiryStatus(String warehouseCode) {
        List<ExpiryBatch> batches =
                batchMapper.selectList(
                        new LambdaQueryWrapper<ExpiryBatch>()
                                .eq(ExpiryBatch::getWarehouseCode, warehouseCode)
                                .ne(ExpiryBatch::getExpiryStatus, "EXPIRED"));

        int count = 0;
        for (ExpiryBatch batch : batches) {
            String oldLevel = batch.getWarningLevel();
            updateExpiryStatus(batch);
            if (!Objects.equals(oldLevel, batch.getWarningLevel())) {
                batchMapper.updateExpiryStatus(
                        batch.getId(),
                        batch.getRemainDays(),
                        batch.getWarningLevel(),
                        batch.getExpiryStatus());
                count++;

                // 如果变为过期，自动生成预警
                if ("EXPIRED".equals(batch.getWarningLevel())) {
                    createExpiryAlert(batch, "EXPIRED");
                } else if ("W3".equals(batch.getWarningLevel())
                        || "W2".equals(batch.getWarningLevel())
                        || "W1".equals(batch.getWarningLevel())) {
                    createExpiryAlert(batch, "NEAR_EXPIRY");
                }
            }
        }

        log.info("批量更新效期状态: warehouse={}, updated={}", warehouseCode, count);
        return count;
    }

    // ============================================================

    // 3. 临期预警
    // ============================================================

    private void createExpiryAlert(ExpiryBatch batch, String alertType) {
        ExpiryAlert alert = new ExpiryAlert();
        alert.setAlertNo(generateAlertNo());
        alert.setWarehouseCode(batch.getWarehouseCode());
        alert.setOwnerCode(batch.getOwnerCode());
        alert.setSkuCode(batch.getSkuCode());
        alert.setSkuName(batch.getSkuName());
        alert.setBatchNo(batch.getBatchNo());
        alert.setExpiryDate(batch.getExpiryDate());
        alert.setRemainDays(batch.getRemainDays());
        alert.setWarningLevel(batch.getWarningLevel());
        alert.setAlertQty(batch.getAvailableQty());
        alert.setAlertType(alertType);
        alert.setStatus("PENDING");
        alertMapper.insert(alert);
        log.info(
                "生成效期预警: alertNo={}, batch={}, type={}, level={}",
                alert.getAlertNo(),
                batch.getBatchNo(),
                alertType,
                batch.getWarningLevel());
    }

    @Transactional(rollbackFor = Exception.class)
    public ExpiryAlert handleAlert(String alertNo, String handleAction, String handledBy) {
        ExpiryAlert alert = alertMapper.selectByAlertNo(alertNo);
        if (alert == null) throw new RuntimeException("预警单不存在: " + alertNo);
        alertMapper.updateStatus(alertNo, "RESOLVED", handleAction, handledBy);
        log.info("处理效期预警: alertNo={}, action={}", alertNo, handleAction);
        return alertMapper.selectByAlertNo(alertNo);
    }

    // ============================================================

    // 4. 过期处理
    // ============================================================

    /** 处理过期库存 */
    @Transactional(rollbackFor = Exception.class)
    public ExpiryHandleLog handleExpiry(
            String batchNo,
            String warehouseCode,
            String skuCode,
            String handleType,
            BigDecimal handleQty,
            String handleReason,
            String refNo,
            String operator) {
        ExpiryBatch batch = batchMapper.selectByBatch(batchNo, warehouseCode, skuCode);
        if (batch == null) throw new RuntimeException("效期批次不存在: " + batchNo);

        ExpiryHandleLog handleLog = new ExpiryHandleLog();
        handleLog.setHandleNo(generateHandleNo());
        handleLog.setWarehouseCode(warehouseCode);
        handleLog.setOwnerCode(batch.getOwnerCode());
        handleLog.setSkuCode(skuCode);
        handleLog.setBatchNo(batchNo);
        handleLog.setExpiryDate(batch.getExpiryDate());
        handleLog.setHandleType(handleType);
        handleLog.setHandleQty(handleQty);
        handleLog.setHandleReason(handleReason);
        handleLog.setRefNo(refNo);
        handleLog.setOperator(operator);
        handleLog.setHandleTime(LocalDateTime.now());
        handleLogMapper.insert(handleLog);

        // 更新批次可用数量
        if (batch.getAvailableQty() != null) {
            batch.setAvailableQty(batch.getAvailableQty().subtract(handleQty));
            batchMapper.updateById(batch);
        }

        log.info(
                "处理过期库存: handleNo={}, batch={}, type={}, qty={}",
                handleLog.getHandleNo(),
                batchNo,
                handleType,
                handleQty);
        return handleLog;
    }

    // ============================================================

    // 5. FEFO（先到期先出）
    // ============================================================

    /** 获取FEFO排序的批次列表 */
    public List<ExpiryBatch> getFefoBatches(String skuCode, String warehouseCode) {
        return batchMapper.selectFefoBySku(skuCode, warehouseCode);
    }

    /** 按FEFO分配库存 */
    public List<Map<String, Object>> allocateByFefo(
            String skuCode, String warehouseCode, BigDecimal needQty) {
        List<ExpiryBatch> batches = batchMapper.selectFefoBySku(skuCode, warehouseCode);
        List<Map<String, Object>> result = new ArrayList<>();
        BigDecimal remaining = needQty;

        for (ExpiryBatch batch : batches) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;
            if (batch.getAvailableQty() == null
                    || batch.getAvailableQty().compareTo(BigDecimal.ZERO) <= 0) continue;

            BigDecimal allocateQty = batch.getAvailableQty().min(remaining);
            Map<String, Object> item = new HashMap<>();
            item.put("batchNo", batch.getBatchNo());
            item.put("expiryDate", batch.getExpiryDate());
            item.put("remainDays", batch.getRemainDays());
            item.put("allocateQty", allocateQty);
            result.add(item);
            remaining = remaining.subtract(allocateQty);
        }

        return result;
    }

    // ============================================================

    // 6. 查询
    // ============================================================

    public ExpiryBatch getBatch(String batchNo, String warehouseCode, String skuCode) {
        return batchMapper.selectByBatch(batchNo, warehouseCode, skuCode);
    }

    public List<ExpiryBatch> getExpiringBatches(String warehouseCode, int withinDays) {
        return batchMapper.selectExpiringBatches(
                warehouseCode, LocalDate.now().plusDays(withinDays));
    }

    public List<ExpiryBatch> getWarningBatches(String warehouseCode) {
        return batchMapper.selectWarningBatches(warehouseCode);
    }

    public Page<ExpiryBatch> pageBatches(
            Page<ExpiryBatch> page, String warehouseCode, String skuCode, String expiryStatus) {
        LambdaQueryWrapper<ExpiryBatch> wrapper = new LambdaQueryWrapper<>();
        if (warehouseCode != null) wrapper.eq(ExpiryBatch::getWarehouseCode, warehouseCode);
        if (skuCode != null) wrapper.eq(ExpiryBatch::getSkuCode, skuCode);
        if (expiryStatus != null) wrapper.eq(ExpiryBatch::getExpiryStatus, expiryStatus);
        wrapper.orderByAsc(ExpiryBatch::getExpiryDate);
        return batchMapper.selectPage(page, wrapper);
    }

    public Page<ExpiryAlert> pageAlerts(
            Page<ExpiryAlert> page, String warehouseCode, String status) {
        LambdaQueryWrapper<ExpiryAlert> wrapper = new LambdaQueryWrapper<>();
        if (warehouseCode != null) wrapper.eq(ExpiryAlert::getWarehouseCode, warehouseCode);
        if (status != null) wrapper.eq(ExpiryAlert::getStatus, status);
        wrapper.orderByDesc(ExpiryAlert::getCreatedTime);
        return alertMapper.selectPage(page, wrapper);
    }

    public ExpiryAlert getAlertByNo(String alertNo) {
        return alertMapper.selectByAlertNo(alertNo);
    }

    public List<ExpiryHandleLog> getHandleLogsByBatch(String batchNo) {
        return handleLogMapper.selectByBatchNo(batchNo);
    }

    public ExpiryHandleLog getHandleLogByNo(String handleNo) {
        return handleLogMapper.selectByHandleNo(handleNo);
    }

    // ============================================================

    // 工具方法
    // ============================================================

    private String generateAlertNo() {
        return "EA"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }

    private String generateHandleNo() {
        return "EH"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }
}
