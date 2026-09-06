package com.xwms.base.location.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import com.xwms.base.location.entity.Location;
import com.xwms.base.location.mapper.LocationMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 库位服务 核心能力： 1. 库位CRUD 2. 空库位查询（上架用） 3. 库位状态管理 4. 库位路径排序（拣货路径规划用） */
@Slf4j
@Service
@RequiredArgsConstructor
public class LocationService {

    private final LocationMapper locationMapper;

    /** 查询空库位（上架推荐） 按库区/温区/类型筛选，按排序号排序 */
    public List<Location> findEmptyLocations(
            String warehouseCode, String areaCode, String temperatureZone, String locationType) {
        return locationMapper.selectList(
                new LambdaQueryWrapper<Location>()
                        .eq(Location::getWarehouseCode, warehouseCode)
                        .eq(areaCode != null, Location::getAreaCode, areaCode)
                        .eq(temperatureZone != null, Location::getTemperatureZone, temperatureZone)
                        .eq(locationType != null, Location::getLocationType, locationType)
                        .eq(Location::getStatus, "EMPTY")
                        .orderByAsc(Location::getSortNo));
    }

    /** 更新库位状态 */
    public void updateStatus(String locationCode, String status) {
        Location location =
                locationMapper.selectOne(
                        new LambdaQueryWrapper<Location>()
                                .eq(Location::getLocationCode, locationCode));
        if (location == null) {
            throw new RuntimeException("库位不存在: " + locationCode);
        }
        location.setStatus(status);
        locationMapper.updateById(location);
        log.info("库位状态更新: location={}, status={}", locationCode, status);
    }

    /** 按路径排序查询库位（拣货路径规划） S型路径：奇数排从下到上，偶数排从上到下 */
    public List<Location> listByRouteOrder(String warehouseCode, List<String> locationCodes) {
        if (locationCodes == null || locationCodes.isEmpty()) {
            return List.of();
        }
        List<Location> locations =
                locationMapper.selectList(
                        new LambdaQueryWrapper<Location>()
                                .eq(Location::getWarehouseCode, warehouseCode)
                                .in(Location::getLocationCode, locationCodes));
        // S型路径排序：先按排，再按列（奇偶排方向相反），再按层
        locations.sort(
                (a, b) -> {
                    int rowCompare = a.getRowNo().compareTo(b.getRowNo());
                    if (rowCompare != 0) return rowCompare;
                    // 奇数排正序，偶数排倒序（S型）
                    int row = Integer.parseInt(a.getRowNo().replaceAll("\\D", ""));
                    int colCompare =
                            row % 2 == 1
                                    ? a.getColumnNo().compareTo(b.getColumnNo())
                                    : b.getColumnNo().compareTo(a.getColumnNo());
                    if (colCompare != 0) return colCompare;
                    return a.getLevelNo().compareTo(b.getLevelNo());
                });
        return locations;
    }
}
