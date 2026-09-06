package com.xwms.base.location.controller;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.base.location.entity.Location;
import com.xwms.base.location.mapper.LocationMapper;
import com.xwms.base.location.service.LocationService;
import com.xwms.common.core.PageResult;
import com.xwms.common.core.Result;

import lombok.RequiredArgsConstructor;

/** 库位管理Controller 仓库-库区-库位三级结构管理 */
@RestController
@RequestMapping("/api/location")
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;
    private final LocationMapper locationMapper;

    /** 新增库位 */
    @PostMapping
    public Result<Location> create(@RequestBody Location location) {
        locationMapper.insert(location);
        return Result.success(location);
    }

    /** 更新库位 */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Location location) {
        location.setId(id);
        locationMapper.updateById(location);
        return Result.success();
    }

    /** 分页查询库位 */
    @GetMapping("/page")
    public Result<PageResult<Location>> page(
            @RequestParam(required = false) String locationCode,
            @RequestParam(required = false) String warehouse,
            @RequestParam(required = false) String areaCode,
            @RequestParam(required = false) String locationType,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        Page<Location> page =
                locationMapper.selectPage(
                        new Page<>(pageNum, pageSize),
                        new LambdaQueryWrapper<Location>()
                                .like(locationCode != null, Location::getLocationCode, locationCode)
                                .eq(warehouse != null, Location::getWarehouseCode, warehouse)
                                .eq(areaCode != null, Location::getAreaCode, areaCode)
                                .eq(locationType != null, Location::getLocationType, locationType)
                                .eq(status != null, Location::getStatus, status)
                                .orderByAsc(Location::getSortNo));
        return Result.success(
                PageResult.of(
                        page.getRecords(),
                        page.getTotal(),
                        (int) page.getCurrent(),
                        (int) page.getSize()));
    }

    /** 查询空库位（上架推荐） */
    @GetMapping("/empty")
    public Result<List<Location>> findEmpty(
            @RequestParam String warehouse,
            @RequestParam(required = false) String areaCode,
            @RequestParam(required = false) String temperatureZone,
            @RequestParam(required = false) String locationType) {
        return Result.success(
                locationService.findEmptyLocations(
                        warehouse, areaCode, temperatureZone, locationType));
    }

    /** 更新库位状态 */
    @PostMapping("/{locationCode}/status")
    public Result<Void> updateStatus(
            @PathVariable String locationCode, @RequestParam String status) {
        locationService.updateStatus(locationCode, status);
        return Result.success();
    }

    /** 按S型路径排序查询库位（拣货路径规划用） */
    @PostMapping("/route")
    public Result<List<Location>> listByRouteOrder(
            @RequestParam String warehouse, @RequestBody List<String> locationCodes) {
        return Result.success(locationService.listByRouteOrder(warehouse, locationCodes));
    }
}
