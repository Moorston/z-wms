package com.xwms.core.metrics.controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.common.core.Result;
import com.xwms.core.metrics.entity.*;
import com.xwms.core.metrics.service.InventoryMetricsService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "库存指标体系管理", description = "指标体系/指标分类/指标计算/指标监控")
@RestController
@RequestMapping("/api/metrics")
@RequiredArgsConstructor
public class InventoryMetricsController {

    private final InventoryMetricsService metricsService;

    // ==================== 指标体系 ====================

    @Operation(summary = "创建指标体系")
    @PostMapping("/system")
    public Result<MetricsSystem> createMetricsSystem(
            @RequestParam String systemName,
            @RequestParam String systemCode,
            @RequestParam String systemType,
            @RequestParam(required = false) String warehouseCode,
            @RequestParam(required = false) String ownerCode,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String systemConfig,
            @RequestParam(required = false) Integer sortOrder,
            @RequestParam String createdBy) {
        return Result.success(
                metricsService.createMetricsSystem(
                        systemName,
                        systemCode,
                        systemType,
                        warehouseCode,
                        ownerCode,
                        description,
                        systemConfig,
                        sortOrder,
                        createdBy));
    }

    @Operation(summary = "按类型获取活跃体系")
    @GetMapping("/system/type/{systemType}")
    public Result<List<MetricsSystem>> getActiveSystemsByType(@PathVariable String systemType) {
        return Result.success(metricsService.getActiveSystemsByType(systemType));
    }

    @Operation(summary = "获取默认体系")
    @GetMapping("/system/default")
    public Result<MetricsSystem> getDefaultSystem(@RequestParam String warehouseCode) {
        return Result.success(metricsService.getDefaultSystem(warehouseCode));
    }

    @Operation(summary = "分页查询指标体系")
    @GetMapping("/system/list")
    public Result<Page<MetricsSystem>> pageMetricsSystem(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String systemType,
            @RequestParam(required = false) String warehouseCode,
            @RequestParam(required = false) String isActive) {
        return Result.success(
                metricsService.pageMetricsSystem(
                        new Page<>(page, size), systemType, warehouseCode, isActive));
    }

    // ==================== 指标分类 ====================

    @Operation(summary = "创建指标分类")
    @PostMapping("/category")
    public Result<MetricsCategory> createMetricsCategory(
            @RequestParam String categoryName,
            @RequestParam String categoryCode,
            @RequestParam String systemId,
            @RequestParam(required = false) String parentCategoryId,
            @RequestParam(required = false) Integer categoryLevel,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String categoryConfig,
            @RequestParam(required = false) Integer sortOrder,
            @RequestParam String createdBy) {
        return Result.success(
                metricsService.createMetricsCategory(
                        categoryName,
                        categoryCode,
                        systemId,
                        parentCategoryId,
                        categoryLevel,
                        description,
                        categoryConfig,
                        sortOrder,
                        createdBy));
    }

    @Operation(summary = "按体系获取活跃分类")
    @GetMapping("/category/system/{systemId}")
    public Result<List<MetricsCategory>> getActiveCategoriesBySystem(
            @PathVariable String systemId) {
        return Result.success(metricsService.getActiveCategoriesBySystem(systemId));
    }

    @Operation(summary = "按父分类获取活跃分类")
    @GetMapping("/category/parent/{parentCategoryId}")
    public Result<List<MetricsCategory>> getActiveCategoriesByParent(
            @PathVariable String parentCategoryId) {
        return Result.success(metricsService.getActiveCategoriesByParent(parentCategoryId));
    }

    @Operation(summary = "分页查询指标分类")
    @GetMapping("/category/list")
    public Result<Page<MetricsCategory>> pageMetricsCategory(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String systemId,
            @RequestParam(required = false) String parentCategoryId,
            @RequestParam(required = false) String isActive) {
        return Result.success(
                metricsService.pageMetricsCategory(
                        new Page<>(page, size), systemId, parentCategoryId, isActive));
    }

    // ==================== 指标计算 ====================

    @Operation(summary = "创建指标计算")
    @PostMapping("/calculation")
    public Result<MetricsCalculation> createCalculation(
            @RequestParam String metricId,
            @RequestParam String metricCode,
            @RequestParam(required = false) String metricName,
            @RequestParam(required = false) String systemId,
            @RequestParam String warehouseCode,
            @RequestParam(required = false) String ownerCode,
            @RequestParam String periodType,
            @RequestParam LocalDateTime periodStart,
            @RequestParam LocalDateTime periodEnd,
            @RequestParam(required = false) String calcFormula,
            @RequestParam(required = false) String calcParams,
            @RequestParam(required = false) String calcUnit,
            @RequestParam(required = false) BigDecimal targetValue,
            @RequestParam(required = false) BigDecimal benchmarkValue,
            @RequestParam String operator) {
        return Result.success(
                metricsService.createCalculation(
                        metricId,
                        metricCode,
                        metricName,
                        systemId,
                        warehouseCode,
                        ownerCode,
                        periodType,
                        periodStart,
                        periodEnd,
                        calcFormula,
                        calcParams,
                        calcUnit,
                        targetValue,
                        benchmarkValue,
                        operator));
    }

