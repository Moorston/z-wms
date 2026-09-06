package com.xwms.integration.adapter.express;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import com.xwms.integration.adapter.AbstractIntegrationAdapter;

import lombok.extern.slf4j.Slf4j;

/** 顺丰快递适配器 对接顺丰开放平台API 核心场景： 1. 批量获取快递单号 2. 物流轨迹查询 3. 运费计算 4. 电子面单打印 5. 签收回单 */
@Slf4j
@Component
public class SfExpressAdapter extends AbstractIntegrationAdapter {

    @Value("${integration.sf.base-url:https://bspgw.sf-express.com}")
    private String baseUrl;

    @Value("${integration.sf.client-code:}")
    private String clientCode;

    @Value("${integration.sf.check-word:}")
    private String checkWord;

    @Override
    public String getPluginId() {
        return "sf-express";
    }

    @Override
    public String getPluginName() {
        return "顺丰快递适配器";
    }

    @Override
    public String getSystemType() {
        return "EXPRESS";
    }

    @Override
    public int getPriority() {
        return 100;
    }

    @Override
    protected String getBaseUrl() {
        return baseUrl;
    }

    @Override
    protected HttpHeaders buildAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/x-www-form-urlencoded");
        // 顺丰签名：MD5(msgData + timestamp + checkWord)
        return headers;
    }

    /** 获取快递单号（批量） */
    public Map<String, Object> getWaybillNo(Map<String, Object> request) {
        log.info(
                "顺丰获取快递单: orderNo={}, count={}",
                request.get("orderNo"),
                request.getOrDefault("count", 1));
        return doPost("/std/service/OrderService", request);
    }

    /** 查询物流轨迹 */
    public Map<String, Object> queryRoute(String waybillNo) {
        log.info("顺丰查询轨迹: waybillNo={}", waybillNo);
        return doPost("/std/service/RouteService", Map.of("waybillNo", waybillNo));
    }

    /** 计算运费 */
    public Map<String, Object> calculateFreight(Map<String, Object> request) {
        return doPost("/std/service/QueryService", request);
    }

    @Override
    public Map<String, Object> pushOrder(Map<String, Object> order) {
        // 顺丰下单接口
        return getWaybillNo(order);
    }

    @Override
    public Map<String, Object> pullOrder(Map<String, Object> params) {
        return queryRoute((String) params.get("waybillNo"));
    }
}
