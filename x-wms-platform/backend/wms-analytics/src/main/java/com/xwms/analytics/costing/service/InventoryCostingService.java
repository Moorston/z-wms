package com.xwms.analytics.costing.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.analytics.costing.entity.*;
import com.xwms.analytics.costing.mapper.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 库存成本核算管理核心服务 核心能力: 成本核算/成本调整/成本分摊/成本明细 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryCostingService {

    private final CostCalculateMapper calculateMapper;
    private final CostAdjustMapper adjustMapper;
    private final CostAllocationMapper allocationMapper;
    private final CostDetailMapper detailMapper;

    private static final AtomicInteger SEQ = new AtomicInteger(0);
    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    // ============================================================

    // 1. 成本核算管理
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public CostCalculate createCalculate(
            String calculateName,
            String calculateType,
            String warehouseCode,
            String ownerCode,
            LocalDate periodStart,
            LocalDate periodEnd,
            String costingMethod,
            String operator) {
        CostCalculate calculate = new CostCalculate();
        calculate.setCalculateId(generateCalculateId());
        calculate.setCalculateName(calculateName);
        calculate.setCalculateType(calculateType);
        calculate.setWarehouseCode(warehouseCode);
        calculate.setOwnerCode(ownerCode);
        calculate.setPeriodStart(periodStart);
        calculate.setPeriodEnd(periodEnd);
        calculate.setCostingMethod(costingMethod);
        calculate.setTotalSkuCount(0);
        calculate.setCalculatedCount(0);
        calculate.setFailedCount(0);
        calculate.setStatus("PENDING");
        calculate.setOperator(operator);
        calculateMapper.insert(calculate);
        log.info(
                "创建成本核算: id={}, type={}, warehouse={}, period={}~{}",
                calculate.getCalculateId(),
                calculateType,
                warehouseCode,
                periodStart,
                periodEnd);
        return calculate;
    }

    @Transactional(rollbackFor = Exception.class)
    public CostCalculate startCalculate(String calculateId) {
        CostCalculate calculate = calculateMapper.selectByCalculateId(calculateId);
        if (calculate == null) throw new RuntimeException("成本核算不存在: " + calculateId);
        if (!"PENDING".equals(calculate.getStatus())) {
            throw new RuntimeException("核算状态不正确: " + calculate.getStatus());
        }
        calculateMapper.startCalculate(calculateId, "CALCULATING");
        log.info("开始成本核算: id={}", calculateId);
        return calculateMapper.selectByCalculateId(calculateId);
    }

    @Transactional(rollbackFor = Exception.class)
    public CostCalculate completeCalculate(
            String calculateId,
            int calculatedCount,
            int failedCount,
            BigDecimal totalQuantity,
            BigDecimal totalAmount,
            BigDecimal averageCost,
            Long durationMs,
            String errorMessage) {
        String status = failedCount > 0 && calculatedCount == 0 ? "FAILED" : "COMPLETED";
        calculateMapper.completeCalculate(
                calculateId,
                status,
                calculatedCount,
                failedCount,
                totalQuantity,
                totalAmount,
                averageCost,
                durationMs,
                errorMessage);
        log.info(
                "完成成本核算: id={}, calculated={}, failed={}, status={}",
                calculateId,
                calculatedCount,
                failedCount,
                status);
        return calculateMapper.selectByCalculateId(calculateId);
    }

    public CostCalculate getCalculateById(String calculateId) {
        return calculateMapper.selectByCalculateId(calculateId);
    }

    public CostCalculate getCalculateByWarehouseAndPeriod(
            String warehouseCode, LocalDate periodStart, LocalDate periodEnd) {
        return calculateMapper.selectByWarehouseAndPeriod(warehouseCode, periodStart, periodEnd);
    }

    public List<CostCalculate> getCalculatesByStatus(String status) {
        return calculateMapper.selectByStatus(status);
    }

    public List<CostCalculate> getRecentCalculatesByWarehouseAndType(
            String warehouseCode, String calculateType, int limit) {
        return calculateMapper.selectRecentByWarehouseAndType(warehouseCode, calculateType, limit);
    }

    /** 执行成本核算（核心逻辑） */
    @Transactional(rollbackFor = Exception.class)
    public CostCalculate executeCalculate(String calculateId) {
        CostCalculate calculate = calculateMapper.selectByCalculateId(calculateId);
        if (calculate == null) throw new RuntimeException("成本核算不存在: " + calculateId);

        long startTime = System.currentTimeMillis();
        int calculatedCount = 0;
        int failedCount = 0;
        BigDecimal totalQuantity = BigDecimal.ZERO;
        BigDecimal totalAmount = BigDecimal.ZERO;

        try {
            // TODO: 根据成本方法执行核算
            // FIFO: 先进先出，按入库批次顺序计算出库成本
            // LIFO: 后进先出，按最新入库批次计算出库成本
            // WEIGHTED_AVG: 加权平均，(期初金额+本期入库金额)/(期初数量+本期入库数量)
            // MOVING_AVG: 移动平均，每次入库后重新计算平均成本
            // STANDARD: 标准成本，按预设标准成本计算
            // SPECIFIC: 个别计价，按实际批次成本计算

            log.info("执行成本核算: id={}, method={}", calculateId, calculate.getCostingMethod());

            // 模拟核算完成
            calculatedCount =
                    calculate.getTotalSkuCount() != null ? calculate.getTotalSkuCount() : 0;

            long durationMs = System.currentTimeMillis() - startTime;
            return completeCalculate(
                    calculateId,
                    calculatedCount,
                    failedCount,
                    totalQuantity,
                    totalAmount,
                    BigDecimal.ZERO,
                    durationMs,
                    null);
        } catch (Exception e) {
            long durationMs = System.currentTimeMillis() - startTime;
            log.error("成本核算失败: id={}, error={}", calculateId, e.getMessage(), e);
            return completeCalculate(
                    calculateId,
                    calculatedCount,
                    failedCount + 1,
                    totalQuantity,
                    totalAmount,
                    BigDecimal.ZERO,
                    durationMs,
                    e.getMessage());
        }
    }

    public Page<CostCalculate> pageCalculates(
            Page<CostCalculate> page, String warehouseCode, String calculateType, String status) {
        LambdaQueryWrapper<CostCalculate> wrapper = new LambdaQueryWrapper<>();
        if (warehouseCode != null) wrapper.eq(CostCalculate::getWarehouseCode, warehouseCode);
        if (calculateType != null) wrapper.eq(CostCalculate::getCalculateType, calculateType);
        if (status != null) wrapper.eq(CostCalculate::getStatus, status);
        wrapper.orderByDesc(CostCalculate::getCreatedTime);
        return calculateMapper.selectPage(page, wrapper);
    }

    // ============================================================

    // 2. 成本调整管理
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public CostAdjust createAdjust(
            String adjustType,
            String warehouseCode,
            String ownerCode,
            String skuCode,
            String batchNo,
            String locationCode,
            BigDecimal beforeQuantity,
            BigDecimal afterQuantity,
            BigDecimal beforeCost,
            BigDecimal afterCost,
            BigDecimal beforeAmount,
            BigDecimal afterAmount,
            String adjustReason,
            String adjustBasis,
            String relatedBizType,
            String relatedBizNo,
            String operator) {
        CostAdjust adjust = new CostAdjust();
        adjust.setAdjustId(generateAdjustId());
        adjust.setAdjustType(adjustType);
        adjust.setWarehouseCode(warehouseCode);
        adjust.setOwnerCode(ownerCode);
        adjust.setSkuCode(skuCode);
        adjust.setBatchNo(batchNo);
        adjust.setLocationCode(locationCode);
        adjust.setBeforeQuantity(beforeQuantity);
        adjust.setAfterQuantity(afterQuantity);
        adjust.setQuantityDiff(
                afterQuantity != null && beforeQuantity != null
                        ? afterQuantity.subtract(beforeQuantity)
                        : BigDecimal.ZERO);
        adjust.setBeforeCost(beforeCost);
        adjust.setAfterCost(afterCost);
        adjust.setCostDiff(
                afterCost != null && beforeCost != null
                        ? afterCost.subtract(beforeCost)
                        : BigDecimal.ZERO);
        adjust.setBeforeAmount(beforeAmount);
        adjust.setAfterAmount(afterAmount);
        adjust.setAmountDiff(
                afterAmount != null && beforeAmount != null
                        ? afterAmount.subtract(beforeAmount)
                        : BigDecimal.ZERO);
        adjust.setAdjustReason(adjustReason);
        adjust.setAdjustBasis(adjustBasis);
        adjust.setRelatedBizType(relatedBizType);
        adjust.setRelatedBizNo(relatedBizNo);
        adjust.setStatus("PENDING");
        adjust.setOperator(operator);
        adjust.setAdjustTime(LocalDateTime.now());
        adjustMapper.insert(adjust);
        log.info(
                "创建成本调整: id={}, type={}, sku={}, amountDiff={}",
                adjust.getAdjustId(),
                adjustType,
                skuCode,
                adjust.getAmountDiff());
        return adjust;
    }

    @Transactional(rollbackFor = Exception.class)
    public CostAdjust approveAdjust(
            String adjustId, String approver, String approveOpinion, boolean approved) {
        CostAdjust adjust = adjustMapper.selectByAdjustId(adjustId);
        if (adjust == null) throw new RuntimeException("成本调整不存在: " + adjustId);
        if (!"PENDING".equals(adjust.getStatus())) {
            throw new RuntimeException("调整状态不正确: " + adjust.getStatus());
        }
        String status = approved ? "APPROVED" : "REJECTED";
        adjustMapper.approveAdjust(adjustId, status, approver, approveOpinion);

        if (approved) {
            // 记录成本明细
            recordCostDetail(
                    null,
                    adjustId,
                    null,
                    adjust.getWarehouseCode(),
                    adjust.getOwnerCode(),
                    adjust.getSkuCode(),
                    adjust.getBatchNo(),
                    adjust.getLocationCode(),
                    "ADJUST",
                    adjust.getAdjustId(),
                    adjust.getAfterQuantity(),
                    adjust.getAfterCost(),
                    adjust.getAfterAmount(),
                    adjust.getBeforeQuantity(),
                    adjust.getAfterQuantity(),
                    adjust.getBeforeCost(),
                    adjust.getAfterCost(),
                    adjust.getBeforeAmount(),
                    adjust.getAfterAmount(),
                    adjust.getAmountDiff(),
                    null,
                    LocalDate.now(),
                    approver);
        }

        log.info("审核成本调整: id={}, approved={}, approver={}", adjustId, approved, approver);
        return adjustMapper.selectByAdjustId(adjustId);
    }

    public CostAdjust getAdjustById(String adjustId) {
        return adjustMapper.selectByAdjustId(adjustId);
    }

    public List<CostAdjust> getAdjustsBySku(String skuCode, int limit) {
        return adjustMapper.selectBySku(skuCode, limit);
    }

    public List<CostAdjust> getAdjustsByStatus(String status) {
        return adjustMapper.selectByStatus(status);
    }

    public List<CostAdjust> getAdjustsByWarehouseAndType(
            String warehouseCode, String adjustType, int limit) {
        return adjustMapper.selectByWarehouseAndType(warehouseCode, adjustType, limit);
    }

    public Page<CostAdjust> pageAdjusts(
            Page<CostAdjust> page,
            String warehouseCode,
            String adjustType,
            String status,
            String skuCode) {
        LambdaQueryWrapper<CostAdjust> wrapper = new LambdaQueryWrapper<>();
        if (warehouseCode != null) wrapper.eq(CostAdjust::getWarehouseCode, warehouseCode);
        if (adjustType != null) wrapper.eq(CostAdjust::getAdjustType, adjustType);
        if (status != null) wrapper.eq(CostAdjust::getStatus, status);
        if (skuCode != null) wrapper.eq(CostAdjust::getSkuCode, skuCode);
        wrapper.orderByDesc(CostAdjust::getAdjustTime);
        return adjustMapper.selectPage(page, wrapper);
    }

    // ============================================================

    // 3. 成本分摊管理
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public CostAllocation createAllocation(
            String allocationName,
            String allocationType,
            String warehouseCode,
            String ownerCode,
            LocalDate periodStart,
            LocalDate periodEnd,
            BigDecimal totalAmount,
            String allocationMethod,
            String operator) {
        CostAllocation allocation = new CostAllocation();
        allocation.setAllocationId(generateAllocationId());
        allocation.setAllocationName(allocationName);
        allocation.setAllocationType(allocationType);
        allocation.setWarehouseCode(warehouseCode);
        allocation.setOwnerCode(ownerCode);
        allocation.setPeriodStart(periodStart);
        allocation.setPeriodEnd(periodEnd);
        allocation.setTotalAmount(totalAmount);
        allocation.setAllocationMethod(allocationMethod);
        allocation.setAllocatedCount(0);
        allocation.setAllocatedAmount(BigDecimal.ZERO);
        allocation.setStatus("PENDING");
        allocation.setOperator(operator);
        allocationMapper.insert(allocation);
        log.info(
                "创建成本分摊: id={}, type={}, totalAmount={}, method={}",
                allocation.getAllocationId(),
                allocationType,
                totalAmount,
                allocationMethod);
        return allocation;
    }

    @Transactional(rollbackFor = Exception.class)
    public CostAllocation startAllocation(String allocationId) {
        CostAllocation allocation = allocationMapper.selectByAllocationId(allocationId);
        if (allocation == null) throw new RuntimeException("成本分摊不存在: " + allocationId);
        if (!"PENDING".equals(allocation.getStatus())) {
            throw new RuntimeException("分摊状态不正确: " + allocation.getStatus());
        }
        allocationMapper.startAllocation(allocationId, "ALLOCATING");
        log.info("开始成本分摊: id={}", allocationId);
        return allocationMapper.selectByAllocationId(allocationId);
    }

    @Transactional(rollbackFor = Exception.class)
    public CostAllocation completeAllocation(
            String allocationId,
            int allocatedCount,
            BigDecimal allocatedAmount,
            Long durationMs,
            String errorMessage) {
        String status = allocatedAmount.compareTo(BigDecimal.ZERO) == 0 ? "FAILED" : "COMPLETED";
        allocationMapper.completeAllocation(
                allocationId, status, allocatedCount, allocatedAmount, durationMs, errorMessage);
        log.info(
                "完成成本分摊: id={}, allocatedCount={}, allocatedAmount={}, status={}",
                allocationId,
                allocatedCount,
                allocatedAmount,
                status);
        return allocationMapper.selectByAllocationId(allocationId);
    }

    /** 执行成本分摊（核心逻辑） */
    @Transactional(rollbackFor = Exception.class)
    public CostAllocation executeAllocation(String allocationId) {
        CostAllocation allocation = allocationMapper.selectByAllocationId(allocationId);
        if (allocation == null) throw new RuntimeException("成本分摊不存在: " + allocationId);

        long startTime = System.currentTimeMillis();
        int allocatedCount = 0;
        BigDecimal allocatedAmount = BigDecimal.ZERO;

        try {
            // TODO: 根据分摊方法执行分摊
            // BY_QUANTITY: 按数量分摊，每个SKU分摊金额 = 总金额 × (SKU数量 / 总数量)
            // BY_AMOUNT: 按金额分摊，每个SKU分摊金额 = 总金额 × (SKU金额 / 总金额)
            // BY_WEIGHT: 按重量分摊，每个SKU分摊金额 = 总金额 × (SKU重量 / 总重量)
            // BY_VOLUME: 按体积分摊，每个SKU分摊金额 = 总金额 × (SKU体积 / 总体积)
            // BY_SKU: 按SKU平均分摊，每个SKU分摊金额 = 总金额 / SKU数量

            log.info(
                    "执行成本分摊: id={}, method={}, totalAmount={}",
                    allocationId,
                    allocation.getAllocationMethod(),
                    allocation.getTotalAmount());

            // 模拟分摊完成
            allocatedAmount = allocation.getTotalAmount();

            long durationMs = System.currentTimeMillis() - startTime;
            return completeAllocation(
                    allocationId, allocatedCount, allocatedAmount, durationMs, null);
        } catch (Exception e) {
            long durationMs = System.currentTimeMillis() - startTime;
            log.error("成本分摊失败: id={}, error={}", allocationId, e.getMessage(), e);
            return completeAllocation(
                    allocationId, allocatedCount, allocatedAmount, durationMs, e.getMessage());
        }
    }

    public CostAllocation getAllocationById(String allocationId) {
        return allocationMapper.selectByAllocationId(allocationId);
    }

    public List<CostAllocation> getAllocationsByStatus(String status) {
        return allocationMapper.selectByStatus(status);
    }

    public List<CostAllocation> getAllocationsByWarehouseAndType(
            String warehouseCode, String allocationType, int limit) {
        return allocationMapper.selectByWarehouseAndType(warehouseCode, allocationType, limit);
    }

    public Page<CostAllocation> pageAllocations(
            Page<CostAllocation> page, String warehouseCode, String allocationType, String status) {
        LambdaQueryWrapper<CostAllocation> wrapper = new LambdaQueryWrapper<>();
        if (warehouseCode != null) wrapper.eq(CostAllocation::getWarehouseCode, warehouseCode);
        if (allocationType != null) wrapper.eq(CostAllocation::getAllocationType, allocationType);
        if (status != null) wrapper.eq(CostAllocation::getStatus, status);
        wrapper.orderByDesc(CostAllocation::getCreatedTime);
        return allocationMapper.selectPage(page, wrapper);
    }

    // ============================================================

    // 4. 成本明细管理
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public CostDetail recordCostDetail(
            String calculateId,
            String adjustId,
            String allocationId,
            String warehouseCode,
            String ownerCode,
            String skuCode,
            String batchNo,
            String locationCode,
            String bizType,
            String bizNo,
            BigDecimal quantity,
            BigDecimal unitCost,
            BigDecimal totalCost,
            BigDecimal beforeQuantity,
            BigDecimal afterQuantity,
            BigDecimal beforeUnitCost,
            BigDecimal afterUnitCost,
            BigDecimal beforeTotalCost,
            BigDecimal afterTotalCost,
            BigDecimal costDiff,
            String costingMethod,
            LocalDate periodDate,
            String operator) {
        CostDetail detail = new CostDetail();
        detail.setDetailId(generateDetailId());
        detail.setCalculateId(calculateId);
        detail.setAdjustId(adjustId);
        detail.setAllocationId(allocationId);
        detail.setWarehouseCode(warehouseCode);
        detail.setOwnerCode(ownerCode);
        detail.setSkuCode(skuCode);
        detail.setBatchNo(batchNo);
        detail.setLocationCode(locationCode);
        detail.setBizType(bizType);
        detail.setBizNo(bizNo);
        detail.setQuantity(quantity);
        detail.setUnitCost(unitCost);
        detail.setTotalCost(totalCost);
        detail.setBeforeQuantity(beforeQuantity);
        detail.setAfterQuantity(afterQuantity);
        detail.setBeforeUnitCost(beforeUnitCost);
        detail.setAfterUnitCost(afterUnitCost);
        detail.setBeforeTotalCost(beforeTotalCost);
        detail.setAfterTotalCost(afterTotalCost);
        detail.setCostDiff(costDiff);
        detail.setCostingMethod(costingMethod);
        detail.setPeriodDate(periodDate != null ? periodDate : LocalDate.now());
        detail.setOperator(operator);
        detail.setOperationTime(LocalDateTime.now());
        detailMapper.insert(detail);
        log.debug(
                "记录成本明细: id={}, sku={}, biz={}, totalCost={}",
                detail.getDetailId(),
                skuCode,
                bizType,
                totalCost);
        return detail;
    }

    public CostDetail getDetailById(String detailId) {
        return detailMapper.selectByDetailId(detailId);
    }

    public List<CostDetail> getDetailsByCalculateId(String calculateId) {
        return detailMapper.selectByCalculateId(calculateId);
    }

    public List<CostDetail> getDetailsByAdjustId(String adjustId) {
        return detailMapper.selectByAdjustId(adjustId);
    }

    public List<CostDetail> getDetailsByAllocationId(String allocationId) {
        return detailMapper.selectByAllocationId(allocationId);
    }

    public List<CostDetail> getDetailsBySkuAndPeriod(
            String skuCode, LocalDate startDate, LocalDate endDate) {
        return detailMapper.selectBySkuAndPeriod(skuCode, startDate, endDate);
    }

    public List<CostDetail> getDetailsByBiz(String bizType, String bizNo) {
        return detailMapper.selectByBiz(bizType, bizNo);
    }

    public BigDecimal sumCostByWarehouseAndPeriod(
            String warehouseCode, LocalDate periodDate, String bizType) {
        return detailMapper.sumCostByWarehouseAndPeriod(warehouseCode, periodDate, bizType);
    }

    public Page<CostDetail> pageDetails(
            Page<CostDetail> page,
            String warehouseCode,
            String skuCode,
            String bizType,
            String calculateId,
            LocalDate startDate,
            LocalDate endDate) {
        LambdaQueryWrapper<CostDetail> wrapper = new LambdaQueryWrapper<>();
        if (warehouseCode != null) wrapper.eq(CostDetail::getWarehouseCode, warehouseCode);
        if (skuCode != null) wrapper.eq(CostDetail::getSkuCode, skuCode);
        if (bizType != null) wrapper.eq(CostDetail::getBizType, bizType);
        if (calculateId != null) wrapper.eq(CostDetail::getCalculateId, calculateId);
        if (startDate != null) wrapper.ge(CostDetail::getPeriodDate, startDate);
        if (endDate != null) wrapper.le(CostDetail::getPeriodDate, endDate);
        wrapper.orderByDesc(CostDetail::getOperationTime);
        return detailMapper.selectPage(page, wrapper);
    }

    // ============================================================

    // 工具方法
    // ============================================================

    private String generateCalculateId() {
        return "CC"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }

    private String generateAdjustId() {
        return "CA"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }

    private String generateAllocationId() {
        return "CAL"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }

    private String generateDetailId() {
        return "CD"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }
}
