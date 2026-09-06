package com.xwms.base.location.controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.base.location.entity.*;
import com.xwms.base.location.service.LocationManagementService;
import com.xwms.common.core.Result;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/** 库位管理 Controller 整合仓库/库区/库位组/库位四级管理 */
@Tag(name = "库位管理", description = "仓库/库区/库位组/库位")
@RestController
@RequestMapping("/api/location")
@RequiredArgsConstructor
public class LocationManagementController {

    private final LocationManagementService locationManagementService;

    // ============================================================
    // 仓库管理
    // ============================================================

    @Operation(summary = "创建仓库")
    @PostMapping("/warehouse")
    public Result<Warehouse> createWarehouse(@RequestBody Warehouse warehouse) {
        return Result.success(locationManagementService.createWarehouse(warehouse));
    }

    @Operation(summary = "更新仓库")
    @PutMapping("/warehouse")
    public Result<Warehouse> updateWarehouse(@RequestBody Warehouse warehouse) {
        return Result.success(locationManagementService.updateWarehouse(warehouse));
    }

    @Operation(summary = "分页查询仓库")
    @GetMapping("/warehouse")
    public Result<Page<Warehouse>> pageWarehouses(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String warehouseType,
            @RequestParam(required = false) String status) {
        return Result.success(
                locationManagementService.pageWarehouses(
                        new Page<>(page, size), warehouseType, status));
    }

    @Operation(summary = "查询活跃仓库")
    @GetMapping("/warehouse/active")
    public Result<List<Warehouse>> getActiveWarehouses() {
        return Result.success(locationManagementService.getActiveWarehouses());
    }

    @Operation(summary = "按编码查询仓库")
    @GetMapping("/warehouse/{code}")
    public Result<Warehouse> getWarehouseByCode(@PathVariable String code) {
        return Result.success(locationManagementService.getWarehouseByCode(code));
    }

    // ============================================================
    // 库区管理
    // ============================================================

    @Operation(summary = "创建库区")
    @PostMapping("/area")
    public Result<Area> createArea(@RequestBody Area area) {
        return Result.success(locationManagementService.createArea(area));
    }

    @Operation(summary = "更新库区")
    @PutMapping("/area")
    public Result<Area> updateArea(@RequestBody Area area) {
        return Result.success(locationManagementService.updateArea(area));
    }

    @Operation(summary = "分页查询库区")
    @GetMapping("/area")
    public Result<Page<Area>> pageAreas(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String warehouseCode,
            @RequestParam(required = false) String areaType,
            @RequestParam(required = false) String temperatureZone,
            @RequestParam(required = false) String status) {
        return Result.success(
                locationManagementService.pageAreas(
                        new Page<>(page, size), warehouseCode, areaType, temperatureZone, status));
    }

    @Operation(summary = "按仓库查询库区")
    @GetMapping("/area/warehouse/{warehouseCode}")
    public Result<List<Area>> getAreasByWarehouse(@PathVariable String warehouseCode) {
        return Result.success(locationManagementService.getAreasByWarehouse(warehouseCode));
    }

    @Operation(summary = "按编码查询库区")
    @GetMapping("/area/{code}")
    public Result<Area> getAreaByCode(@PathVariable String code) {
        return Result.success(locationManagementService.getAreaByCode(code));
    }

    // ============================================================
    // 库位组管理
    // ============================================================

    @Operation(summary = "创建库位组")
    @PostMapping("/group")
    public Result<LocationGroup> createLocationGroup(@RequestBody LocationGroup group) {
        return Result.success(locationManagementService.createLocationGroup(group));
    }

    @Operation(summary = "更新库位组")
    @PutMapping("/group")
    public Result<LocationGroup> updateLocationGroup(@RequestBody LocationGroup group) {
        return Result.success(locationManagementService.updateLocationGroup(group));
    }

    @Operation(summary = "分页查询库位组")
    @GetMapping("/group")
    public Result<Page<LocationGroup>> pageLocationGroups(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String warehouseCode,
            @RequestParam(required = false) String areaCode,
            @RequestParam(required = false) String groupType,
            @RequestParam(required = false) String status) {
        return Result.success(
                locationManagementService.pageLocationGroups(
                        new Page<>(page, size), warehouseCode, areaCode, groupType, status));
    }

    @Operation(summary = "按仓库查询库位组")
    @GetMapping("/group/warehouse/{warehouseCode}")
    public Result<List<LocationGroup>> getLocationGroupsByWarehouse(
            @PathVariable String warehouseCode) {
        return Result.success(
                locationManagementService.getLocationGroupsByWarehouse(warehouseCode));
    }

