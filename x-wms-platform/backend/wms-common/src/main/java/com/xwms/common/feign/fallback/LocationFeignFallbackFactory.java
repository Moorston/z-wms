package com.xwms.common.feign.fallback;

import java.util.List;

import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import com.xwms.common.core.Result;
import com.xwms.common.feign.LocationFeignClient;
import com.xwms.common.feign.dto.LocationDTO;

import lombok.extern.slf4j.Slf4j;

/** 库位Feign降级工厂 当wms-base服务不可用时，提供降级响应 注意：降级仅用于查询类接口，写操作不建议降级 */
@Slf4j
@Component
public class LocationFeignFallbackFactory implements FallbackFactory<LocationFeignClient> {

    @Override
    public LocationFeignClient create(Throwable cause) {
        log.error("LocationFeignClient调用失败，触发降级", cause);
        return new LocationFeignClient() {
            @Override
            public Result<List<LocationDTO>> findEmpty(
                    String warehouse,
                    String areaCode,
                    String temperatureZone,
                    String locationType) {
                log.warn("库位查询降级: warehouse={}, areaCode={}", warehouse, areaCode);
                // 降级返回空列表，由调用方决定是否使用本地缓存
                return Result.success(List.of());
            }

            @Override
            public Result<Void> updateStatus(String locationCode, String status) {
                // 写操作不降级，直接抛出异常
                throw new RuntimeException("库位状态更新失败，服务不可用: " + locationCode);
            }

            @Override
            public Result<List<LocationDTO>> listByRouteOrder(
                    String warehouse, List<String> locationCodes) {
                log.warn("库位路径排序降级: warehouse={}", warehouse);
                return Result.success(List.of());
            }
        };
    }
}
