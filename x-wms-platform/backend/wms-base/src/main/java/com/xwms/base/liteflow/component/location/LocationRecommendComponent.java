package com.xwms.base.liteflow.component.location;

import java.util.List;
import java.util.Map;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

import com.xwms.base.location.entity.Location;
import com.xwms.base.location.service.LocationManagementService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** LiteFlow库位推荐组件 - 上架流程中推荐空库位 根据上架规则（库区/温区/库位类型）推荐最优空库位 */
@Slf4j
@LiteflowComponent("locationRecommend")
@RequiredArgsConstructor
public class LocationRecommendComponent extends NodeComponent {

    private final LocationManagementService locationManagementService;

    @Override
    public void process() {
        @SuppressWarnings("unchecked")
        Map<String, Object> context = this.getContextBean(Map.class);
        if (context == null || context.get("warehouseCode") == null) {
            log.info("无库位推荐上下文, 跳过");
            return;
        }
        try {
            String warehouseCode = context.get("warehouseCode").toString();
            String areaCode =
                    context.get("areaCode") != null ? context.get("areaCode").toString() : null;
            String temperatureZone =
                    context.get("temperatureZone") != null
                            ? context.get("temperatureZone").toString()
                            : null;
            String locationType =
                    context.get("locationType") != null
                            ? context.get("locationType").toString()
                            : null;

            List<Location> emptyLocations =
                    locationManagementService.findEmptyLocations(
                            warehouseCode, areaCode, temperatureZone, locationType);

            if (!emptyLocations.isEmpty()) {
                Location recommended = emptyLocations.get(0);
                context.put("recommendedLocation", recommended.getLocationCode());
                log.info("库位推荐: {} -> {}", warehouseCode, recommended.getLocationCode());
            } else {
                log.warn("无可用空库位: 仓库={}", warehouseCode);
            }
        } catch (Exception e) {
            log.error("库位推荐失败: {}", e.getMessage());
        }
    }

    @Override
    public boolean isAccess() {
        return true;
    }
}
