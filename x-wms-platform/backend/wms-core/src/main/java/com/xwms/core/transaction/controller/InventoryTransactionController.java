package com.xwms.core.transaction.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.common.core.Result;
import com.xwms.core.transaction.entity.*;
import com.xwms.core.transaction.service.InventoryTransactionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/** 库存流水管理 Controller */
@Tag(name = "库存流水管理", description = "流水记录/流水查询/流水汇总/库存对账")
@RestController
@RequestMapping("/api/transaction")
@RequiredArgsConstructor
public class InventoryTransactionController {

    private final InventoryTransactionService txnService;

    // ============================================================

    // 流水记录
    // ============================================================

    @Operation(summary = "记录库存流水")
    @PostMapping("/record")
    public Result<InventoryTransaction> recordTransaction(
            @RequestParam String txnType,
            @RequestParam String txnDirection,
            @RequestParam String warehouseCode,
            @RequestParam(required = false) String ownerCode,
            @RequestParam String skuCode,
            @RequestParam(required = false) String skuName,
            @RequestParam(required = false) String locationCode,
            @RequestParam(required = false) String batchNo,
            @RequestParam(required = false) String serialNo,
            @RequestParam(required = false) String containerNo,
            @RequestParam BigDecimal quantity,
            @RequestParam(required = false) BigDecimal beforeQty,
            @RequestParam(required = false) BigDecimal afterQty,
            @RequestParam(required = false) BigDecimal unitCost,
            @RequestParam(required = false) String businessType,
            @RequestParam(required = false) String businessNo,
            @RequestParam(required = false) Integer businessLine,
            @RequestParam(required = false) String refTxnNo,
            @RequestParam(required = false) String operator,
            @RequestParam(required = false) String remark,
            @RequestParam(required = false) String traceId) {
        return Result.success(
                txnService.recordTransaction(
                        txnType,
                        txnDirection,
                        warehouseCode,
                        ownerCode,
                        skuCode,
                        skuName,
                        locationCode,
                        batchNo,
                        serialNo,
                        containerNo,
                        quantity,
                        beforeQty,
                        afterQty,
                        unitCost,
                        businessType,
                        businessNo,
                        businessLine,
                        refTxnNo,
                        operator,
                        remark,
                        traceId));
    }

    // ============================================================

    // 流水查询
    // ============================================================

    @Operation(summary = "按流水号查询")
    @GetMapping("/{txnNo}")
    public Result<InventoryTransaction> getByTxnNo(@PathVariable String txnNo) {
        return Result.success(txnService.getByTxnNo(txnNo));
    }

    @Operation(summary = "按业务单号查询流水")
    @GetMapping("/business/{businessNo}")
    public Result<List<InventoryTransaction>> getByBusinessNo(@PathVariable String businessNo) {
        return Result.success(txnService.getByBusinessNo(businessNo));
    }

    @Operation(summary = "按SKU和时间查询流水")
    @GetMapping("/sku/{skuCode}")
    public Result<List<InventoryTransaction>> getBySkuAndTime(
            @PathVariable String skuCode,
            @RequestParam String warehouseCode,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        return Result.success(
                txnService.getBySkuAndTime(skuCode, warehouseCode, startTime, endTime));
    }

    @Operation(summary = "按批次查询流水")
    @GetMapping("/batch/{batchNo}")
    public Result<List<InventoryTransaction>> getByBatchNo(@PathVariable String batchNo) {
        return Result.success(txnService.getByBatchNo(batchNo));
    }

    @Operation(summary = "按关联流水号查询")
    @GetMapping("/ref/{refTxnNo}")
    public Result<List<InventoryTransaction>> getByRefTxnNo(@PathVariable String refTxnNo) {
        return Result.success(txnService.getByRefTxnNo(refTxnNo));
    }

