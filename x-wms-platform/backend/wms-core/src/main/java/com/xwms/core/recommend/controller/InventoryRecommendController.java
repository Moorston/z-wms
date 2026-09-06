package com.xwms.core.recommend.controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.common.core.Result;
import com.xwms.core.recommend.entity.*;
import com.xwms.core.recommend.service.InventoryRecommendService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/** 库存智能推荐管理 Controller */
@Tag(name = "库存智能推荐管理", description = "补货推荐/库位推荐/波次推荐/路径推荐")
@RestController
@RequestMapping("/api/recommend")
@RequiredArgsConstructor
public class InventoryRecommendController {

    private final InventoryRecommendService recommendService;

    // ============================================================

    // 补货推荐
    // ============================================================

    @Operation(summary = "创建补货推荐")
    @PostMapping("/replenish")
    public Result<ReplenishRecommend> createReplenishRecommend(
            @RequestParam String warehouseCode,
            @RequestParam(required = false) String ownerCode,
            @RequestParam String skuCode,
            @RequestParam(required = false) String skuName,
            @RequestParam(required = false) String categoryCode,
            @RequestParam(required = false) String fromLocation,
            @RequestParam(required = false) String toLocation,
            @RequestParam(required = false) BigDecimal currentQuantity,
            @RequestParam(required = false) BigDecimal safetyStock,
            @RequestParam(required = false) BigDecimal reorderPoint,
            @RequestParam(required = false) BigDecimal maxStock,
            @RequestParam(required = false) BigDecimal forecastDemand,
            @RequestParam(required = false) String forecastPeriod,
            @RequestParam(required = false) Integer leadTime,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) BigDecimal confidence,
            @RequestParam(required = false) String reason,
            @RequestParam String operator) {
        return Result.success(
                recommendService.createReplenishRecommend(
                        warehouseCode,
                        ownerCode,
                        skuCode,
                        skuName,
                        categoryCode,
                        fromLocation,
                        toLocation,
                        currentQuantity,
                        safetyStock,
                        reorderPoint,
                        maxStock,
                        forecastDemand,
                        forecastPeriod,
                        leadTime,
                        priority,
                        confidence,
                        reason,
                        operator));
    }

    @Operation(summary = "接受补货推荐")
    @PostMapping("/replenish/{recommendId}/accept")
    public Result<ReplenishRecommend> acceptReplenishRecommend(
            @PathVariable String recommendId, @RequestParam String operator) {
        return Result.success(recommendService.acceptReplenishRecommend(recommendId, operator));
    }

    @Operation(summary = "拒绝补货推荐")
    @PostMapping("/replenish/{recommendId}/reject")
    public Result<ReplenishRecommend> rejectReplenishRecommend(
            @PathVariable String recommendId,
            @RequestParam(required = false) String reason,
            @RequestParam String operator) {
        return Result.success(
                recommendService.rejectReplenishRecommend(recommendId, reason, operator));
    }

    @Operation(summary = "按ID查询补货推荐")
    @GetMapping("/replenish/{recommendId}")
    public Result<ReplenishRecommend> getReplenishRecommendById(@PathVariable String recommendId) {
        return Result.success(recommendService.getReplenishRecommendById(recommendId));
    }

    @Operation(summary = "按仓库和SKU查询待处理补货推荐")
    @GetMapping("/replenish/pending")
    public Result<List<ReplenishRecommend>> getPendingReplenishByWarehouseAndSku(
            @RequestParam String warehouseCode, @RequestParam String skuCode) {
        return Result.success(
                recommendService.getPendingReplenishByWarehouseAndSku(warehouseCode, skuCode));
    }

    @Operation(summary = "按仓库和状态查询补货推荐")
    @GetMapping("/replenish/warehouse-status")
    public Result<List<ReplenishRecommend>> getReplenishByWarehouseAndStatus(
            @RequestParam String warehouseCode, @RequestParam String status) {
        return Result.success(
                recommendService.getReplenishByWarehouseAndStatus(warehouseCode, status));
    }

    @Operation(summary = "按仓库和优先级查询补货推荐")
    @GetMapping("/replenish/warehouse-priority")
    public Result<List<ReplenishRecommend>> getReplenishByWarehouseAndPriority(
            @RequestParam String warehouseCode, @RequestParam String priority) {
        return Result.success(
                recommendService.getReplenishByWarehouseAndPriority(warehouseCode, priority));
    }

    @Operation(summary = "分页查询补货推荐")
    @GetMapping("/replenish/list")
    public Result<Page<ReplenishRecommend>> pageReplenish(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String warehouseCode,
            @RequestParam(required = false) String skuCode,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority) {
        return Result.success(
                recommendService.pageReplenish(
                        new Page<>(page, size), warehouseCode, skuCode, status, priority));
    }

    // ============================================================

    // 库位推荐
    // ============================================================

    @Operation(summary = "创建库位推荐")
    @PostMapping("/location")
    public Result<LocationRecommend> createLocationRecommend(
            @RequestParam String warehouseCode,
            @RequestParam(required = false) String ownerCode,
            @RequestParam String skuCode,
            @RequestParam(required = false) String skuName,
            @RequestParam(required = false) String batchNo,
            @RequestParam(required = false) BigDecimal quantity,
            @RequestParam String bizType,
            @RequestParam(required = false) String bizNo,
            @RequestParam(required = false) String recommendLocations,
            @RequestParam(required = false) String bestLocation,
            @RequestParam(required = false) BigDecimal bestScore,
            @RequestParam(required = false) String recommendReason,
            @RequestParam(required = false) String factors,
            @RequestParam String operator) {
        return Result.success(
                recommendService.createLocationRecommend(
                        warehouseCode,
                        ownerCode,
                        skuCode,
                        skuName,
                        batchNo,
                        quantity,
                        bizType,
                        bizNo,
                        recommendLocations,
                        bestLocation,
                        bestScore,
                        recommendReason,
                        factors,
                        operator));
    }

    @Operation(summary = "接受库位推荐")
    @PostMapping("/location/{recommendId}/accept")
    public Result<LocationRecommend> acceptLocationRecommend(
            @PathVariable String recommendId,
            @RequestParam(required = false) String selectedLocation,
            @RequestParam String operator) {
        return Result.success(
                recommendService.acceptLocationRecommend(recommendId, selectedLocation, operator));
    }

    @Operation(summary = "拒绝库位推荐")
    @PostMapping("/location/{recommendId}/reject")
    public Result<LocationRecommend> rejectLocationRecommend(
            @PathVariable String recommendId,
            @RequestParam(required = false) String reason,
            @RequestParam String operator) {
        return Result.success(
                recommendService.rejectLocationRecommend(recommendId, reason, operator));
    }

    @Operation(summary = "按ID查询库位推荐")
    @GetMapping("/location/{recommendId}")
    public Result<LocationRecommend> getLocationRecommendById(@PathVariable String recommendId) {
        return Result.success(recommendService.getLocationRecommendById(recommendId));
    }

    @Operation(summary = "按仓库和SKU查询待处理库位推荐")
    @GetMapping("/location/pending")
    public Result<List<LocationRecommend>> getPendingLocationByWarehouseAndSku(
            @RequestParam String warehouseCode, @RequestParam String skuCode) {
        return Result.success(
                recommendService.getPendingLocationByWarehouseAndSku(warehouseCode, skuCode));
    }

    @Operation(summary = "按业务查询库位推荐")
    @GetMapping("/location/biz")
    public Result<List<LocationRecommend>> getLocationByBiz(
            @RequestParam String bizType, @RequestParam String bizNo) {
        return Result.success(recommendService.getLocationByBiz(bizType, bizNo));
    }

    @Operation(summary = "按仓库和状态查询库位推荐")
    @GetMapping("/location/warehouse-status")
    public Result<List<LocationRecommend>> getLocationByWarehouseAndStatus(
            @RequestParam String warehouseCode, @RequestParam String status) {
        return Result.success(
                recommendService.getLocationByWarehouseAndStatus(warehouseCode, status));
    }

    @Operation(summary = "分页查询库位推荐")
    @GetMapping("/location/list")
    public Result<Page<LocationRecommend>> pageLocation(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String warehouseCode,
            @RequestParam(required = false) String skuCode,
            @RequestParam(required = false) String bizType,
            @RequestParam(required = false) String status) {
        return Result.success(
                recommendService.pageLocation(
                        new Page<>(page, size), warehouseCode, skuCode, bizType, status));
    }

    // ============================================================

    // 波次推荐
    // ============================================================

    @Operation(summary = "创建波次推荐")
    @PostMapping("/wave")
    public Result<WaveRecommend> createWaveRecommend(
            @RequestParam String warehouseCode,
            @RequestParam(required = false) String ownerCode,
            @RequestParam String waveType,
            @RequestParam(required = false) String recommendStrategy,
            @RequestParam(required = false) Integer orderCount,
            @RequestParam(required = false) Integer skuCount,
            @RequestParam(required = false) BigDecimal totalQuantity,
            @RequestParam(required = false) String recommendOrders,
            @RequestParam(required = false) String recommendWaveName,
            @RequestParam(required = false) Long estimatePickTime,
            @RequestParam(required = false) Integer estimatePickers,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) BigDecimal confidence,
            @RequestParam(required = false) String reason,
            @RequestParam String operator) {
        return Result.success(
                recommendService.createWaveRecommend(
                        warehouseCode,
                        ownerCode,
                        waveType,
                        recommendStrategy,
                        orderCount,
                        skuCount,
                        totalQuantity,
                        recommendOrders,
                        recommendWaveName,
                        estimatePickTime,
                        estimatePickers,
                        priority,
                        confidence,
                        reason,
                        operator));
    }

    @Operation(summary = "接受波次推荐")
    @PostMapping("/wave/{recommendId}/accept")
    public Result<WaveRecommend> acceptWaveRecommend(
            @PathVariable String recommendId,
            @RequestParam(required = false) String relatedWaveId,
            @RequestParam String operator) {
        return Result.success(
                recommendService.acceptWaveRecommend(recommendId, relatedWaveId, operator));
    }

    @Operation(summary = "拒绝波次推荐")
    @PostMapping("/wave/{recommendId}/reject")
    public Result<WaveRecommend> rejectWaveRecommend(
            @PathVariable String recommendId,
            @RequestParam(required = false) String reason,
            @RequestParam String operator) {
        return Result.success(recommendService.rejectWaveRecommend(recommendId, reason, operator));
    }

    @Operation(summary = "按ID查询波次推荐")
    @GetMapping("/wave/{recommendId}")
    public Result<WaveRecommend> getWaveRecommendById(@PathVariable String recommendId) {
        return Result.success(recommendService.getWaveRecommendById(recommendId));
    }

    @Operation(summary = "按仓库和类型查询待处理波次推荐")
    @GetMapping("/wave/pending")
    public Result<List<WaveRecommend>> getPendingWaveByWarehouseAndType(
            @RequestParam String warehouseCode, @RequestParam String waveType) {
        return Result.success(
                recommendService.getPendingWaveByWarehouseAndType(warehouseCode, waveType));
    }

    @Operation(summary = "按仓库和状态查询波次推荐")
    @GetMapping("/wave/warehouse-status")
    public Result<List<WaveRecommend>> getWaveByWarehouseAndStatus(
            @RequestParam String warehouseCode, @RequestParam String status) {
        return Result.success(recommendService.getWaveByWarehouseAndStatus(warehouseCode, status));
    }

    @Operation(summary = "按仓库和优先级查询波次推荐")
    @GetMapping("/wave/warehouse-priority")
    public Result<List<WaveRecommend>> getWaveByWarehouseAndPriority(
            @RequestParam String warehouseCode, @RequestParam String priority) {
        return Result.success(
                recommendService.getWaveByWarehouseAndPriority(warehouseCode, priority));
    }

    @Operation(summary = "分页查询波次推荐")
    @GetMapping("/wave/list")
    public Result<Page<WaveRecommend>> pageWave(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String warehouseCode,
            @RequestParam(required = false) String waveType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority) {
        return Result.success(
                recommendService.pageWave(
                        new Page<>(page, size), warehouseCode, waveType, status, priority));
    }

    // ============================================================

    // 路径推荐
    // ============================================================

    @Operation(summary = "创建路径推荐")
    @PostMapping("/path")
    public Result<PathRecommend> createPathRecommend(
            @RequestParam String warehouseCode,
            @RequestParam(required = false) String ownerCode,
            @RequestParam(required = false) String waveId,
            @RequestParam(required = false) String pickerId,
            @RequestParam(required = false) String pickerName,
            @RequestParam String pickMode,
            @RequestParam(required = false) Integer locationCount,
            @RequestParam(required = false) Integer skuCount,
            @RequestParam(required = false) BigDecimal totalQuantity,
            @RequestParam(required = false) String recommendPath,
            @RequestParam(required = false) String startLocation,
            @RequestParam(required = false) String endLocation,
            @RequestParam(required = false) BigDecimal estimateDistance,
            @RequestParam(required = false) Long estimateTime,
            @RequestParam(required = false) String pathAlgorithm,
            @RequestParam(required = false) String congestionAvoidance,
            @RequestParam(required = false) String revisitAvoidance,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) BigDecimal confidence,
            @RequestParam(required = false) String reason,
            @RequestParam String operator) {
        return Result.success(
                recommendService.createPathRecommend(
                        warehouseCode,
                        ownerCode,
                        waveId,
                        pickerId,
                        pickerName,
                        pickMode,
                        locationCount,
                        skuCount,
                        totalQuantity,
                        recommendPath,
                        startLocation,
                        endLocation,
                        estimateDistance,
                        estimateTime,
                        pathAlgorithm,
                        congestionAvoidance,
                        revisitAvoidance,
                        priority,
                        confidence,
                        reason,
                        operator));
    }

    @Operation(summary = "接受路径推荐")
    @PostMapping("/path/{recommendId}/accept")
    public Result<PathRecommend> acceptPathRecommend(
            @PathVariable String recommendId, @RequestParam String operator) {
        return Result.success(recommendService.acceptPathRecommend(recommendId, operator));
    }

    @Operation(summary = "拒绝路径推荐")
    @PostMapping("/path/{recommendId}/reject")
    public Result<PathRecommend> rejectPathRecommend(
            @PathVariable String recommendId,
            @RequestParam(required = false) String reason,
            @RequestParam String operator) {
        return Result.success(recommendService.rejectPathRecommend(recommendId, reason, operator));
    }

    @Operation(summary = "完成路径推荐（记录实际路径）")
    @PostMapping("/path/{recommendId}/complete")
    public Result<PathRecommend> completePathRecommend(
            @PathVariable String recommendId,
            @RequestParam(required = false) String actualPath,
            @RequestParam(required = false) BigDecimal actualDistance,
            @RequestParam(required = false) Long actualTime,
            @RequestParam String operator) {
        return Result.success(
                recommendService.completePathRecommend(
                        recommendId, actualPath, actualDistance, actualTime, operator));
    }

    @Operation(summary = "按ID查询路径推荐")
    @GetMapping("/path/{recommendId}")
    public Result<PathRecommend> getPathRecommendById(@PathVariable String recommendId) {
        return Result.success(recommendService.getPathRecommendById(recommendId));
    }

    @Operation(summary = "按波次查询路径推荐")
    @GetMapping("/path/wave/{waveId}")
    public Result<List<PathRecommend>> getPathByWaveId(@PathVariable String waveId) {
        return Result.success(recommendService.getPathByWaveId(waveId));
    }

    @Operation(summary = "按拣货员查询待处理路径推荐")
    @GetMapping("/path/pending")
    public Result<List<PathRecommend>> getPendingPathByPicker(@RequestParam String pickerId) {
        return Result.success(recommendService.getPendingPathByPicker(pickerId));
    }

    @Operation(summary = "按仓库和状态查询路径推荐")
    @GetMapping("/path/warehouse-status")
    public Result<List<PathRecommend>> getPathByWarehouseAndStatus(
            @RequestParam String warehouseCode, @RequestParam String status) {
        return Result.success(recommendService.getPathByWarehouseAndStatus(warehouseCode, status));
    }

    @Operation(summary = "分页查询路径推荐")
    @GetMapping("/path/list")
    public Result<Page<PathRecommend>> pagePath(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String warehouseCode,
            @RequestParam(required = false) String waveId,
            @RequestParam(required = false) String pickerId,
            @RequestParam(required = false) String status) {
        return Result.success(
                recommendService.pagePath(
                        new Page<>(page, size), warehouseCode, waveId, pickerId, status));
    }
}
