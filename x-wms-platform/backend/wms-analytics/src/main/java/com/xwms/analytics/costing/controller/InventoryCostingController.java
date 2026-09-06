package com.xwms.analytics.costing.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.analytics.costing.entity.*;
import com.xwms.analytics.costing.service.InventoryCostingService;
import com.xwms.common.core.Result;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/** 库存成本核算管理 Controller */
@Tag(name = "库存成本核算管理", description = "成本核算/成本调整/成本分摊/成本明细")
@RestController
@RequestMapping("/api/costing")
@RequiredArgsConstructor
public class InventoryCostingController {

    private final InventoryCostingService costingService;

    // ============================================================

    // 成本核算
    // ============================================================

    @Operation(summary = "创建成本核算")
    @PostMapping("/calculate")
    public Result<CostCalculate> createCalculate(
            @RequestParam(required = false) String calculateName,
            @RequestParam String calculateType,
            @RequestParam String warehouseCode,
            @RequestParam(required = false) String ownerCode,
            @RequestParam LocalDate periodStart,
            @RequestParam LocalDate periodEnd,
            @RequestParam String costingMethod,
            @RequestParam String operator) {
        return Result.success(
                costingService.createCalculate(
                        calculateName,
                        calculateType,
                        warehouseCode,
                        ownerCode,
                        periodStart,
                        periodEnd,
                        costingMethod,
                        operator));
    }

    @Operation(summary = "开始成本核算")
    @PostMapping("/calculate/{calculateId}/start")
    public Result<CostCalculate> startCalculate(@PathVariable String calculateId) {
        return Result.success(costingService.startCalculate(calculateId));
    }

    @Operation(summary = "完成成本核算")
    @PostMapping("/calculate/{calculateId}/complete")
    public Result<CostCalculate> completeCalculate(
            @PathVariable String calculateId,
            @RequestParam int calculatedCount,
            @RequestParam int failedCount,
            @RequestParam(required = false) BigDecimal totalQuantity,
            @RequestParam(required = false) BigDecimal totalAmount,
            @RequestParam(required = false) BigDecimal averageCost,
            @RequestParam Long durationMs,
            @RequestParam(required = false) String errorMessage) {
        return Result.success(
                costingService.completeCalculate(
                        calculateId,
                        calculatedCount,
                        failedCount,
                        totalQuantity,
                        totalAmount,
                        averageCost,
                        durationMs,
                        errorMessage));
    }

    @Operation(summary = "执行成本核算")
    @PostMapping("/calculate/{calculateId}/execute")
    public Result<CostCalculate> executeCalculate(@PathVariable String calculateId) {
        return Result.success(costingService.executeCalculate(calculateId));
    }

    @Operation(summary = "按ID查询成本核算")
    @GetMapping("/calculate/{calculateId}")
    public Result<CostCalculate> getCalculateById(@PathVariable String calculateId) {
        return Result.success(costingService.getCalculateById(calculateId));
    }

    @Operation(summary = "按仓库和期间查询成本核算")
    @GetMapping("/calculate/warehouse-period")
    public Result<CostCalculate> getCalculateByWarehouseAndPeriod(
            @RequestParam String warehouseCode,
            @RequestParam LocalDate periodStart,
            @RequestParam LocalDate periodEnd) {
        return Result.success(
                costingService.getCalculateByWarehouseAndPeriod(
                        warehouseCode, periodStart, periodEnd));
    }

    @Operation(summary = "按状态查询成本核算")
    @GetMapping("/calculate/status/{status}")
    public Result<List<CostCalculate>> getCalculatesByStatus(@PathVariable String status) {
        return Result.success(costingService.getCalculatesByStatus(status));
    }

    @Operation(summary = "查询最近成本核算")
    @GetMapping("/calculate/recent")
    public Result<List<CostCalculate>> getRecentCalculatesByWarehouseAndType(
            @RequestParam String warehouseCode,
            @RequestParam String calculateType,
            @RequestParam(defaultValue = "10") int limit) {
        return Result.success(
                costingService.getRecentCalculatesByWarehouseAndType(
                        warehouseCode, calculateType, limit));
    }

    @Operation(summary = "分页查询成本核算")
    @GetMapping("/calculate/list")
    public Result<Page<CostCalculate>> pageCalculates(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String warehouseCode,
            @RequestParam(required = false) String calculateType,
            @RequestParam(required = false) String status) {
        return Result.success(
                costingService.pageCalculates(
                        new Page<>(page, size), warehouseCode, calculateType, status));
    }

    // ============================================================

    // 成本调整
    // ============================================================

