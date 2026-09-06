package com.xwms.core.forecast.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.common.core.Result;
import com.xwms.core.forecast.entity.*;
import com.xwms.core.forecast.service.InventoryForecastService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/** 库存分析预测管理 Controller */
@Tag(name = "库存分析预测管理", description = "库存分析/需求预测/库存优化/预测模型")
@RestController
@RequestMapping("/api/forecast")
@RequiredArgsConstructor
public class InventoryForecastController {

    private final InventoryForecastService forecastService;

    // ============================================================

    // 库存分析
    // ============================================================

    @Operation(summary = "创建库存分析")
    @PostMapping("/analysis")
    public Result<InventoryAnalysis> createAnalysis(
            @RequestParam(required = false) String analysisName,
            @RequestParam String analysisType,
            @RequestParam String warehouseCode,
            @RequestParam(required = false) String ownerCode,
            @RequestParam LocalDate periodStart,
            @RequestParam LocalDate periodEnd,
            @RequestParam(required = false) String skuCode,
            @RequestParam(required = false) String categoryCode,
            @RequestParam String operator) {
        return Result.success(
                forecastService.createAnalysis(
                        analysisName,
                        analysisType,
                        warehouseCode,
                        ownerCode,
                        periodStart,
                        periodEnd,
                        skuCode,
                        categoryCode,
                        operator));
    }

    @Operation(summary = "执行库存分析")
    @PostMapping("/analysis/{analysisId}/execute")
    public Result<InventoryAnalysis> executeAnalysis(@PathVariable String analysisId) {
        return Result.success(forecastService.executeAnalysis(analysisId));
    }

    @Operation(summary = "按ID查询库存分析")
    @GetMapping("/analysis/{analysisId}")
    public Result<InventoryAnalysis> getAnalysisById(@PathVariable String analysisId) {
        return Result.success(forecastService.getAnalysisById(analysisId));
    }

    @Operation(summary = "按仓库和类型和期间查询库存分析")
    @GetMapping("/analysis/warehouse-type-period")
    public Result<InventoryAnalysis> getAnalysisByWarehouseAndTypeAndPeriod(
            @RequestParam String warehouseCode,
            @RequestParam String analysisType,
            @RequestParam LocalDate periodStart,
            @RequestParam LocalDate periodEnd) {
        return Result.success(
                forecastService.getAnalysisByWarehouseAndTypeAndPeriod(
                        warehouseCode, analysisType, periodStart, periodEnd));
    }

    @Operation(summary = "按SKU和类型查询库存分析")
    @GetMapping("/analysis/sku-type")
    public Result<List<InventoryAnalysis>> getAnalysisBySkuAndType(
            @RequestParam String skuCode,
            @RequestParam String analysisType,
            @RequestParam(defaultValue = "10") int limit) {
        return Result.success(
                forecastService.getAnalysisBySkuAndType(skuCode, analysisType, limit));
    }

    @Operation(summary = "查询最近库存分析")
    @GetMapping("/analysis/recent")
    public Result<List<InventoryAnalysis>> getRecentAnalysisByWarehouseAndType(
            @RequestParam String warehouseCode,
            @RequestParam String analysisType,
            @RequestParam(defaultValue = "10") int limit) {
        return Result.success(
                forecastService.getRecentAnalysisByWarehouseAndType(
                        warehouseCode, analysisType, limit));
    }

    @Operation(summary = "分页查询库存分析")
    @GetMapping("/analysis/list")
    public Result<Page<InventoryAnalysis>> pageAnalysis(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String warehouseCode,
            @RequestParam(required = false) String analysisType,
            @RequestParam(required = false) String skuCode,
            @RequestParam(required = false) String status) {
        return Result.success(
                forecastService.pageAnalysis(
                        new Page<>(page, size), warehouseCode, analysisType, skuCode, status));
    }

    // ============================================================

    // 需求预测
    // ============================================================

    @Operation(summary = "创建需求预测")
    @PostMapping("/demand")
    public Result<DemandForecast> createForecast(
            @RequestParam(required = false) String forecastName,
            @RequestParam String modelCode,
            @RequestParam String warehouseCode,
            @RequestParam(required = false) String ownerCode,
            @RequestParam String skuCode,
            @RequestParam(required = false) String categoryCode,
            @RequestParam String forecastPeriod,
            @RequestParam Integer forecastHorizon,
            @RequestParam(required = false) LocalDate historyStart,
            @RequestParam(required = false) LocalDate historyEnd,
            @RequestParam LocalDate forecastStart,
            @RequestParam LocalDate forecastEnd,
            @RequestParam String operator) {
        return Result.success(
                forecastService.createForecast(
                        forecastName,
                        modelCode,
                        warehouseCode,
                        ownerCode,
                        skuCode,
                        categoryCode,
                        forecastPeriod,
                        forecastHorizon,
                        historyStart,
                        historyEnd,
                        forecastStart,
                        forecastEnd,
                        operator));
    }

