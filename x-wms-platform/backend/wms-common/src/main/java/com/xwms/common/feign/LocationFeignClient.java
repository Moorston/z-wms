package com.xwms.common.feign;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import com.xwms.common.core.Result;
import com.xwms.common.feign.config.FeignConfig;
import com.xwms.common.feign.dto.LocationDTO;
import com.xwms.common.feign.fallback.LocationFeignFallbackFactory;

/** 库位Feign客户端 wms-core调用wms-base获取库位信息/空库位推荐/路径排序 */
@FeignClient(
        name = "wms-base",
        contextId = "locationFeignClient",
        path = "/api/location",
        configuration = FeignConfig.class,
        fallbackFactory = LocationFeignFallbackFactory.class)
public interface LocationFeignClient {

    @GetMapping("/empty")
    Result<List<LocationDTO>> findEmpty(
            @RequestParam("warehouse") String warehouse,
            @RequestParam(value = "areaCode", required = false) String areaCode,
            @RequestParam(value = "temperatureZone", required = false) String temperatureZone,
            @RequestParam(value = "locationType", required = false) String locationType);

    @PostMapping("/{locationCode}/status")
    Result<Void> updateStatus(
            @PathVariable("locationCode") String locationCode,
            @RequestParam("status") String status);

    @PostMapping("/route")
    Result<List<LocationDTO>> listByRouteOrder(
            @RequestParam("warehouse") String warehouse, @RequestBody List<String> locationCodes);
}