    @Operation(summary = "创建成本调整")
    @PostMapping("/adjust")
    public Result<CostAdjust> createAdjust(
            @RequestParam String adjustType,
            @RequestParam String warehouseCode,
            @RequestParam(required = false) String ownerCode,
            @RequestParam String skuCode,
            @RequestParam(required = false) String batchNo,
            @RequestParam(required = false) String locationCode,
            @RequestParam(required = false) BigDecimal beforeQuantity,
            @RequestParam(required = false) BigDecimal afterQuantity,
            @RequestParam(required = false) BigDecimal beforeCost,
            @RequestParam(required = false) BigDecimal afterCost,
            @RequestParam(required = false) BigDecimal beforeAmount,
            @RequestParam(required = false) BigDecimal afterAmount,
            @RequestParam String adjustReason,
            @RequestParam(required = false) String adjustBasis,
            @RequestParam(required = false) String relatedBizType,
            @RequestParam(required = false) String relatedBizNo,
            @RequestParam String operator) {
        return Result.success(
                costingService.createAdjust(
                        adjustType,
                        warehouseCode,
                        ownerCode,
                        skuCode,
                        batchNo,
                        locationCode,
                        beforeQuantity,
                        afterQuantity,
                        beforeCost,
                        afterCost,
                        beforeAmount,
                        afterAmount,
                        adjustReason,
                        adjustBasis,
                        relatedBizType,
                        relatedBizNo,
                        operator));
    }

    @Operation(summary = "审核成本调整")
    @PostMapping("/adjust/{adjustId}/approve")
    public Result<CostAdjust> approveAdjust(
            @PathVariable String adjustId,
            @RequestParam String approver,
            @RequestParam(required = false) String approveOpinion,
            @RequestParam boolean approved) {
        return Result.success(
                costingService.approveAdjust(adjustId, approver, approveOpinion, approved));
    }

    @Operation(summary = "按ID查询成本调整")
    @GetMapping("/adjust/{adjustId}")
    public Result<CostAdjust> getAdjustById(@PathVariable String adjustId) {
        return Result.success(costingService.getAdjustById(adjustId));
    }

    @Operation(summary = "按SKU查询成本调整")
    @GetMapping("/adjust/sku/{skuCode}")
    public Result<List<CostAdjust>> getAdjustsBySku(
            @PathVariable String skuCode, @RequestParam(defaultValue = "20") int limit) {
        return Result.success(costingService.getAdjustsBySku(skuCode, limit));
    }

    @Operation(summary = "按状态查询成本调整")
    @GetMapping("/adjust/status/{status}")
    public Result<List<CostAdjust>> getAdjustsByStatus(@PathVariable String status) {
        return Result.success(costingService.getAdjustsByStatus(status));
    }

    @Operation(summary = "按仓库和类型查询成本调整")
    @GetMapping("/adjust/warehouse-type")
    public Result<List<CostAdjust>> getAdjustsByWarehouseAndType(
            @RequestParam String warehouseCode,
            @RequestParam String adjustType,
            @RequestParam(defaultValue = "20") int limit) {
        return Result.success(
                costingService.getAdjustsByWarehouseAndType(warehouseCode, adjustType, limit));
    }