    @Operation(summary = "执行需求预测")
    @PostMapping("/demand/{forecastId}/execute")
    public Result<DemandForecast> executeForecast(@PathVariable String forecastId) {
        return Result.success(forecastService.executeForecast(forecastId));
    }

    @Operation(summary = "按ID查询需求预测")
    @GetMapping("/demand/{forecastId}")
    public Result<DemandForecast> getForecastById(@PathVariable String forecastId) {
        return Result.success(forecastService.getForecastById(forecastId));
    }

    @Operation(summary = "按SKU和周期查询需求预测")
    @GetMapping("/demand/sku-period")
    public Result<List<DemandForecast>> getForecastBySkuAndPeriod(
            @RequestParam String skuCode,
            @RequestParam String forecastPeriod,
            @RequestParam(defaultValue = "10") int limit) {
        return Result.success(
                forecastService.getForecastBySkuAndPeriod(skuCode, forecastPeriod, limit));
    }

    @Operation(summary = "按仓库和模型和期间查询需求预测")
    @GetMapping("/demand/warehouse-model-period")
    public Result<List<DemandForecast>> getForecastByWarehouseAndModelAndPeriod(
            @RequestParam String warehouseCode,
            @RequestParam String modelCode,
            @RequestParam LocalDate forecastStart,
            @RequestParam LocalDate forecastEnd) {
        return Result.success(
                forecastService.getForecastByWarehouseAndModelAndPeriod(
                        warehouseCode, modelCode, forecastStart, forecastEnd));
    }

    @Operation(summary = "查询最近需求预测")
    @GetMapping("/demand/recent")
    public Result<List<DemandForecast>> getRecentForecastByWarehouseAndPeriod(
            @RequestParam String warehouseCode,
            @RequestParam String forecastPeriod,
            @RequestParam(defaultValue = "10") int limit) {
        return Result.success(
                forecastService.getRecentForecastByWarehouseAndPeriod(
                        warehouseCode, forecastPeriod, limit));
    }

    @Operation(summary = "分页查询需求预测")
    @GetMapping("/demand/list")
    public Result<Page<DemandForecast>> pageForecast(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String warehouseCode,
            @RequestParam(required = false) String modelCode,
            @RequestParam(required = false) String skuCode,
            @RequestParam(required = false) String forecastPeriod,
            @RequestParam(required = false) String status) {
        return Result.success(
                forecastService.pageForecast(
                        new Page<>(page, size),
                        warehouseCode,
                        modelCode,
                        skuCode,
                        forecastPeriod,
                        status));
    }

    // ============================================================

    // 库存优化
    // ============================================================

    @Operation(summary = "创建库存优化")
    @PostMapping("/optimization")
    public Result<InventoryOptimization> createOptimization(
            @RequestParam(required = false) String optimizationName,
            @RequestParam String optimizationType,
            @RequestParam String warehouseCode,
            @RequestParam(required = false) String ownerCode,
            @RequestParam(required = false) String skuCode,
            @RequestParam(required = false) String categoryCode,
            @RequestParam(required = false) BigDecimal currentValue,
            @RequestParam String operator) {
        return Result.success(
                forecastService.createOptimization(
                        optimizationName,
                        optimizationType,
                        warehouseCode,
                        ownerCode,
                        skuCode,
                        categoryCode,
                        currentValue,
                        operator));
    }

    @Operation(summary = "执行库存优化")
    @PostMapping("/optimization/{optimizationId}/execute")
    public Result<InventoryOptimization> executeOptimization(@PathVariable String optimizationId) {
        return Result.success(forecastService.executeOptimization(optimizationId));
    }

    @Operation(summary = "实施库存优化")
    @PostMapping("/optimization/{optimizationId}/implement")
    public Result<InventoryOptimization> implementOptimization(
            @PathVariable String optimizationId, @RequestParam String implementer) {
        return Result.success(forecastService.implementOptimization(optimizationId, implementer));
    }

    @Operation(summary = "按ID查询库存优化")
    @GetMapping("/optimization/{optimizationId}")
    public Result<InventoryOptimization> getOptimizationById(@PathVariable String optimizationId) {
        return Result.success(forecastService.getOptimizationById(optimizationId));
    }

    @Operation(summary = "按仓库和类型查询库存优化")
    @GetMapping("/optimization/warehouse-type")
    public Result<List<InventoryOptimization>> getOptimizationByWarehouseAndType(
            @RequestParam String warehouseCode,
            @RequestParam String optimizationType,
            @RequestParam(defaultValue = "10") int limit) {
        return Result.success(
                forecastService.getOptimizationByWarehouseAndType(
                        warehouseCode, optimizationType, limit));
    }

