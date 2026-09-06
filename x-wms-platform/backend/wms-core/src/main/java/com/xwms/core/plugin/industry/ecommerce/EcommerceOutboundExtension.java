package com.xwms.core.plugin.industry.ecommerce;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.xwms.common.plugin.extension.ExtensionResult;
import com.xwms.common.plugin.extension.OutboundExtension;
import com.xwms.core.plugin.industry.ecommerce.service.EcommercePreSaleService;
import com.xwms.core.plugin.industry.ecommerce.service.EcommerceWaveService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 电商大促出库扩展插件 核心管控：预售预占、大促波次优化、极速出库、合单拆单、快递单批量 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EcommerceOutboundExtension implements OutboundExtension {

    private final EcommercePreSaleService preSaleService;
    private final EcommerceWaveService waveService;

    @Override
    public String getPluginId() {
        return "ecommerce-outbound";
    }

    @Override
    public String getPluginName() {
        return "电商大促出库插件";
    }

    @Override
    public int getPriority() {
        return 150;
    }

    /** 订单创建前：预售订单预占活动库存 */
    @Override
    public ExtensionResult beforeOrderCreate(Map<String, Object> context) {
        boolean isPreSale = Boolean.TRUE.equals(context.get("preSale"));
        if (!isPreSale) return ExtensionResult.pass();

        String activityId = (String) context.get("activityId");
        String sku = (String) context.get("sku");
        int qty = (int) context.getOrDefault("qty", 1);
        String orderNo = (String) context.get("orderNo");

        boolean success = preSaleService.preAllocate(activityId, sku, qty, orderNo);
        if (!success) {
            return ExtensionResult.reject("预售活动库存不足，请选择其他商品或稍后再试");
        }
        return ExtensionResult.intercept(
                "预售预占成功", Map.of("allocateType", "PRE_SALE", "expireMinutes", 30));
    }

    /** 分配前校验：大促期间优先分配活动库存 */
    @Override
    public ExtensionResult beforeAllocation(Map<String, Object> context) {
        boolean isPromo = Boolean.TRUE.equals(context.get("promoOrder"));
        if (!isPromo) return ExtensionResult.pass();
        // 大促订单标记，波次服务会特殊处理
        return ExtensionResult.intercept("大促订单", Map.of("waveStrategy", "PROMO"));
    }

    /** 复核校验：极速出库模式跳过复核 */
    @Override
    public ExtensionResult beforeReview(Map<String, Object> context) {
        if (waveService.isFastShipMode(context)) {
            log.info("极速出库模式，跳过复核: orderNo={}", context.get("orderNo"));
            return ExtensionResult.intercept("极速出库跳过复核", Map.of("skipReview", true));
        }
        return ExtensionResult.pass();
    }

    /** 发运前：大促批量获取快递单 */
    @Override
    public ExtensionResult beforeShip(Map<String, Object> context) {
        boolean isPromo = Boolean.TRUE.equals(context.get("promoOrder"));
        if (isPromo) {
            log.info("大促订单发运: orderNo={}, 触发批量快递单获取", context.get("orderNo"));
            // TODO: 发送到Kafka wms-express-get topic，批量聚合获取
        }
        return ExtensionResult.pass();
    }
}