    @Operation(summary = "分页查询成本调整")
    @GetMapping("/adjust/list")
    public Result<Page<CostAdjust>> pageAdjusts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String warehouseCode,
            @RequestParam(required = false) String adjustType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String skuCode) {
        return Result.success(
                costingService.pageAdjusts(
                        new Page<>(page, size), warehouseCode, adjustType, status, skuCode));
    }

    // ============================================================

    // 成本分摊
    // ============================================================

    @Operation(summary = "创建成本分摊")
    @PostMapping("/allocation")
    public Result<CostAllocation> createAllocation(
            @RequestParam(required = false) String allocationName,
            @RequestParam String allocationType,
            @RequestParam String warehouseCode,
            @RequestParam(required = false) String ownerCode,
            @RequestParam(required = false) LocalDate periodStart,
            @RequestParam(required = false) LocalDate periodEnd,
            @RequestParam BigDecimal totalAmount,
            @RequestParam String allocationMethod,
            @RequestParam String operator) {
        return Result.success(
                costingService.createAllocation(
                        allocationName,
                        allocationType,
                        warehouseCode,
                        ownerCode,
                        periodStart,
                        periodEnd,
                        totalAmount,
                        allocationMethod,
                        operator));
    }

    @Operation(summary = "开始成本分摊")
    @PostMapping("/allocation/{allocationId}/start")
    public Result<CostAllocation> startAllocation(@PathVariable String allocationId) {
        return Result.success(costingService.startAllocation(allocationId));
    }

    @Operation(summary = "完成成本分摊")
    @PostMapping("/allocation/{allocationId}/complete")
    public Result<CostAllocation> completeAllocation(
            @PathVariable String allocationId,
            @RequestParam int allocatedCount,
            @RequestParam BigDecimal allocatedAmount,
            @RequestParam Long durationMs,
            @RequestParam(required = false) String errorMessage) {
        return Result.success(
                costingService.completeAllocation(
                        allocationId, allocatedCount, allocatedAmount, durationMs, errorMessage));
    }

    @Operation(summary = "执行成本分摊")
    @PostMapping("/allocation/{allocationId}/execute")
    public Result<CostAllocation> executeAllocation(@PathVariable String allocationId) {
        return Result.success(costingService.executeAllocation(allocationId));
    }

    @Operation(summary = "按ID查询成本分摊")
    @GetMapping("/allocation/{allocationId}")
    public Result<CostAllocation> getAllocationById(@PathVariable String allocationId) {
        return Result.success(costingService.getAllocationById(allocationId));
    }

    @Operation(summary = "按状态查询成本分摊")
    @GetMapping("/allocation/status/{status}")
    public Result<List<CostAllocation>> getAllocationsByStatus(@PathVariable String status) {
        return Result.success(costingService.getAllocationsByStatus(status));
    }

    @Operation(summary = "按仓库和类型查询成本分摊")
    @GetMapping("/allocation/warehouse-type")
    public Result<List<CostAllocation>> getAllocationsByWarehouseAndType(
            @RequestParam String warehouseCode,
            @RequestParam String allocationType,
            @RequestParam(defaultValue = "10") int limit) {
        return Result.success(
                costingService.getAllocationsByWarehouseAndType(
                        warehouseCode, allocationType, limit));
    }

    @Operation(summary = "分页查询成本分摊")
    @GetMapping("/allocation/list")
    public Result<Page<CostAllocation>> pageAllocations(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String warehouseCode,
            @RequestParam(required = false) String allocationType,
            @RequestParam(required = false) String status) {
        return Result.success(
                costingService.pageAllocations(
                        new Page<>(page, size), warehouseCode, allocationType, status));
    }

    // ============================================================

    // 成本明细
    // ============================================================

    @Operation(summary = "按ID查询成本明细")
    @GetMapping("/detail/{detailId}")
    public Result<CostDetail> getDetailById(@PathVariable String detailId) {
        return Result.success(costingService.getDetailById(detailId));
    }

    @Operation(summary = "按核算ID查询成本明细")
    @GetMapping("/detail/calculate/{calculateId}")
    public Result<List<CostDetail>> getDetailsByCalculateId(@PathVariable String calculateId) {
        return Result.success(costingService.getDetailsByCalculateId(calculateId));
    }

    @Operation(summary = "按调整ID查询成本明细")
    @GetMapping("/detail/adjust/{adjustId}")
    public Result<List<CostDetail>> getDetailsByAdjustId(@PathVariable String adjustId) {
        return Result.success(costingService.getDetailsByAdjustId(adjustId));
    }

    @Operation(summary = "按分摊ID查询成本明细")
    @GetMapping("/detail/allocation/{allocationId}")
    public Result<List<CostDetail>> getDetailsByAllocationId(@PathVariable String allocationId) {
        return Result.success(costingService.getDetailsByAllocationId(allocationId));
    }

    @Operation(summary = "按SKU和期间查询成本明细")
    @GetMapping("/detail/sku-period")
    public Result<List<CostDetail>> getDetailsBySkuAndPeriod(
            @RequestParam String skuCode,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {
        return Result.success(costingService.getDetailsBySkuAndPeriod(skuCode, startDate, endDate));
    }

    @Operation(summary = "按业务查询成本明细")
    @GetMapping("/detail/biz")
    public Result<List<CostDetail>> getDetailsByBiz(
            @RequestParam String bizType, @RequestParam String bizNo) {
        return Result.success(costingService.getDetailsByBiz(bizType, bizNo));
    }

    @Operation(summary = "汇总成本")
    @GetMapping("/detail/sum-cost")
    public Result<BigDecimal> sumCostByWarehouseAndPeriod(
            @RequestParam String warehouseCode,
            @RequestParam LocalDate periodDate,
            @RequestParam String bizType) {
        return Result.success(
                costingService.sumCostByWarehouseAndPeriod(warehouseCode, periodDate, bizType));
    }

    @Operation(summary = "分页查询成本明细")
    @GetMapping("/detail/list")
    public Result<Page<CostDetail>> pageDetails(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String warehouseCode,
            @RequestParam(required = false) String skuCode,
            @RequestParam(required = false) String bizType,
            @RequestParam(required = false) String calculateId,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        return Result.success(
                costingService.pageDetails(
                        new Page<>(page, size),
                        warehouseCode,
                        skuCode,
                        bizType,
                        calculateId,
                        startDate,
                        endDate));
    }
}