    @Operation(summary = "按SKU和类型查询库存优化")
    @GetMapping("/optimization/sku-type")
    public Result<List<InventoryOptimization>> getOptimizationBySkuAndType(
            @RequestParam String skuCode,
            @RequestParam String optimizationType,
            @RequestParam(defaultValue = "10") int limit) {
        return Result.success(
                forecastService.getOptimizationBySkuAndType(skuCode, optimizationType, limit));
    }

    @Operation(summary = "按状态查询库存优化")
    @GetMapping("/optimization/status/{status}")
    public Result<List<InventoryOptimization>> getOptimizationByStatus(
            @PathVariable String status) {
        return Result.success(forecastService.getOptimizationByStatus(status));
    }

    @Operation(summary = "分页查询库存优化")
    @GetMapping("/optimization/list")
    public Result<Page<InventoryOptimization>> pageOptimization(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String warehouseCode,
            @RequestParam(required = false) String optimizationType,
            @RequestParam(required = false) String skuCode,
            @RequestParam(required = false) String status) {
        return Result.success(
                forecastService.pageOptimization(
                        new Page<>(page, size), warehouseCode, optimizationType, skuCode, status));
    }

    // ============================================================

    // 预测模型
    // ============================================================

    @Operation(summary = "创建预测模型")
    @PostMapping("/model")
    public Result<ForecastModel> createModel(
            @RequestParam String modelCode,
            @RequestParam String modelName,
            @RequestParam String modelType,
            @RequestParam(required = false) String algorithm,
            @RequestParam(required = false) String version,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String framework,
            @RequestParam String createdBy) {
        return Result.success(
                forecastService.createModel(
                        modelCode,
                        modelName,
                        modelType,
                        algorithm,
                        version,
                        description,
                        framework,
                        createdBy));
    }

    @Operation(summary = "训练预测模型")
    @PostMapping("/model/{modelCode}/{version}/train")
    public Result<ForecastModel> trainModel(
            @PathVariable String modelCode,
            @PathVariable String version,
            @RequestParam(required = false) LocalDate trainingDataStart,
            @RequestParam(required = false) LocalDate trainingDataEnd,
            @RequestParam(required = false) Integer trainingSamples,
            @RequestParam(required = false) BigDecimal accuracy,
            @RequestParam(required = false) BigDecimal mae,
            @RequestParam(required = false) BigDecimal rmse,
            @RequestParam(required = false) BigDecimal mape,
            @RequestParam(required = false) Long trainingDuration,
            @RequestParam(required = false) String modelPath,
            @RequestParam(required = false) Long modelSize) {
        return Result.success(
                forecastService.trainModel(
                        modelCode,
                        version,
                        trainingDataStart,
                        trainingDataEnd,
                        trainingSamples,
                        accuracy,
                        mae,
                        rmse,
                        mape,
                        trainingDuration,
                        modelPath,
                        modelSize));
    }

    @Operation(summary = "设置默认预测模型")
    @PostMapping("/model/{modelCode}/{version}/default")
    public Result<ForecastModel> setDefaultModel(
            @PathVariable String modelCode,
            @PathVariable String version,
            @RequestParam String modelType) {
        return Result.success(forecastService.setDefaultModel(modelCode, version, modelType));
    }

    @Operation(summary = "按编码和版本查询预测模型")
    @GetMapping("/model/{modelCode}/{version}")
    public Result<ForecastModel> getModelByCodeAndVersion(
            @PathVariable String modelCode, @PathVariable String version) {
        return Result.success(forecastService.getModelByCodeAndVersion(modelCode, version));
    }

    @Operation(summary = "按类型查询活跃预测模型")
    @GetMapping("/model/active/{modelType}")
    public Result<List<ForecastModel>> getActiveModelsByType(@PathVariable String modelType) {
        return Result.success(forecastService.getActiveModelsByType(modelType));
    }

    @Operation(summary = "按类型查询默认预测模型")
    @GetMapping("/model/default/{modelType}")
    public Result<ForecastModel> getDefaultModelByType(@PathVariable String modelType) {
        return Result.success(forecastService.getDefaultModelByType(modelType));
    }

    @Operation(summary = "按状态查询预测模型")
    @GetMapping("/model/status/{status}")
    public Result<List<ForecastModel>> getModelsByStatus(@PathVariable String status) {
        return Result.success(forecastService.getModelsByStatus(status));
    }

    @Operation(summary = "分页查询预测模型")
    @GetMapping("/model/list")
    public Result<Page<ForecastModel>> pageModels(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String modelType,
            @RequestParam(required = false) String status) {
        return Result.success(
                forecastService.pageModels(new Page<>(page, size), modelType, status));
    }
}
