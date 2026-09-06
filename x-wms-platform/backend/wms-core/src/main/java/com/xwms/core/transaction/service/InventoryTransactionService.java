package com.xwms.core.transaction.service;

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

import com.xwms.core.transaction.entity.*;
import com.xwms.core.transaction.mapper.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 库存流水管理核心服务 核心能力: 流水记录/流水查询/流水汇总/库存对账 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryTransactionService {

    private final InventoryTransactionMapper txnMapper;
    private final InventoryTxnSummaryMapper summaryMapper;
    private final InventoryReconcileMapper reconcileMapper;
    private final InventoryReconcileDiffMapper diffMapper;

    private static final AtomicInteger SEQ = new AtomicInteger(0);
    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    // ============================================================

    // 1. 记录库存流水（核心）
    // ============================================================

    /**
     * 记录库存流水
     *
     * @param txnType 流水类型
     * @param txnDirection 方向
     * @param warehouseCode 仓库
     * @param ownerCode 货主
     * @param skuCode SKU
     * @param locationCode 库位
     * @param batchNo 批次
     * @param quantity 变动数量
     * @param beforeQty 变动前数量
     * @param afterQty 变动后数量
     * @param unitCost 单位成本
     * @param businessType 业务类型
     * @param businessNo 业务单号
     * @param operator 操作人
     * @return 流水记录
     */
    @Transactional(rollbackFor = Exception.class)
    public InventoryTransaction recordTransaction(
            String txnType,
            String txnDirection,
            String warehouseCode,
            String ownerCode,
            String skuCode,
            String skuName,
            String locationCode,
            String batchNo,
            String serialNo,
            String containerNo,
            BigDecimal quantity,
            BigDecimal beforeQty,
            BigDecimal afterQty,
            BigDecimal unitCost,
            String businessType,
            String businessNo,
            Integer businessLine,
            String refTxnNo,
            String operator,
            String remark,
            String traceId) {
        InventoryTransaction txn = new InventoryTransaction();
        txn.setTxnNo(generateTxnNo());
        txn.setTxnType(txnType);
        txn.setTxnDirection(txnDirection);
        txn.setWarehouseCode(warehouseCode);
        txn.setOwnerCode(ownerCode);
        txn.setSkuCode(skuCode);
        txn.setSkuName(skuName);
        txn.setLocationCode(locationCode);
        txn.setBatchNo(batchNo);
        txn.setSerialNo(serialNo);
        txn.setContainerNo(containerNo);
        txn.setQuantity(quantity);
        txn.setBeforeQty(beforeQty);
        txn.setAfterQty(afterQty);
        txn.setUnitCost(unitCost);
        if (unitCost != null && quantity != null) {
            txn.setTotalCost(unitCost.multiply(quantity));
        }
        txn.setBusinessType(businessType);
        txn.setBusinessNo(businessNo);
        txn.setBusinessLine(businessLine);
        txn.setRefTxnNo(refTxnNo);
        txn.setOperator(operator);
        txn.setOperateTime(LocalDateTime.now());
        txn.setRemark(remark);
        txn.setTraceId(traceId);
        txnMapper.insert(txn);

        log.debug(
                "记录库存流水: txnNo={}, type={}, direction={}, sku={}, qty={}, before={}, after={}",
                txn.getTxnNo(),
                txnType,
                txnDirection,
                skuCode,
                quantity,
                beforeQty,
                afterQty);
        return txn;
    }

    // ============================================================

    // 2. 流水查询
    // ============================================================

    public InventoryTransaction getByTxnNo(String txnNo) {
        return txnMapper.selectByTxnNo(txnNo);
    }

    public List<InventoryTransaction> getByBusinessNo(String businessNo) {
        return txnMapper.selectByBusinessNo(businessNo);
    }

    public List<InventoryTransaction> getBySkuAndTime(
            String skuCode, String warehouseCode, LocalDateTime startTime, LocalDateTime endTime) {
        return txnMapper.selectBySkuAndTime(skuCode, warehouseCode, startTime, endTime);
    }

    public List<InventoryTransaction> getByBatchNo(String batchNo) {
        return txnMapper.selectByBatchNo(batchNo);
    }

    public List<InventoryTransaction> getByRefTxnNo(String refTxnNo) {
        return txnMapper.selectByRefTxnNo(refTxnNo);
    }

    public Page<InventoryTransaction> pageTransactions(
            Page<InventoryTransaction> page,
            String warehouseCode,
            String skuCode,
            String txnType,
            String businessNo,
            LocalDateTime startTime,
            LocalDateTime endTime) {
        LambdaQueryWrapper<InventoryTransaction> wrapper = new LambdaQueryWrapper<>();
        if (warehouseCode != null)
            wrapper.eq(InventoryTransaction::getWarehouseCode, warehouseCode);
        if (skuCode != null) wrapper.eq(InventoryTransaction::getSkuCode, skuCode);
        if (txnType != null) wrapper.eq(InventoryTransaction::getTxnType, txnType);
        if (businessNo != null) wrapper.eq(InventoryTransaction::getBusinessNo, businessNo);
        if (startTime != null) wrapper.ge(InventoryTransaction::getOperateTime, startTime);
        if (endTime != null) wrapper.le(InventoryTransaction::getOperateTime, endTime);
        wrapper.orderByDesc(InventoryTransaction::getOperateTime);
        return txnMapper.selectPage(page, wrapper);
    }

    // ============================================================

    // 3. 流水汇总（按日/SKU/库位）
    // ============================================================

    /** 执行日汇总 */
    @Transactional(rollbackFor = Exception.class)
    public List<InventoryTxnSummary> dailySummary(LocalDate summaryDate, String warehouseCode) {
        LocalDateTime startTime = summaryDate.atStartOfDay();
        LocalDateTime endTime = summaryDate.plusDays(1).atStartOfDay();

        // 查询当日所有流水
        List<InventoryTransaction> txns =
                txnMapper.selectList(
                        new LambdaQueryWrapper<InventoryTransaction>()
                                .eq(InventoryTransaction::getWarehouseCode, warehouseCode)
                                .ge(InventoryTransaction::getOperateTime, startTime)
                                .lt(InventoryTransaction::getOperateTime, endTime));

        // 按SKU+库位+批次分组
        Map<String, List<InventoryTransaction>> grouped = new HashMap<>();
        for (InventoryTransaction txn : txns) {
            String key = txn.getSkuCode() + "|" + txn.getLocationCode() + "|" + txn.getBatchNo();
            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(txn);
        }

        List<InventoryTxnSummary> summaries = new ArrayList<>();
        for (Map.Entry<String, List<InventoryTransaction>> entry : grouped.entrySet()) {
            String[] parts = entry.getKey().split("\\|");
            String skuCode = parts[0];
            String locationCode = parts[1].equals("null") ? null : parts[1];
            String batchNo = parts[2].equals("null") ? null : parts[2];

            InventoryTxnSummary summary =
                    calculateSummary(
                            summaryDate,
                            warehouseCode,
                            skuCode,
                            locationCode,
                            batchNo,
                            entry.getValue());
            summaryMapper.insert(summary);
            summaries.add(summary);
        }

        log.info(
                "日汇总完成: date={}, warehouse={}, skuCount={}",
                summaryDate,
                warehouseCode,
                summaries.size());
        return summaries;
    }

    private InventoryTxnSummary calculateSummary(
            LocalDate summaryDate,
            String warehouseCode,
            String skuCode,
            String locationCode,
            String batchNo,
            List<InventoryTransaction> txns) {
        BigDecimal inboundQty = BigDecimal.ZERO;
        BigDecimal outboundQty = BigDecimal.ZERO;
        BigDecimal adjustInQty = BigDecimal.ZERO;
        BigDecimal adjustOutQty = BigDecimal.ZERO;
        BigDecimal transferInQty = BigDecimal.ZERO;
        BigDecimal transferOutQty = BigDecimal.ZERO;
        BigDecimal inboundCost = BigDecimal.ZERO;
        BigDecimal outboundCost = BigDecimal.ZERO;

        BigDecimal beginQty = null;
        BigDecimal endQty = null;
        BigDecimal beginCost = null;
        BigDecimal endCost = null;

        for (InventoryTransaction txn : txns) {
            if ("IN".equals(txn.getTxnDirection())) {
                if ("INBOUND".equals(txn.getTxnType())) {
                    inboundQty = inboundQty.add(txn.getQuantity());
                    if (txn.getTotalCost() != null)
                        inboundCost = inboundCost.add(txn.getTotalCost());
                } else if ("ADJUST".equals(txn.getTxnType())) {
                    adjustInQty = adjustInQty.add(txn.getQuantity());
                } else if ("TRANSFER".equals(txn.getTxnType())) {
                    transferInQty = transferInQty.add(txn.getQuantity());
                }
            } else if ("OUT".equals(txn.getTxnDirection())) {
                if ("OUTBOUND".equals(txn.getTxnType())) {
                    outboundQty = outboundQty.add(txn.getQuantity());
                    if (txn.getTotalCost() != null)
                        outboundCost = outboundCost.add(txn.getTotalCost());
                } else if ("ADJUST".equals(txn.getTxnType())) {
                    adjustOutQty = adjustOutQty.add(txn.getQuantity());
                } else if ("TRANSFER".equals(txn.getTxnType())) {
                    transferOutQty = transferOutQty.add(txn.getQuantity());
                }
            }

            if (beginQty == null) beginQty = txn.getBeforeQty();
            endQty = txn.getAfterQty();
            if (beginCost == null && txn.getUnitCost() != null && txn.getBeforeQty() != null) {
                beginCost = txn.getUnitCost().multiply(txn.getBeforeQty());
            }
            if (txn.getUnitCost() != null && txn.getAfterQty() != null) {
                endCost = txn.getUnitCost().multiply(txn.getAfterQty());
            }
        }

        InventoryTxnSummary summary = new InventoryTxnSummary();
        summary.setSummaryDate(summaryDate);
        summary.setWarehouseCode(warehouseCode);
        summary.setSkuCode(skuCode);
        summary.setLocationCode(locationCode);
        summary.setBatchNo(batchNo);
        summary.setBeginQty(beginQty);
        summary.setInboundQty(inboundQty);
        summary.setOutboundQty(outboundQty);
        summary.setAdjustInQty(adjustInQty);
        summary.setAdjustOutQty(adjustOutQty);
        summary.setTransferInQty(transferInQty);
        summary.setTransferOutQty(transferOutQty);
        summary.setEndQty(endQty);
        summary.setBeginCost(beginCost);
        summary.setInboundCost(inboundCost);
        summary.setOutboundCost(outboundCost);
        summary.setEndCost(endCost);
        summary.setTxnCount(txns.size());
        summary.setSummaryTime(LocalDateTime.now());
        return summary;
    }

    public List<InventoryTxnSummary> getSummaryByDateAndWarehouse(
            LocalDate summaryDate, String warehouseCode) {
        return summaryMapper.selectByDateAndWarehouse(summaryDate, warehouseCode);
    }

    public List<InventoryTxnSummary> getSummaryBySkuAndDateRange(
            String skuCode, String warehouseCode, LocalDate startDate, LocalDate endDate) {
        return summaryMapper.selectBySkuAndDateRange(skuCode, warehouseCode, startDate, endDate);
    }

    // ============================================================

    // 4. 库存对账
    // ============================================================

    /** 执行库存对账 */
    @Transactional(rollbackFor = Exception.class)
    public InventoryReconcile reconcile(
            String reconcileType,
            String warehouseCode,
            String ownerCode,
            LocalDate reconcileDate,
            Map<String, BigDecimal> actualStock) {
        // 获取系统库存（从汇总表或实时查询）
        List<InventoryTxnSummary> summaries =
                summaryMapper.selectByDateAndWarehouse(reconcileDate, warehouseCode);

        BigDecimal systemBeginQty = BigDecimal.ZERO;
        BigDecimal systemEndQty = BigDecimal.ZERO;
        BigDecimal actualBeginQty = BigDecimal.ZERO;
        BigDecimal actualEndQty = BigDecimal.ZERO;
        int diffCount = 0;

        InventoryReconcile reconcile = new InventoryReconcile();
        reconcile.setReconcileNo(generateReconcileNo());
        reconcile.setReconcileType(reconcileType);
        reconcile.setWarehouseCode(warehouseCode);
        reconcile.setOwnerCode(ownerCode);
        reconcile.setReconcileDate(reconcileDate);
        reconcile.setStatus("PENDING");

        // 对比系统和实际库存
        for (InventoryTxnSummary summary : summaries) {
            systemBeginQty =
                    systemBeginQty.add(
                            summary.getBeginQty() != null
                                    ? summary.getBeginQty()
                                    : BigDecimal.ZERO);
            systemEndQty =
                    systemEndQty.add(
                            summary.getEndQty() != null ? summary.getEndQty() : BigDecimal.ZERO);

            String key =
                    summary.getSkuCode()
                            + "|"
                            + summary.getLocationCode()
                            + "|"
                            + summary.getBatchNo();
            BigDecimal actual = actualStock.getOrDefault(key, summary.getEndQty());
            actualEndQty = actualEndQty.add(actual);

            BigDecimal diff =
                    actual.subtract(
                            summary.getEndQty() != null ? summary.getEndQty() : BigDecimal.ZERO);
            if (diff.compareTo(BigDecimal.ZERO) != 0) {
                diffCount++;
                // 记录差异
                createReconcileDiff(
                        reconcile.getReconcileNo(), warehouseCode, summary, actual, diff);
            }
        }

        reconcile.setBeginQty(systemBeginQty);
        reconcile.setEndQty(systemEndQty);
        reconcile.setActualBeginQty(actualBeginQty);
        reconcile.setActualEndQty(actualEndQty);
        reconcile.setDiffQty(actualEndQty.subtract(systemEndQty));
        reconcile.setDiffCount(diffCount);
        reconcileMapper.insert(reconcile);

        log.info(
                "库存对账完成: reconcileNo={}, type={}, warehouse={}, diffCount={}, diffQty={}",
                reconcile.getReconcileNo(),
                reconcileType,
                warehouseCode,
                diffCount,
                reconcile.getDiffQty());
        return reconcile;
    }

    private void createReconcileDiff(
            String reconcileNo,
            String warehouseCode,
            InventoryTxnSummary summary,
            BigDecimal actualQty,
            BigDecimal diffQty) {
        InventoryReconcileDiff diff = new InventoryReconcileDiff();
        diff.setDiffId(generateDiffId());
        diff.setReconcileNo(reconcileNo);
        diff.setWarehouseCode(warehouseCode);
        diff.setSkuCode(summary.getSkuCode());
        diff.setLocationCode(summary.getLocationCode());
        diff.setBatchNo(summary.getBatchNo());
        diff.setSystemQty(summary.getEndQty());
        diff.setActualQty(actualQty);
        diff.setDiffQty(diffQty);
        diff.setDiffType(diffQty.compareTo(BigDecimal.ZERO) < 0 ? "SHORTAGE" : "OVERAGE");
        diff.setStatus("PENDING");
        diffMapper.insert(diff);
    }

    @Transactional(rollbackFor = Exception.class)
    public InventoryReconcile resolveReconcile(String reconcileNo, String operator, String remark) {
        InventoryReconcile reconcile = reconcileMapper.selectByReconcileNo(reconcileNo);
        if (reconcile == null) throw new RuntimeException("对账单不存在: " + reconcileNo);
        reconcileMapper.updateStatus(reconcileNo, "RESOLVED", operator, remark);
        log.info("解决库存对账: reconcileNo={}, operator={}", reconcileNo, operator);
        return reconcileMapper.selectByReconcileNo(reconcileNo);
    }

    @Transactional(rollbackFor = Exception.class)
    public InventoryReconcileDiff resolveDiff(
            String diffId, String resolveAction, String resolvedBy) {
        InventoryReconcileDiff diff = diffMapper.selectByDiffId(diffId);
        if (diff == null) throw new RuntimeException("差异记录不存在: " + diffId);
        diffMapper.updateStatus(diffId, "RESOLVED", resolveAction, resolvedBy);
        log.info("解决对账差异: diffId={}, action={}", diffId, resolveAction);
        return diffMapper.selectByDiffId(diffId);
    }

    public InventoryReconcile getReconcileByNo(String reconcileNo) {
        return reconcileMapper.selectByReconcileNo(reconcileNo);
    }

    public List<InventoryReconcileDiff> getDiffsByReconcileNo(String reconcileNo) {
        return diffMapper.selectByReconcileNo(reconcileNo);
    }

    public Page<InventoryReconcile> pageReconciles(
            Page<InventoryReconcile> page, String warehouseCode, String status) {
        LambdaQueryWrapper<InventoryReconcile> wrapper = new LambdaQueryWrapper<>();
        if (warehouseCode != null) wrapper.eq(InventoryReconcile::getWarehouseCode, warehouseCode);
        if (status != null) wrapper.eq(InventoryReconcile::getStatus, status);
        wrapper.orderByDesc(InventoryReconcile::getReconcileDate);
        return reconcileMapper.selectPage(page, wrapper);
    }

    // ============================================================

    // 工具方法
    // ============================================================

    private String generateTxnNo() {
        return "TXN"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }

    private String generateReconcileNo() {
        return "REC"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }

    private String generateDiffId() {
        return "DIF"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }
}
