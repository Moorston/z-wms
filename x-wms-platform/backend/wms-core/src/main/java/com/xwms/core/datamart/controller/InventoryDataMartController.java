package com.xwms.core.datamart.controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.common.core.Result;
import com.xwms.core.datamart.entity.*;
import com.xwms.core.datamart.service.InventoryDataMartService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "库存数据集市管理", description = "数据集市/指标管理/维度管理/数据模型")
@RestController
@RequestMapping("/api/datamart")
@RequiredArgsConstructor
public class InventoryDataMartController {

    private final InventoryDataMartService dataMartService;

    // ==================== 数据集市 ====================

    @Operation(summary = "创建数据集市")
    @PostMapping("/mart")
    public Result<DataMart> createDataMart(
            @RequestParam String martName,
            @RequestParam String martCode,
            @RequestParam String martType,
            @RequestParam(required = false) String warehouseCode,
            @RequestParam(required = false) String ownerCode,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String dataSource,
            @RequestParam(required = false) String martConfig,
            @RequestParam(required = false) String refreshStrategy,
            @RequestParam(required = false) String refreshCron,
            @RequestParam(required = false) Integer sortOrder,
            @RequestParam String createdBy) {
        return Result.success(
                dataMartService.createDataMart(
                        martName,
                        martCode,
                        martType,
                        warehouseCode,
                        ownerCode,
                        description,
                        dataSource,
                        martConfig,
                        refreshStrategy,
                        refreshCron,
                        sortOrder,
                        createdBy));
    }

    @Operation(summary = "刷新数据集市")
    @PostMapping("/mart/{martId}/refresh")
    public Result<DataMart> refreshDataMart(
            @PathVariable String martId,
            @RequestParam Long recordCount,
            @RequestParam BigDecimal dataSizeMb,
            @RequestParam String operator) {
        return Result.success(
                dataMartService.refreshDataMart(martId, recordCount, dataSizeMb, operator));
    }

    @Operation(summary = "按类型获取活跃集市")
    @GetMapping("/mart/type/{martType}")
    public Result<List<DataMart>> getActiveMartsByType(@PathVariable String martType) {
        return Result.success(dataMartService.getActiveMartsByType(martType));
    }

    @Operation(summary = "按仓库获取活跃集市")
    @GetMapping("/mart/warehouse/{warehouseCode}")
    public Result<List<DataMart>> getActiveMartsByWarehouse(@PathVariable String warehouseCode) {
        return Result.success(dataMartService.getActiveMartsByWarehouse(warehouseCode));
    }

    @Operation(summary = "分页查询数据集市")
    @GetMapping("/mart/list")
    public Result<Page<DataMart>> pageDataMart(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String martType,
            @RequestParam(required = false) String warehouseCode,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String isActive) {
        return Result.success(
                dataMartService.pageDataMart(
                        new Page<>(page, size), martType, warehouseCode, status, isActive));
    }

    // ==================== 指标管理 ====================

    @Operation(summary = "创建指标定义")
    @PostMapping("/metric")
    public Result<MetricDefine> createMetricDefine(
            @RequestParam String metricName,
            @RequestParam String metricCode,
            @RequestParam String metricType,
            @RequestParam(required = false) String metricCategory,
            @RequestParam(required = false) String martId,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String calculationFormula,
            @RequestParam(required = false) String dataSource,
            @RequestParam(required = false) String unit,
            @RequestParam(required = false) Integer precision,
            @RequestParam(required = false) String aggregationType,
            @RequestParam(required = false) String isDerived,
            @RequestParam(required = false) String parentMetricId,
            @RequestParam(required = false) BigDecimal targetValue,
            @RequestParam(required = false) BigDecimal benchmarkValue,
            @RequestParam(required = false) BigDecimal thresholdWarning,
            @RequestParam(required = false) BigDecimal thresholdCritical,
            @RequestParam(required = false) String direction,
            @RequestParam(required = false) Integer sortOrder,
            @RequestParam String createdBy) {
        return Result.success(
                dataMartService.createMetricDefine(
                        metricName,
                        metricCode,
                        metricType,
                        metricCategory,
                        martId,
                        description,
                        calculationFormula,
                        dataSource,
                        unit,
                        precision,
                        aggregationType,
                        isDerived,
                        parentMetricId,
                        targetValue,
                        benchmarkValue,
                        thresholdWarning,
                        thresholdCritical,
                        direction,
                        sortOrder,
                        createdBy));
    }

    @Operation(summary = "按集市获取活跃指标")
    @GetMapping("/metric/mart/{martId}")
    public Result<List<MetricDefine>> getActiveMetricsByMart(@PathVariable String martId) {
        return Result.success(dataMartService.getActiveMetricsByMart(martId));
    }

    @Operation(summary = "按类型获取活跃指标")
    @GetMapping("/metric/type/{metricType}")
    public Result<List<MetricDefine>> getActiveMetricsByType(@PathVariable String metricType) {
        return Result.success(dataMartService.getActiveMetricsByType(metricType));
    }

    @Operation(summary = "按分类获取活跃指标")
    @GetMapping("/metric/category/{metricCategory}")
    public Result<List<MetricDefine>> getActiveMetricsByCategory(
            @PathVariable String metricCategory) {
        return Result.success(dataMartService.getActiveMetricsByCategory(metricCategory));
    }