    @Operation(summary = "执行指标计算")
    @PostMapping("/calculation/{calcId}/execute")
    public Result<MetricsCalculation> executeCalculation(
            @PathVariable String calcId,
            @RequestParam BigDecimal calcResult,
            @RequestParam String operator) {
        return Result.success(metricsService.executeCalculation(calcId, calcResult, operator));
    }

    @Operation(summary = "指标计算失败")
    @PostMapping("/calculation/{calcId}/fail")
    public Result<MetricsCalculation> failCalculation(
            @PathVariable String calcId,
            @RequestParam String errorMessage,
            @RequestParam String operator) {
        return Result.success(metricsService.failCalculation(calcId, errorMessage, operator));
    }

    @Operation(summary = "按指标获取最近计算")
    @GetMapping("/calculation/recent/{metricId}")
    public Result<List<MetricsCalculation>> getRecentCalculationsByMetric(
            @PathVariable String metricId, @RequestParam(defaultValue = "10") int limit) {
        return Result.success(metricsService.getRecentCalculationsByMetric(metricId, limit));
    }

    @Operation(summary = "按仓库和期间获取计算")
    @GetMapping("/calculation/warehouse-period")
    public Result<List<MetricsCalculation>> getCalculationsByWarehouseAndPeriod(
            @RequestParam String warehouseCode,
            @RequestParam LocalDateTime periodStart,
            @RequestParam LocalDateTime periodEnd) {
        return Result.success(
                metricsService.getCalculationsByWarehouseAndPeriod(
                        warehouseCode, periodStart, periodEnd));
    }

    @Operation(summary = "分页查询指标计算")
    @GetMapping("/calculation/list")
    public Result<Page<MetricsCalculation>> pageCalculation(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String metricId,
            @RequestParam(required = false) String warehouseCode,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String periodType) {
        return Result.success(
                metricsService.pageCalculation(
                        new Page<>(page, size), metricId, warehouseCode, status, periodType));
    }

    // ==================== 指标监控 ====================

    @Operation(summary = "创建指标监控")
    @PostMapping("/monitor")
    public Result<MetricsMonitor> createMetricsMonitor(
            @RequestParam String metricId,
            @RequestParam String metricCode,
            @RequestParam(required = false) String metricName,
            @RequestParam(required = false) String systemId,
            @RequestParam String warehouseCode,
            @RequestParam(required = false) String ownerCode,
            @RequestParam(required = false) String monitorType,
            @RequestParam(required = false) String monitorConfig,
            @RequestParam(required = false) BigDecimal targetValue,
            @RequestParam(required = false) BigDecimal thresholdWarning,
            @RequestParam(required = false) BigDecimal thresholdCritical,
            @RequestParam(required = false) String unit,
            @RequestParam(required = false) Integer checkInterval,
            @RequestParam String operator) {
        return Result.success(
                metricsService.createMetricsMonitor(
                        metricId,
                        metricCode,
                        metricName,
                        systemId,
                        warehouseCode,
                        ownerCode,
                        monitorType,
                        monitorConfig,
                        targetValue,
                        thresholdWarning,
                        thresholdCritical,
                        unit,
                        checkInterval,
                        operator));
    }

    @Operation(summary = "检查指标监控")
    @PostMapping("/monitor/{monitorId}/check")
    public Result<MetricsMonitor> checkMonitor(
            @PathVariable String monitorId,
            @RequestParam BigDecimal currentValue,
            @RequestParam String operator) {
        return Result.success(metricsService.checkMonitor(monitorId, currentValue, operator));
    }

    @Operation(summary = "按指标获取活跃监控")
    @GetMapping("/monitor/metric/{metricId}")
    public Result<MetricsMonitor> getActiveMonitorByMetric(@PathVariable String metricId) {
        return Result.success(metricsService.getActiveMonitorByMetric(metricId));
    }

    @Operation(summary = "按仓库获取活跃监控")
    @GetMapping("/monitor/warehouse/{warehouseCode}")
    public Result<List<MetricsMonitor>> getActiveMonitorsByWarehouse(
            @PathVariable String warehouseCode) {
        return Result.success(metricsService.getActiveMonitorsByWarehouse(warehouseCode));
    }

    @Operation(summary = "按状态获取活跃监控")
    @GetMapping("/monitor/status/{status}")
    public Result<List<MetricsMonitor>> getActiveMonitorsByStatus(@PathVariable String status) {
        return Result.success(metricsService.getActiveMonitorsByStatus(status));
    }

    @Operation(summary = "分页查询指标监控")
    @GetMapping("/monitor/list")
    public Result<Page<MetricsMonitor>> pageMonitor(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String metricId,
            @RequestParam(required = false) String warehouseCode,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String isActive) {
        return Result.success(
                metricsService.pageMonitor(
                        new Page<>(page, size), metricId, warehouseCode, status, isActive));
    }
}