    @Operation(summary = "按编码查询库位组")
    @GetMapping("/group/{code}")
    public Result<LocationGroup> getLocationGroupByCode(@PathVariable String code) {
        return Result.success(locationManagementService.getLocationGroupByCode(code));
    }

    // ============================================================
    // 库位管理
    // ============================================================

    @Operation(summary = "创建库位")
    @PostMapping
    public Result<Location> createLocation(@RequestBody Location location) {
        return Result.success(locationManagementService.createLocation(location));
    }

    @Operation(summary = "更新库位")
    @PutMapping
    public Result<Location> updateLocation(@RequestBody Location location) {
        return Result.success(locationManagementService.updateLocation(location));
    }

    @Operation(summary = "分页查询库位")
    @GetMapping
    public Result<Page<Location>> pageLocations(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String warehouseCode,
            @RequestParam(required = false) String areaCode,
            @RequestParam(required = false) String locationGroup,
            @RequestParam(required = false) String locationType,
            @RequestParam(required = false) String temperatureZone,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String isVirtual) {
        return Result.success(
                locationManagementService.pageLocations(
                        new Page<>(page, size),
                        warehouseCode,
                        areaCode,
                        locationGroup,
                        locationType,
                        temperatureZone,
                        status,
                        isVirtual));
    }

    @Operation(summary = "按编码查询库位")
    @GetMapping("/{code}")
    public Result<Location> getLocationByCode(@PathVariable String code) {
        return Result.success(locationManagementService.getLocationByCode(code));
    }

    @Operation(summary = "查询空库位（上架推荐）")
    @GetMapping("/empty")
    public Result<List<Location>> findEmptyLocations(
            @RequestParam String warehouseCode,
            @RequestParam(required = false) String areaCode,
            @RequestParam(required = false) String temperatureZone,
            @RequestParam(required = false) String locationType) {
        return Result.success(
                locationManagementService.findEmptyLocations(
                        warehouseCode, areaCode, temperatureZone, locationType));
    }

    @Operation(summary = "更新库位状态")
    @PutMapping("/{code}/status")
    public Result<Location> updateLocationStatus(
            @PathVariable String code, @RequestParam String status) {
        return Result.success(locationManagementService.updateLocationStatus(code, status));
    }

    @Operation(summary = "冻结/解冻库位")
    @PutMapping("/{code}/freeze")
    public Result<Location> freezeLocation(
            @PathVariable String code, @RequestParam(defaultValue = "true") boolean freeze) {
        return Result.success(locationManagementService.freezeLocation(code, freeze));
    }

    @Operation(summary = "更新库位容量使用")
    @PutMapping("/{code}/capacity")
    public Result<Location> updateLocationCapacity(
            @PathVariable String code, @RequestParam BigDecimal usedCapacity) {
        return Result.success(locationManagementService.updateLocationCapacity(code, usedCapacity));
    }

    @Operation(summary = "按路径排序查询库位")
    @PostMapping("/path")
    public Result<List<Location>> findLocationsByPath(
            @RequestParam String warehouseCode, @RequestBody List<String> locationCodes) {
        return Result.success(
                locationManagementService.findLocationsByPath(warehouseCode, locationCodes));
    }

    @Operation(summary = "批量生成库位")
    @PostMapping("/batch-generate")
    public Result<Integer> batchGenerateLocations(
            @RequestParam String warehouseCode,
            @RequestParam String areaCode,
            @RequestParam(required = false) String locationGroup,
            @RequestParam String locationType,
            @RequestParam(required = false) String temperatureZone,
            @RequestParam(defaultValue = "R") String rowPrefix,
            @RequestParam int rowStart,
            @RequestParam int rowEnd,
            @RequestParam(defaultValue = "C") String columnPrefix,
            @RequestParam int columnStart,
            @RequestParam int columnEnd,
            @RequestParam(defaultValue = "L") String levelPrefix,
            @RequestParam int levelStart,
            @RequestParam int levelEnd,
            @RequestParam(required = false) BigDecimal capacity,
            @RequestParam(required = false) BigDecimal maxWeight) {
        return Result.success(
                locationManagementService.batchGenerateLocations(
                        warehouseCode,
                        areaCode,
                        locationGroup,
                        locationType,
                        temperatureZone,
                        rowPrefix,
                        rowStart,
                        rowEnd,
                        columnPrefix,
                        columnStart,
                        columnEnd,
                        levelPrefix,
                        levelStart,
                        levelEnd,
                        capacity,
                        maxWeight));
    }
}