    @Operation(summary = "分页查询指标定义")
    @GetMapping("/metric/list")
    public Result<Page<MetricDefine>> pageMetricDefine(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String metricType,
            @RequestParam(required = false) String metricCategory,
            @RequestParam(required = false) String martId,
            @RequestParam(required = false) String isActive) {
        return Result.success(
                dataMartService.pageMetricDefine(
                        new Page<>(page, size), metricType, metricCategory, martId, isActive));
    }

    // ==================== 维度管理 ====================

    @Operation(summary = "创建维度定义")
    @PostMapping("/dimension")
    public Result<DimensionDefine> createDimensionDefine(
            @RequestParam String dimensionName,
            @RequestParam String dimensionCode,
            @RequestParam String dimensionType,
            @RequestParam(required = false) String martId,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String dataType,
            @RequestParam(required = false) String dataSource,
            @RequestParam(required = false) String dimensionConfig,
            @RequestParam(required = false) Integer hierarchyLevel,
            @RequestParam(required = false) String parentDimensionId,
            @RequestParam(required = false) String isTimeDimension,
            @RequestParam(required = false) String isGeoDimension,
            @RequestParam(required = false) Integer sortOrder,
            @RequestParam String createdBy) {
        return Result.success(
                dataMartService.createDimensionDefine(
                        dimensionName,
                        dimensionCode,
                        dimensionType,
                        martId,
                        description,
                        dataType,
                        dataSource,
                        dimensionConfig,
                        hierarchyLevel,
                        parentDimensionId,
                        isTimeDimension,
                        isGeoDimension,
                        sortOrder,
                        createdBy));
    }

    @Operation(summary = "按集市获取活跃维度")
    @GetMapping("/dimension/mart/{martId}")
    public Result<List<DimensionDefine>> getActiveDimensionsByMart(@PathVariable String martId) {
        return Result.success(dataMartService.getActiveDimensionsByMart(martId));
    }

    @Operation(summary = "按类型获取活跃维度")
    @GetMapping("/dimension/type/{dimensionType}")
    public Result<List<DimensionDefine>> getActiveDimensionsByType(
            @PathVariable String dimensionType) {
        return Result.success(dataMartService.getActiveDimensionsByType(dimensionType));
    }

    @Operation(summary = "获取时间维度")
    @GetMapping("/dimension/time")
    public Result<List<DimensionDefine>> getTimeDimensions() {
        return Result.success(dataMartService.getTimeDimensions());
    }

    @Operation(summary = "分页查询维度定义")
    @GetMapping("/dimension/list")
    public Result<Page<DimensionDefine>> pageDimensionDefine(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String dimensionType,
            @RequestParam(required = false) String martId,
            @RequestParam(required = false) String isTimeDimension,
            @RequestParam(required = false) String isActive) {
        return Result.success(
                dataMartService.pageDimensionDefine(
                        new Page<>(page, size), dimensionType, martId, isTimeDimension, isActive));
    }

    // ==================== 数据模型 ====================

    @Operation(summary = "创建数据模型")
    @PostMapping("/model")
    public Result<DataModel> createDataModel(
            @RequestParam String modelName,
            @RequestParam String modelCode,
            @RequestParam String modelType,
            @RequestParam(required = false) String martId,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String modelConfig,
            @RequestParam(required = false) String tableName,
            @RequestParam(required = false) String fields,
            @RequestParam(required = false) String relations,
            @RequestParam(required = false) String partitions,
            @RequestParam(required = false) String indexes,
            @RequestParam(required = false) String storageEngine,
            @RequestParam(required = false) String refreshStrategy,
            @RequestParam(required = false) String refreshCron,
            @RequestParam(required = false) Integer sortOrder,
            @RequestParam String createdBy) {
        return Result.success(
                dataMartService.createDataModel(
                        modelName,
                        modelCode,
                        modelType,
                        martId,
                        description,
                        modelConfig,
                        tableName,
                        fields,
                        relations,
                        partitions,
                        indexes,
                        storageEngine,
                        refreshStrategy,
                        refreshCron,
                        sortOrder,
                        createdBy));
    }

    @Operation(summary = "刷新数据模型")
    @PostMapping("/model/{modelId}/refresh")
    public Result<DataModel> refreshDataModel(
            @PathVariable String modelId,
            @RequestParam Long recordCount,
            @RequestParam BigDecimal dataSizeMb,
            @RequestParam String operator) {
        return Result.success(
                dataMartService.refreshDataModel(modelId, recordCount, dataSizeMb, operator));
    }

    @Operation(summary = "按集市获取活跃模型")
    @GetMapping("/model/mart/{martId}")
    public Result<List<DataModel>> getActiveModelsByMart(@PathVariable String martId) {
        return Result.success(dataMartService.getActiveModelsByMart(martId));
    }

    @Operation(summary = "按类型获取活跃模型")
    @GetMapping("/model/type/{modelType}")
    public Result<List<DataModel>> getActiveModelsByType(@PathVariable String modelType) {
        return Result.success(dataMartService.getActiveModelsByType(modelType));
    }

    @Operation(summary = "分页查询数据模型")
    @GetMapping("/model/list")
    public Result<Page<DataModel>> pageDataModel(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String modelType,
            @RequestParam(required = false) String martId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String isActive) {
        return Result.success(
                dataMartService.pageDataModel(
                        new Page<>(page, size), modelType, martId, status, isActive));
    }
}
