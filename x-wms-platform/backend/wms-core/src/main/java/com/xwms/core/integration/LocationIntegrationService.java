package com.xwms.core.integration;

import java.util.List;

import org.springframework.stereotype.Service;

import com.xwms.common.core.Result;
import com.xwms.common.feign.LocationFeignClient;
import com.xwms.common.feign.dto.LocationDTO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 库位集成服务 封装wms-core对wms-base库位服务的Feign调用 提供空库位推荐、路径排序、状态更新等能力 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LocationIntegrationService {

    private final LocationFeignClient locationFeignClient;

    /** 推荐空库位（上架用） 按温区、库区、库位类型筛选，按排序号返回 */
    public List<LocationDTO> recommendEmptyLocations(
            String warehouse, String areaCode, String temperatureZone, String locationType) {
        Result<List<LocationDTO>> result =
                locationFeignClient.findEmpty(warehouse, areaCode, temperatureZone, locationType);
        if (result == null || result.getData() == null) {
            log.warn("空库位查询返回空: warehouse={}", warehouse);
            return List.of();
        }
        return result.getData();
    }

    /** 获取最优空库位（第一个） */
    public LocationDTO getBestEmptyLocation(
            String warehouse, String areaCode, String temperatureZone, String locationType) {
        List<LocationDTO> locations =
                recommendEmptyLocations(warehouse, areaCode, temperatureZone, locationType);
        return locations.isEmpty() ? null : locations.get(0);
    }

    /** S型路径排序（拣货路径规划用） 奇数排正序，偶数排倒序，避免重走 */
    public List<LocationDTO> sortByRoute(String warehouse, List<String> locationCodes) {
        if (locationCodes == null || locationCodes.isEmpty()) {
            return List.of();
        }
        Result<List<LocationDTO>> result =
                locationFeignClient.listByRouteOrder(warehouse, locationCodes);
        if (result == null || result.getData() == null) {
            log.warn("库位路径排序返回空: warehouse={}, count={}", warehouse, locationCodes.size());
            return List.of();
        }
        return result.getData();
    }

    /** 更新库位状态 上架后EMPTY→NORMAL，出库后NORMAL→EMPTY */
    public void updateLocationStatus(String locationCode, String status) {
        try {
            locationFeignClient.updateStatus(locationCode, status);
            log.info("库位状态更新: location={}, status={}", locationCode, status);
        } catch (Exception e) {
            // 库位状态更新失败不影响主流程，记录告警
            log.error(
                    "库位状态更新失败: location={}, status={}, error={}",
                    locationCode,
                    status,
                    e.getMessage());
        }
    }

    /** 批量更新库位状态 */
    public void batchUpdateStatus(List<String> locationCodes, String status) {
        locationCodes.forEach(code -> updateLocationStatus(code, status));
    }
}
