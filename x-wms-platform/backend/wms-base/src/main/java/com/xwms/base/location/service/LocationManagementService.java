package com.xwms.base.location.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.base.location.entity.*;
import com.xwms.base.location.enums.LocationStatus;
import com.xwms.base.location.mapper.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 库位管理核心服务 整合仓库/库区/库位组/库位四级管理 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LocationManagementService {

    private final WarehouseMapper warehouseMapper;
    private final AreaMapper areaMapper;
    private final LocationGroupMapper locationGroupMapper;
    private final LocationMapper locationMapper;

    // ============================================================
    // 1. 仓库管理
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public Warehouse createWarehouse(Warehouse warehouse) {
        warehouseMapper.insert(warehouse);
        log.info("创建仓库: {}={}", warehouse.getWarehouseCode(), warehouse.getWarehouseName());
        return warehouse;
    }

    @Transactional(rollbackFor = Exception.class)
    public Warehouse updateWarehouse(Warehouse warehouse) {
        warehouseMapper.updateById(warehouse);
        return warehouse;
    }

    public Page<Warehouse> pageWarehouses(
            Page<Warehouse> page, String warehouseType, String status) {
        LambdaQueryWrapper<Warehouse> wrapper = new LambdaQueryWrapper<>();
        if (warehouseType != null) wrapper.eq(Warehouse::getWarehouseType, warehouseType);
        if (status != null) wrapper.eq(Warehouse::getStatus, status);
        wrapper.orderByAsc(Warehouse::getWarehouseCode);
        return warehouseMapper.selectPage(page, wrapper);
    }

    public List<Warehouse> getActiveWarehouses() {
        return warehouseMapper.selectActiveWarehouses();
    }

    public Warehouse getWarehouseByCode(String warehouseCode) {
        return warehouseMapper.selectByCode(warehouseCode);
    }

    // ============================================================
    // 2. 库区管理
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public Area createArea(Area area) {
        areaMapper.insert(area);
        log.info("创建库区: {}={}", area.getAreaCode(), area.getAreaName());
        return area;
    }

    @Transactional(rollbackFor = Exception.class)
    public Area updateArea(Area area) {
        areaMapper.updateById(area);
        return area;
    }

    public Page<Area> pageAreas(
            Page<Area> page,
            String warehouseCode,
            String areaType,
            String temperatureZone,
            String status) {
        LambdaQueryWrapper<Area> wrapper = new LambdaQueryWrapper<>();
        if (warehouseCode != null) wrapper.eq(Area::getWarehouseCode, warehouseCode);
        if (areaType != null) wrapper.eq(Area::getAreaType, areaType);
        if (temperatureZone != null) wrapper.eq(Area::getTemperatureZone, temperatureZone);
        if (status != null) wrapper.eq(Area::getStatus, status);
        wrapper.orderByAsc(Area::getWarehouseCode).orderByAsc(Area::getSortNo);
        return areaMapper.selectPage(page, wrapper);
    }

    public List<Area> getAreasByWarehouse(String warehouseCode) {
        return areaMapper.selectByWarehouse(warehouseCode);
    }

    public Area getAreaByCode(String areaCode) {
        return areaMapper.selectByCode(areaCode);
    }

    // ============================================================
    // 3. 库位组管理
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public LocationGroup createLocationGroup(LocationGroup group) {
        locationGroupMapper.insert(group);
        log.info("创建库位组: {}={}", group.getGroupCode(), group.getGroupName());
        return group;
    }

    @Transactional(rollbackFor = Exception.class)
    public LocationGroup updateLocationGroup(LocationGroup group) {
        locationGroupMapper.updateById(group);
        return group;
    }

    public Page<LocationGroup> pageLocationGroups(
            Page<LocationGroup> page,
            String warehouseCode,
            String areaCode,
            String groupType,
            String status) {
        LambdaQueryWrapper<LocationGroup> wrapper = new LambdaQueryWrapper<>();
        if (warehouseCode != null) wrapper.eq(LocationGroup::getWarehouseCode, warehouseCode);
        if (areaCode != null) wrapper.eq(LocationGroup::getAreaCode, areaCode);
        if (groupType != null) wrapper.eq(LocationGroup::getGroupType, groupType);
        if (status != null) wrapper.eq(LocationGroup::getStatus, status);
        wrapper.orderByAsc(LocationGroup::getWarehouseCode).orderByAsc(LocationGroup::getSortNo);
        return locationGroupMapper.selectPage(page, wrapper);
    }

    public List<LocationGroup> getLocationGroupsByWarehouse(String warehouseCode) {
        return locationGroupMapper.selectByWarehouse(warehouseCode);
    }

    public LocationGroup getLocationGroupByCode(String groupCode) {
        return locationGroupMapper.selectByCode(groupCode);
    }

    // ============================================================
    // 4. 库位管理
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public Location createLocation(Location location) {
        if (location.getStatus() == null) {
            location.setStatus(LocationStatus.EMPTY.getCode());
        }
        locationMapper.insert(location);
        log.info("创建库位: {}", location.getLocationCode());
        return location;
    }

    @Transactional(rollbackFor = Exception.class)
    public Location updateLocation(Location location) {
        locationMapper.updateById(location);
        return location;
    }

    public Page<Location> pageLocations(
            Page<Location> page,
            String warehouseCode,
            String areaCode,
            String locationGroup,
            String locationType,
            String temperatureZone,
            String status,
            String isVirtual) {
        LambdaQueryWrapper<Location> wrapper = new LambdaQueryWrapper<>();
        if (warehouseCode != null) wrapper.eq(Location::getWarehouseCode, warehouseCode);
        if (areaCode != null) wrapper.eq(Location::getAreaCode, areaCode);
        if (locationGroup != null) wrapper.eq(Location::getLocationGroup, locationGroup);
        if (locationType != null) wrapper.eq(Location::getLocationType, locationType);
        if (temperatureZone != null) wrapper.eq(Location::getTemperatureZone, temperatureZone);
        if (status != null) wrapper.eq(Location::getStatus, status);
        if (isVirtual != null) wrapper.eq(Location::getIsVirtual, isVirtual);
        wrapper.orderByAsc(Location::getWarehouseCode).orderByAsc(Location::getSortNo);
        return locationMapper.selectPage(page, wrapper);
    }

    public Location getLocationByCode(String locationCode) {
        return locationMapper.selectOne(
                new LambdaQueryWrapper<Location>().eq(Location::getLocationCode, locationCode));
    }

    /** 查询空库位（上架推荐） */
    public List<Location> findEmptyLocations(
            String warehouseCode, String areaCode, String temperatureZone, String locationType) {
        return locationMapper.selectList(
                new LambdaQueryWrapper<Location>()
                        .eq(Location::getWarehouseCode, warehouseCode)
                        .eq(areaCode != null, Location::getAreaCode, areaCode)
                        .eq(temperatureZone != null, Location::getTemperatureZone, temperatureZone)
                        .eq(locationType != null, Location::getLocationType, locationType)
                        .eq(Location::getStatus, LocationStatus.EMPTY.getCode())
                        .eq(Location::getIsVirtual, "N")
                        .orderByAsc(Location::getSortNo));
    }

    /** 更新库位状态 */
    @Transactional(rollbackFor = Exception.class)
    public Location updateLocationStatus(String locationCode, String status) {
        Location location = getLocationByCode(locationCode);
        if (location == null) throw new RuntimeException("库位不存在: " + locationCode);
        location.setStatus(status);
        locationMapper.updateById(location);
        log.info("库位状态更新: {}={}", locationCode, status);
        return location;
    }

    /** 冻结/解冻库位 */
    @Transactional(rollbackFor = Exception.class)
    public Location freezeLocation(String locationCode, boolean freeze) {
        return updateLocationStatus(
                locationCode,
                freeze ? LocationStatus.FROZEN.getCode() : LocationStatus.NORMAL.getCode());
    }

    /** 更新库位容量使用 */
    @Transactional(rollbackFor = Exception.class)
    public Location updateLocationCapacity(String locationCode, BigDecimal usedCapacity) {
        Location location = getLocationByCode(locationCode);
        if (location == null) throw new RuntimeException("库位不存在: " + locationCode);
        location.setUsedCapacity(usedCapacity);
        // 自动判断状态
        if (location.getCapacity() != null
                && location.getCapacity().compareTo(BigDecimal.ZERO) > 0) {
            if (usedCapacity.compareTo(BigDecimal.ZERO) == 0) {
                location.setStatus(LocationStatus.EMPTY.getCode());
            } else if (usedCapacity.compareTo(location.getCapacity()) >= 0) {
                location.setStatus(LocationStatus.FULL.getCode());
            } else {
                location.setStatus(LocationStatus.NORMAL.getCode());
            }
        }
        locationMapper.updateById(location);
        return location;
    }

    /** 按路径排序查询库位（拣货路径规划） S型路径：奇数排从下到上，偶数排从上到下 */
    public List<Location> findLocationsByPath(String warehouseCode, List<String> locationCodes) {
        if (locationCodes == null || locationCodes.isEmpty()) {
            return List.of();
        }
        return locationMapper.selectList(
                new LambdaQueryWrapper<Location>()
                        .eq(Location::getWarehouseCode, warehouseCode)
                        .in(Location::getLocationCode, locationCodes)
                        .orderByAsc(Location::getRowNo)
                        .orderByAsc(Location::getColumnNo)
                        .orderByAsc(Location::getLevelNo));
    }

    /** 批量生成库位 */
    @Transactional(rollbackFor = Exception.class)
    public int batchGenerateLocations(
            String warehouseCode,
            String areaCode,
            String locationGroup,
            String locationType,
            String temperatureZone,
            String rowPrefix,
            int rowStart,
            int rowEnd,
            String columnPrefix,
            int columnStart,
            int columnEnd,
            String levelPrefix,
            int levelStart,
            int levelEnd,
            BigDecimal capacity,
            BigDecimal maxWeight) {
        int count = 0;
        for (int r = rowStart; r <= rowEnd; r++) {
            for (int c = columnStart; c <= columnEnd; c++) {
                for (int l = levelStart; l <= levelEnd; l++) {
                    String locationCode =
                            String.format(
                                    "%s-%s-%s%02d-%s%02d-%s%02d",
                                    warehouseCode,
                                    areaCode,
                                    rowPrefix,
                                    r,
                                    columnPrefix,
                                    c,
                                    levelPrefix,
                                    l);
                    Location location = new Location();
                    location.setLocationCode(locationCode);
                    location.setWarehouseCode(warehouseCode);
                    location.setAreaCode(areaCode);
                    location.setLocationGroup(locationGroup);
                    location.setRowNo(rowPrefix + r);
                    location.setColumnNo(columnPrefix + c);
                    location.setLevelNo(levelPrefix + l);
                    location.setLocationType(locationType);
                    location.setTemperatureZone(temperatureZone);
                    location.setStatus(LocationStatus.EMPTY.getCode());
                    location.setCapacity(capacity);
                    location.setUsedCapacity(BigDecimal.ZERO);
                    location.setMaxWeight(maxWeight);
                    location.setSortNo(count + 1);
                    location.setIsVirtual("N");
                    locationMapper.insert(location);
                    count++;
                }
            }
        }
        log.info("批量生成库位: 仓库={}, 库区={}, 数量={}", warehouseCode, areaCode, count);
        return count;
    }
}