    @Operation(summary = "分页查询流水")
    @GetMapping("/list")
    public Result<Page<InventoryTransaction>> pageTransactions(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String warehouseCode,
            @RequestParam(required = false) String skuCode,
            @RequestParam(required = false) String txnType,
            @RequestParam(required = false) String businessNo,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
                    LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
                    LocalDateTime endTime) {
        return Result.success(
                txnService.pageTransactions(
                        new Page<>(page, size),
                        warehouseCode,
                        skuCode,
                        txnType,
                        businessNo,
                        startTime,
                        endTime));
    }

    // ============================================================

    // 流水汇总
    // ============================================================

    @Operation(summary = "执行日汇总")
    @PostMapping("/summary/daily")
    public Result<List<InventoryTxnSummary>> dailySummary(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate summaryDate,
            @RequestParam String warehouseCode) {
        return Result.success(txnService.dailySummary(summaryDate, warehouseCode));
    }

    @Operation(summary = "按日期和仓库查询汇总")
    @GetMapping("/summary/date/{summaryDate}/{warehouseCode}")
    public Result<List<InventoryTxnSummary>> getSummaryByDateAndWarehouse(
            @PathVariable @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate summaryDate,
            @PathVariable String warehouseCode) {
        return Result.success(txnService.getSummaryByDateAndWarehouse(summaryDate, warehouseCode));
    }

    @Operation(summary = "按SKU和日期范围查询汇总")
    @GetMapping("/summary/sku/{skuCode}")
    public Result<List<InventoryTxnSummary>> getSummaryBySkuAndDateRange(
            @PathVariable String skuCode,
            @RequestParam String warehouseCode,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        return Result.success(
                txnService.getSummaryBySkuAndDateRange(skuCode, warehouseCode, startDate, endDate));
    }

    // ============================================================

    // 库存对账
    // ============================================================

    @Operation(summary = "执行库存对账")
    @PostMapping("/reconcile")
    public Result<InventoryReconcile> reconcile(
            @RequestParam String reconcileType,
            @RequestParam String warehouseCode,
            @RequestParam(required = false) String ownerCode,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate reconcileDate,
            @RequestBody Map<String, BigDecimal> actualStock) {
        return Result.success(
                txnService.reconcile(
                        reconcileType, warehouseCode, ownerCode, reconcileDate, actualStock));
    }

    @Operation(summary = "解决对账单")
    @PostMapping("/reconcile/{reconcileNo}/resolve")
    public Result<InventoryReconcile> resolveReconcile(
            @PathVariable String reconcileNo,
            @RequestParam String operator,
            @RequestParam(required = false) String remark) {
        return Result.success(txnService.resolveReconcile(reconcileNo, operator, remark));
    }

    @Operation(summary = "解决对账差异")
    @PostMapping("/reconcile/diff/{diffId}/resolve")
    public Result<InventoryReconcileDiff> resolveDiff(
            @PathVariable String diffId,
            @RequestParam String resolveAction,
            @RequestParam String resolvedBy) {
        return Result.success(txnService.resolveDiff(diffId, resolveAction, resolvedBy));
    }

    @Operation(summary = "按对账号查询")
    @GetMapping("/reconcile/{reconcileNo}")
    public Result<InventoryReconcile> getReconcileByNo(@PathVariable String reconcileNo) {
        return Result.success(txnService.getReconcileByNo(reconcileNo));
    }

    @Operation(summary = "查询对账差异")
    @GetMapping("/reconcile/{reconcileNo}/diffs")
    public Result<List<InventoryReconcileDiff>> getDiffsByReconcileNo(
            @PathVariable String reconcileNo) {
        return Result.success(txnService.getDiffsByReconcileNo(reconcileNo));
    }

    @Operation(summary = "分页查询对账单")
    @GetMapping("/reconcile/list")
    public Result<Page<InventoryReconcile>> pageReconciles(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String warehouseCode,
            @RequestParam(required = false) String status) {
        return Result.success(
                txnService.pageReconciles(new Page<>(page, size), warehouseCode, status));
    }
}
