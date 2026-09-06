package com.xwms.integration.adapter.ecommerce;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import com.xwms.integration.adapter.AbstractIntegrationAdapter;

import lombok.extern.slf4j.Slf4j;

/**
 * 淘宝/天猫电商适配器 对接淘宝开放平台TOP API 核心场景： 1. 订单拉取（已付款待发货订单） 2. 发货回传（物流单号同步） 3. 库存同步（WMS库存→淘宝） 4. 退款/退货处理
 * 5. 商品信息同步
 */
@Slf4j
@Component
public class TaobaoAdapter extends AbstractIntegrationAdapter {

    @Value("${integration.taobao.base-url:https://eco.taobao.com/router/rest}")
    private String baseUrl;

    @Value("${integration.taobao.app-key:}")
    private String appKey;

    @Value("${integration.taobao.app-secret:}")
    private String appSecret;

    @Value("${integration.taobao.access-token:}")
    private String accessToken;

    @Override
    public String getPluginId() {
        return "taobao";
    }

    @Override
    public String getPluginName() {
        return "淘宝/天猫适配器";
    }

    @Override
    public String getSystemType() {
        return "ECOMMERCE";
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
        return headers;
    }

    /** 拉取淘宝订单（已付款待发货） API: taobao.trades.sold.get */
    @Override
    public Map<String, Object> pullOrder(Map<String, Object> params) {
        String status = (String) params.getOrDefault("status", "WAIT_SELLER_SEND_GOODS");
        log.info("淘宝拉取订单: status={}", status);
        Map<String, String> queryParams =
                Map.of(
                        "method", "taobao.trades.sold.get",
                        "app_key", appKey,
                        "session", accessToken,
                        "timestamp", String.valueOf(System.currentTimeMillis()),
                        "format", "json",
                        "v", "2.0",
                        "sign_method", "md5",
                        "status", status,
                        "fields",
                                "tid,status,receiver_name,receiver_mobile,receiver_address,orders");
        // TODO: 生成签名 sign = MD5(appSecret + params排序拼接 + appSecret)
        return doGet("", queryParams);
    }

    /** 发货回传（物流单号同步到淘宝） API: taobao.logistics.online.send */
    @Override
    public Map<String, Object> pushOrder(Map<String, Object> order) {
        log.info("淘宝发货回传: tid={}, waybillNo={}", order.get("tid"), order.get("waybillNo"));
        return doPost(
                "",
                Map.of(
                        "method",
                        "taobao.logistics.online.send",
                        "app_key",
                        appKey,
                        "session",
                        accessToken,
                        "tid",
                        order.get("tid"),
                        "out_sid",
                        order.get("waybillNo"),
                        "company_code",
                        order.get("expressCode")));
    }

    /** 库存同步（WMS库存→淘宝） API: taobao.item.quantity.update */
    public Map<String, Object> syncInventory(Map<String, Object> inventory) {
        log.info("淘宝库存同步: numIid={}, qty={}", inventory.get("numIid"), inventory.get("qty"));
        return doPost(
                "",
                Map.of(
                        "method",
                        "taobao.item.quantity.update",
                        "app_key",
                        appKey,
                        "session",
                        accessToken,
                        "num_iid",
                        inventory.get("numIid"),
                        "quantity",
                        inventory.get("qty")));
    }

    /** 退款/退货单拉取 */
    public Map<String, Object> pullRefund(Map<String, Object> params) {
        return doGet(
                "",
                Map.of(
                        "method",
                        "taobao.refunds.receive.get",
                        "app_key",
                        appKey,
                        "session",
                        accessToken,
                        "status",
                        String.valueOf(params.getOrDefault("status", "WAIT_SELLER_AGREE"))));
    }
}
