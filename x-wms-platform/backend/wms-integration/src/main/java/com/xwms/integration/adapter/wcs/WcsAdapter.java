package com.xwms.integration.adapter.wcs;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import com.xwms.integration.adapter.AbstractIntegrationAdapter;

import lombok.extern.slf4j.Slf4j;

/**
 * WCS仓储控制系统适配器 对接自动化设备（堆垛机/穿梭车/AGV/输送线/分拣机） 核心场景： 1. 入库任务下发（托盘入库） 2. 出库任务下发（托盘出库） 3. 移库任务下发 4.
 * 设备状态监控 5. 任务结果回传
 */
@Slf4j
@Component
public class WcsAdapter extends AbstractIntegrationAdapter {

    @Value("${integration.wcs.base-url:http://wcs.example.com/api}")
    private String baseUrl;

    @Value("${integration.wcs.api-key:}")
    private String apiKey;

    @Override
    public String getPluginId() {
        return "wcs";
    }

    @Override
    public String getPluginName() {
        return "WCS仓储控制适配器";
    }

    @Override
    public String getSystemType() {
        return "WCS";
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

    /** 下发入库任务 */
    public Map<String, Object> sendInboundTask(Map<String, Object> task) {
        log.info(
                "WCS入库任务: palletNo={}, from={}, to={}",
                task.get("palletNo"),
                task.get("fromLocation"),
                task.get("toLocation"));
        return doPost("/tasks/inbound", task);
    }

    /** 下发出库任务 */
    public Map<String, Object> sendOutboundTask(Map<String, Object> task) {
        log.info(
                "WCS出库任务: palletNo={}, from={}, to={}",
                task.get("palletNo"),
                task.get("fromLocation"),
                task.get("toLocation"));
        return doPost("/tasks/outbound", task);
    }

    /** 下发移库任务 */
    public Map<String, Object> sendTransferTask(Map<String, Object> task) {
        return doPost("/tasks/transfer", task);
    }

    /** 查询设备状态 */
    public Map<String, Object> queryDeviceStatus() {
        return doGet("/devices/status", null);
    }

    /** 查询任务状态 */
    public Map<String, Object> queryTaskStatus(String taskId) {
        return doGet("/tasks/" + taskId + "/status", null);
    }

    @Override
    public Map<String, Object> pushOrder(Map<String, Object> order) {
        String taskType = (String) order.getOrDefault("taskType", "INBOUND");
        return switch (taskType) {
            case "INBOUND" -> sendInboundTask(order);
            case "OUTBOUND" -> sendOutboundTask(order);
            case "TRANSFER" -> sendTransferTask(order);
            default -> sendInboundTask(order);
        };
    }

    @Override
    public Map<String, Object> pullOrder(Map<String, Object> params) {
        return queryTaskStatus((String) params.get("taskId"));
    }
}
