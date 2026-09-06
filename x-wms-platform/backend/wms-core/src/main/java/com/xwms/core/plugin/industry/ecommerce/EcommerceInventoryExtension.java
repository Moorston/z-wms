package com.xwms.core.plugin.industry.ecommerce;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.xwms.common.plugin.extension.ExtensionResult;
import com.xwms.common.plugin.extension.InventoryExtension;
import com.xwms.core.plugin.industry.ecommerce.service.EcommercePreSaleService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 电商大促库存扩展插件 核心管控：活动库存池隔离、大促库存锁定/释放、超卖防护 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EcommerceInventoryExtension implements InventoryExtension {

    private final EcommercePreSaleService preSaleService;

    @Override
    public String getPluginId() {
        return "ecommerce-inventory";
    }

    @Override
    public String getPluginName() {
        return "电商大促库存插件";
    }

    @Override
    public int getPriority() {
        return 150;
    }

    /** 预占前校验：大促活动库存检查 */
    @Override
    public ExtensionResult beforeAllocate(Map<String, Object> context) {
        String activityId = (String) context.get("activityId");
        if (activityId == null) return ExtensionResult.pass();
        String sku = (String) context.get("sku");
        int avail = preSaleService.getActivityStock(activityId, sku);
        if (avail <= 0) {
            return ExtensionResult.reject("活动库存已售罄: activity=" + activityId + ", sku=" + sku);
        }
        return ExtensionResult.pass();
    }

    /** 库存预警：大促库存售罄预警 */
    @Override
    public void onInventoryAlert(Map<String, Object> context) {
        String activityId = (String) context.get("activityId");
        if (activityId == null) return;
        String sku = (String) context.get("sku");
        int avail = preSaleService.getActivityStock(activityId, sku);
        int total = (int) context.getOrDefault("totalStock", 0);
        double soldRate = total > 0 ? (1 - (double) avail / total) * 100 : 0;
        if (soldRate >= 90) {
            log.error(
                    "大促库存售罄预警: activity={}, sku={}, 已售{}%, 剩余={}",
                    activityId, sku, String.format("%.1f", soldRate), avail);
        } else if (soldRate >= 70) {
            log.warn(
                    "大促库存预警: activity={}, sku={}, 已售{}%",
                    activityId, sku, String.format("%.1f", soldRate));
        }
    }
}
