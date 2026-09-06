package com.xwms.integration.adapter.tms;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import com.xwms.integration.adapter.AbstractIntegrationAdapter;

import lombok.extern.slf4j.Slf4j;

/** TMS运输管理系统适配器 对接外部TMS系统，核心场景： 1. 运输计划下发 2. 车辆/司机分配 3. 运输轨迹跟踪 4. 签收回单 5. 运费结算 */
@Slf4j
@Component
public class TmsAdapter extends AbstractIntegrationAdapter {

    @Value("${integration.tms.base-url:http://tms.example.com/api}")
    private String baseUrl;

    @Value("${integration.tms.api-key:}")
    private String apiKey;

    @Override
    public String getPluginId() {
        return "tms";
    }

    @Override
    public String getPluginName() {
        return "TMS运输适配器";
    }

    @Override
    public String getSystemType() {
        return "TMS";
    }

    @Override
    protected String getBaseUrl() {
        return baseUrl;
    }

    @Override
    protected HttpHeaders buildAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-API-Key", apiKey);
        return headers;
    }

    /** 创建运输计划 */
    public Map<String, Object> createTransportPlan(Map<String, Object> plan) {
        log.info("TMS创建运输计划: orderNo={}, carrier={}", plan.get("orderNo"), plan.get("carrier"));
        return doPost("/transport/plans", plan);
    }

    /** 查询运输轨迹 */
    public Map<String, Object> queryTrack(String transportNo) {
        return doGet("/transport/tracks/" + transportNo, null);
    }

    /** 签收回单确认 */
    public Map<String, Object> confirmReceipt(Map<String, Object> receipt) {
        return doPost("/transport/receipts", receipt);
    }

    @Override
    public Map<String, Object> pushOrder(Map<String, Object> order) {
        return createTransportPlan(order);
    }

    @Override
    public Map<String, Object> pullOrder(Map<String, Object> params) {
        return queryTrack((String) params.get("transportNo"));
    }
}
