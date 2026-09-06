package com.xwms.integration.adapter.express;

import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import com.xwms.integration.adapter.AbstractIntegrationAdapter;

import lombok.extern.slf4j.Slf4j;

/**
 * 菜鸟网络适配器
 *
 * <p>核心API： - 电子面单取号：cainiao.waybill.get - 物流轨迹查询：cainiao.tracking.query -
 * 运费预估：cainiao.estimate.price - 预约揽收：cainiao.pickup.reserve
 *
 * <p>鉴权：OAuth2 + app_key/app_secret
 */
@Slf4j
@Component
public class CainiaoAdapter extends AbstractIntegrationAdapter {

    @Override
    public String getPluginId() {
        return "CAINIAO";
    }

    @Override
    public String getPluginName() {
        return "菜鸟网络";
    }

    @Override
    public String getSystemType() {
        return "EXPRESS";
    }

    @Override
    protected String getBaseUrl() {
        return "https://eco.taobao.com/router/rest";
    }

    @Override
    protected HttpHeaders buildAuthHeaders() {
        // TODO: 菜鸟签名算法（与淘宝TOP一致，MD5签名）
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/x-www-form-urlencoded");
        headers.set("app_key", "cainiao_app_key");
        return headers;
    }

    @Override
    public Map<String, Object> pushOrder(Map<String, Object> order) {
        log.info("菜鸟电子面单取号: orderNo={}", order.get("orderNo"));
        // TODO: 调用cainiao.waybill.get
        return Map.of("success", true, "waybillNo", "CN" + System.currentTimeMillis());
    }

    @Override
    public Map<String, Object> pullOrder(Map<String, Object> params) {
        log.info("菜鸟物流轨迹查询: waybillNo={}", params.get("waybillNo"));
        // TODO: 调用cainiao.tracking.query
        return Map.of("tracking", java.util.List.of(), "status", "UNKNOWN");
    }
}
