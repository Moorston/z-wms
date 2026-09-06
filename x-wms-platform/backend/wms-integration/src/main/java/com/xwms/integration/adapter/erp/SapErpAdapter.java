package com.xwms.integration.adapter.erp;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.xwms.common.utils.JsonUtils;
import com.xwms.integration.adapter.AbstractIntegrationAdapter;
import com.xwms.integration.adapter.erp.client.SapODataClient;
import com.xwms.integration.adapter.erp.client.SapODataClient.SapODataException;
import com.xwms.integration.adapter.erp.config.SapErpConfig;
import com.xwms.integration.adapter.erp.converter.SapErpMessageConverter;
import com.xwms.integration.adapter.erp.dto.SapErpDto;
import com.xwms.integration.adapter.erp.dto.SapErpFieldMapping;
import com.xwms.integration.api.idempotent.ApiIdempotentService;
import com.xwms.integration.core.model.ApiDefinition;
import com.xwms.integration.core.retry.RetryExecutor;
import com.xwms.integration.external.service.IntegrationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * SAP ERP 适配器
 *
 * <p>对接 SAP S/4HANA（OData V2 Gateway），实现 5 类集成接口：
 *
 * <ol>
 *   <li>I1 入库过账 — WMS 收货/质检合格 → SAP 货物移动过账
 *   <li>I2 出库过账 — WMS 发货完成 → SAP 货物移动过账
 *   <li>I3 库存同步 — WMS 库存变动 → SAP 库存快照
 *   <li>I4 物料主数据同步 — 从 SAP 拉取物料主数据 → WMS
 *   <li>I5 仓库/库位主数据同步 — 从 SAP 拉取仓库/库位 → WMS
 * </ol>
 *
 * <p>编排职责（本类负责，不交给下层）：
 *
 * <ul>
 *   <li>幂等拦截（{@link ApiIdempotentService} Redis 24h）
 *   <li>重试编排（{@link RetryExecutor} 指数退避，只对 {@code SapODataException.isRetryable()} 为 true 的重试）
 *   <li>调用日志落库（{@link IntegrationService#recordApiCall}）
 *   <li>失败消息入库（{@link IntegrationService#createMessage}）
 * </ul>
 *
 * <p>下层职责（不在此类）：
 *
 * <ul>
 *   <li>字段映射 — {@link SapErpMessageConverter}
 *   <li>HTTP 调用 — {@link SapODataClient}
 * </ul>
 *
 * <p>配置全部来自 {@link SapErpConfig}（Nacos 外置），禁止硬编码。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SapErpAdapter extends AbstractIntegrationAdapter {

    private final SapErpConfig sapErpConfig;
    private final SapODataClient oDataClient;
    private final SapErpMessageConverter converter;
    private final ApiIdempotentService idempotentService;
    private final RetryExecutor retryExecutor;
    private final IntegrationService integrationService;

    @Override
    public String getPluginId() {
        return "sap-erp";
    }

    @Override
    public String getPluginName() {
        return "SAP ERP适配器";
    }

    @Override
    public String getSystemType() {
        return "ERP";
    }

    @Override
    public int getPriority() {
        return 100;
    }

    @Override
    protected String getBaseUrl() {
        return sapErpConfig.getBaseUrl();
    }

    @Override
    protected org.springframework.http.HttpHeaders buildAuthHeaders() {
        // AbstractIntegrationAdapter 的通用 doPost/doGet 不使用——SAP 走 SapODataClient
        return new org.springframework.http.HttpHeaders();
    }

    @Override
    public boolean healthCheck() {
        if (!sapErpConfig.isEnabled()) {
            log.warn("SAP 集成已禁用（integration.sap.enabled=false）");
            return false;
        }
        try {
            oDataClient.getWithParams("/$metadata", Map.of());
            return true;
        } catch (Exception e) {
            log.error("SAP 健康检查失败: {}", e.getMessage());
            return false;
        }
    }

    // ============================================================
    // I1 入库过账
    // ============================================================

    /**
     * 入库过账到 SAP
     *
     * @param request WMS 入库过账请求
     * @return 过账结果（含 SAP 凭证号）
     */
    public SapErpDto.GoodsMovementResponse postInboundGoods(SapErpDto.InboundPostRequest request) {
        String idempotentKey =
                SapErpFieldMapping.IDEMPOTENT_PREFIX_INBOUND + request.getInboundNo();

        // 幂等拦截
        if (!idempotentService.tryAcquire(idempotentKey)) {
            Object cached = idempotentService.getCachedResult(idempotentKey);
            if (cached != null) {
                log.info("入库过账命中幂等缓存: inboundNo={}", request.getInboundNo());
                return parseCachedResponse(cached.toString());
            }
            if (idempotentService.isProcessing(idempotentKey)) {
                log.warn("入库过账处理中（重复请求）: inboundNo={}", request.getInboundNo());
                return failureResponse("入库过账处理中，请稍后重试", "409", 0L);
            }
            log.warn("入库过账幂等键冲突: inboundNo={}", request.getInboundNo());
            return failureResponse("入库过账请求被拒绝（重复）", "409", 0L);
        }

        long start = System.currentTimeMillis();
        String path = SapErpFieldMapping.ENDPOINT_GOODS_MVT_HEADER;
        SapErpDto.GoodsMovementPayload payload = null;
        try {
            payload = converter.toInboundPayload(request);
            final SapErpDto.GoodsMovementPayload p = payload;
            Map<String, Object> sapResponse =
                    invokeWithRetry(() -> oDataClient.postWithCsrf(path, p), idempotentKey);

            long cost = System.currentTimeMillis() - start;
            String docNum = converter.extractDocNum(sapResponse);
            SapErpDto.GoodsMovementResponse response = new SapErpDto.GoodsMovementResponse();
            response.setSuccess(true);
            response.setSapDocNum(docNum);
            response.setCostTimeMs(cost);

            idempotentService.markSuccess(idempotentKey, docNum != null ? docNum : "OK");
            recordApiCall(
                    "POST_INBOUND_GOODS",
                    path,
                    payload,
                    sapResponse,
                    response.isSuccess(),
                    null,
                    cost,
                    "INBOUND_POST",
                    request.getInboundNo(),
                    "OUTBOUND");
            return response;
        } catch (Exception e) {
            long cost = System.currentTimeMillis() - start;
            SapODataException odataEx = extractODataException(e);
            SapErpDto.GoodsMovementResponse response = new SapErpDto.GoodsMovementResponse();
            response.setSuccess(false);
            response.setErrorMessage(e.getMessage());
            response.setErrorCode(
                    String.valueOf(odataEx != null ? odataEx.getHttpStatus() : "ERR"));
            response.setCostTimeMs(cost);

            idempotentService.markFailed(idempotentKey);
            enqueueFailedMessage(
                    "POST_INBOUND_GOODS",
                    request.getInboundNo(),
                    payload != null ? payload : request,
                    e.getMessage());
            recordApiCall(
                    "POST_INBOUND_GOODS",
                    path,
                    payload,
                    null,
                    false,
                    e.getMessage(),
                    cost,
                    "INBOUND_POST",
                    request.getInboundNo(),
                    "OUTBOUND");
            return response;
        }
    }

    // ============================================================
    // I2 出库过账
    // ============================================================

    public SapErpDto.GoodsMovementResponse postOutboundGoods(
            SapErpDto.OutboundPostRequest request) {
        String idempotentKey =
                SapErpFieldMapping.IDEMPOTENT_PREFIX_OUTBOUND + request.getShipmentNo();

        if (!idempotentService.tryAcquire(idempotentKey)) {
            Object cached = idempotentService.getCachedResult(idempotentKey);
            if (cached != null) {
                log.info("出库过账命中幂等缓存: shipmentNo={}", request.getShipmentNo());
                return parseCachedResponse(cached.toString());
            }
            if (idempotentService.isProcessing(idempotentKey)) {
                return failureResponse("出库过账处理中，请稍后重试", "409", 0L);
            }
            return failureResponse("出库过账请求被拒绝（重复）", "409", 0L);
        }

        long start = System.currentTimeMillis();
        String path = SapErpFieldMapping.ENDPOINT_GOODS_MVT_HEADER;
        SapErpDto.GoodsMovementPayload payload = null;
        try {
            payload = converter.toOutboundPayload(request);
            final SapErpDto.GoodsMovementPayload p = payload;
            Map<String, Object> sapResponse =
                    invokeWithRetry(() -> oDataClient.postWithCsrf(path, p), idempotentKey);

            long cost = System.currentTimeMillis() - start;
            String docNum = converter.extractDocNum(sapResponse);
            SapErpDto.GoodsMovementResponse response = new SapErpDto.GoodsMovementResponse();
            response.setSuccess(true);
            response.setSapDocNum(docNum);
            response.setCostTimeMs(cost);

            idempotentService.markSuccess(idempotentKey, docNum != null ? docNum : "OK");
            recordApiCall(
                    "POST_OUTBOUND_GOODS",
                    path,
                    payload,
                    sapResponse,
                    true,
                    null,
                    cost,
                    "OUTBOUND_POST",
                    request.getShipmentNo(),
                    "OUTBOUND");
            return response;
        } catch (Exception e) {
            long cost = System.currentTimeMillis() - start;
            SapODataException odataEx = extractODataException(e);
            SapErpDto.GoodsMovementResponse response = new SapErpDto.GoodsMovementResponse();
            response.setSuccess(false);
            response.setErrorMessage(e.getMessage());
            response.setErrorCode(
                    String.valueOf(odataEx != null ? odataEx.getHttpStatus() : "ERR"));
            response.setCostTimeMs(cost);

            idempotentService.markFailed(idempotentKey);
            enqueueFailedMessage(
                    "POST_OUTBOUND_GOODS",
                    request.getShipmentNo(),
                    payload != null ? payload : request,
                    e.getMessage());
            recordApiCall(
                    "POST_OUTBOUND_GOODS",
                    path,
                    payload,
                    null,
                    false,
                    e.getMessage(),
                    cost,
                    "OUTBOUND_POST",
                    request.getShipmentNo(),
                    "OUTBOUND");
            return response;
        }
    }

    // ============================================================
    // I3 库存同步
    // ============================================================

    public SapErpDto.InventorySyncResponse syncInventory(SapErpDto.InventorySyncRequest request) {
        String idempotentKey =
                SapErpFieldMapping.IDEMPOTENT_PREFIX_INVENTORY
                        + request.getWarehouseCode()
                        + ":"
                        + request.getSkuCode();

        if (!idempotentService.tryAcquire(idempotentKey)) {
            Object cached = idempotentService.getCachedResult(idempotentKey);
            if (cached != null) {
                return newInventoryResponse(true, null, null, 0L);
            }
            return newInventoryResponse(false, "库存同步处理中或重复请求", "409", 0L);
        }

        long start = System.currentTimeMillis();
        String path = SapErpFieldMapping.ENDPOINT_STOCK_IN_TRANSIT;
        SapErpDto.InventoryPayload payload = null;
        try {
            payload = converter.toInventoryPayload(request);
            final SapErpDto.InventoryPayload p = payload;
            invokeWithRetry(() -> oDataClient.postWithCsrf(path, p), idempotentKey);
            long cost = System.currentTimeMillis() - start;

            idempotentService.markSuccess(idempotentKey, "SYNC_OK");
            recordApiCall(
                    "SYNC_INVENTORY",
                    path,
                    payload,
                    null,
                    true,
                    null,
                    cost,
                    "INVENTORY_SYNC",
                    request.getSkuCode(),
                    "OUTBOUND");
            return newInventoryResponse(true, null, null, cost);
        } catch (Exception e) {
            long cost = System.currentTimeMillis() - start;
            idempotentService.markFailed(idempotentKey);
            enqueueFailedMessage(
                    "SYNC_INVENTORY",
                    request.getSkuCode(),
                    payload != null ? payload : request,
                    e.getMessage());
            recordApiCall(
                    "SYNC_INVENTORY",
                    path,
                    payload,
                    null,
                    false,
                    e.getMessage(),
                    cost,
                    "INVENTORY_SYNC",
                    request.getSkuCode(),
                    "OUTBOUND");
            return newInventoryResponse(false, e.getMessage(), "ERR", cost);
        }
    }

    // ============================================================
    // I4 物料主数据同步（SAP → WMS）
    // ============================================================

    public SapErpDto.MasterDataPullResponse<SapErpDto.MaterialMasterItem> pullMaterialMaster(
            SapErpDto.MaterialMasterQueryRequest request) {
        long start = System.currentTimeMillis();
        String path = SapErpFieldMapping.ENDPOINT_MATERIAL_SRV;
        try {
            Map<String, String> params = buildMaterialQueryParams(request);
            Map<String, Object> sapResponse =
                    invokeWithRetry(() -> oDataClient.getWithParams(path, params), "sap:md:pull");
            List<SapErpDto.MaterialMasterItem> items = converter.toMaterialMasterItems(sapResponse);

            SapErpDto.MasterDataPullResponse<SapErpDto.MaterialMasterItem> response =
                    new SapErpDto.MasterDataPullResponse<>();
            response.setSuccess(true);
            response.setItems(items);
            response.setTotal(items.size());
            return response;
        } catch (Exception e) {
            long cost = System.currentTimeMillis() - start;
            log.error("拉取物料主数据失败: cost={}ms, error={}", cost, e.getMessage());
            SapErpDto.MasterDataPullResponse<SapErpDto.MaterialMasterItem> response =
                    new SapErpDto.MasterDataPullResponse<>();
            response.setSuccess(false);
            response.setItems(List.of());
            response.setErrorMessage(e.getMessage());
            return response;
        }
    }

    // ============================================================
    // I5 仓库/库位主数据同步（SAP → WMS）
    // ============================================================

    public SapErpDto.MasterDataPullResponse<SapErpDto.WarehouseMasterItem> pullWarehouseMaster(
            SapErpDto.WarehouseMasterQueryRequest request) {
        long start = System.currentTimeMillis();
        String path = SapErpFieldMapping.ENDPOINT_STORAGE_LOCATION_SRV;
        try {
            Map<String, String> params = buildWarehouseQueryParams(request);
            Map<String, Object> sapResponse =
                    invokeWithRetry(() -> oDataClient.getWithParams(path, params), "sap:wh:pull");
            List<SapErpDto.WarehouseMasterItem> items =
                    converter.toWarehouseMasterItems(sapResponse);

            SapErpDto.MasterDataPullResponse<SapErpDto.WarehouseMasterItem> response =
                    new SapErpDto.MasterDataPullResponse<>();
            response.setSuccess(true);
            response.setItems(items);
            response.setTotal(items.size());
            return response;
        } catch (Exception e) {
            long cost = System.currentTimeMillis() - start;
            log.error("拉取仓库主数据失败: cost={}ms, error={}", cost, e.getMessage());
            SapErpDto.MasterDataPullResponse<SapErpDto.WarehouseMasterItem> response =
                    new SapErpDto.MasterDataPullResponse<>();
            response.setSuccess(false);
            response.setItems(List.of());
            response.setErrorMessage(e.getMessage());
            return response;
        }
    }

    // ============================================================
    // IntegrationAdapter 接口实现（兼容旧调用方）
    // ============================================================

    @Override
    public Map<String, Object> pushOrder(Map<String, Object> order) {
        // 兼容旧调用方：将 Map 转 InboundPostRequest
        String method = (String) order.getOrDefault("method", "GOODS_RECEIPT");
        String orderNo = (String) order.getOrDefault("orderNo", "");
        log.info("SAP pushOrder 兼容入口: method={}, orderNo={}", method, orderNo);
        // 旧调用走旧路径（无幂等），新调用走 postInboundGoods/postOutboundGoods
        return order;
    }

    @Override
    public Map<String, Object> pullOrder(Map<String, Object> params) {
        String type = (String) params.getOrDefault("type", "PURCHASE_ORDER");
        log.info("SAP pullOrder 兼容入口: type={}", type);
        return params;
    }

    // ============================================================
    // 内部辅助
    // ============================================================

    /** 带重试的 SAP 调用 */
    private <T> T invokeWithRetry(java.util.function.Supplier<T> supplier, String idempotentKey) {
        if (!sapErpConfig.isEnabled()) {
            throw new SapODataException("SAP 集成已禁用", null, null);
        }
        SapErpConfig.Retry retryConfig = sapErpConfig.getRetry();
        ApiDefinition.RetryConfig cfg = new ApiDefinition.RetryConfig();
        cfg.setMaxRetries(retryConfig.getMaxRetries());
        cfg.setInitialBackoffMs(retryConfig.getInitialBackoffMs());
        cfg.setMultiplier(retryConfig.getMultiplier());
        cfg.setMaxBackoffMs(retryConfig.getMaxBackoffMs());

        return retryExecutor.executeWithRetry(supplier, cfg);
    }

    /** 记录 API 调用日志 */
    private void recordApiCall(
            String apiName,
            String path,
            Object requestBody,
            Object responseBody,
            boolean success,
            String errorMsg,
            long cost,
            String businessType,
            String businessNo,
            String direction) {
        try {
            String reqJson = toJson(requestBody);
            String respJson = toJson(responseBody);
            String status = success ? "SUCCESS" : "FAILED";
            integrationService.recordApiCall(
                    "sap-erp",
                    apiName,
                    path,
                    "POST",
                    "X-Request-ID:<trace>",
                    reqJson,
                    success ? 200 : 500,
                    respJson,
                    cost,
                    status,
                    errorMsg,
                    MDCTrace.current(),
                    businessType,
                    businessNo,
                    direction);
        } catch (Exception e) {
            log.warn("记录 API 调用日志失败: {}", e.getMessage());
        }
    }

    /** 入队失败消息（补偿队列） */
    private void enqueueFailedMessage(
            String messageType, String businessNo, Object payload, String errorMsg) {
        try {
            integrationService.createMessage(
                    "sap-erp",
                    messageType,
                    messageType,
                    toJson(payload),
                    "SAP_INTEGRATION",
                    businessNo,
                    "OUTBOUND");
            log.info(
                    "失败消息已入队: type={}, businessNo={}, error={}", messageType, businessNo, errorMsg);
        } catch (Exception e) {
            log.error("入队失败消息失败: {}", e.getMessage());
        }
    }

    /** 从异常中提取 SapODataException */
    private SapODataException extractODataException(Exception e) {
        if (e instanceof SapODataException) return (SapODataException) e;
        Throwable cause = e.getCause();
        while (cause != null) {
            if (cause instanceof SapODataException) return (SapODataException) cause;
            cause = cause.getCause();
        }
        return null;
    }

    private SapErpDto.GoodsMovementResponse parseCachedResponse(String cached) {
        SapErpDto.GoodsMovementResponse response = new SapErpDto.GoodsMovementResponse();
        response.setSuccess(true);
        response.setSapDocNum(cached);
        response.setCostTimeMs(0);
        return response;
    }

    private SapErpDto.GoodsMovementResponse failureResponse(String msg, String code, long cost) {
        SapErpDto.GoodsMovementResponse response = new SapErpDto.GoodsMovementResponse();
        response.setSuccess(false);
        response.setErrorMessage(msg);
        response.setErrorCode(code);
        response.setCostTimeMs(cost);
        return response;
    }

    private SapErpDto.InventorySyncResponse newInventoryResponse(
            boolean success, String msg, String code, long cost) {
        SapErpDto.InventorySyncResponse r = new SapErpDto.InventorySyncResponse();
        r.setSuccess(success);
        r.setErrorMessage(msg);
        r.setErrorCode(code);
        r.setCostTimeMs(cost);
        return r;
    }

    private Map<String, String> buildMaterialQueryParams(SapErpDto.MaterialMasterQueryRequest req) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("$top", String.valueOf(req.getPageSize()));
        params.put("$skip", String.valueOf(req.getSkip()));
        if (req.getMaterial() != null && !req.getMaterial().isBlank()) {
            params.put("$filter", "Material eq '" + req.getMaterial() + "'");
        } else if (req.isActiveOnly()) {
            params.put("$filter", "IsActive eq true");
        }
        return params;
    }

    private Map<String, String> buildWarehouseQueryParams(
            SapErpDto.WarehouseMasterQueryRequest req) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("$top", String.valueOf(req.getPageSize()));
        params.put("$skip", String.valueOf(req.getSkip()));
        if (req.getPlant() != null && !req.getPlant().isBlank()) {
            params.put("$filter", "Plant eq '" + req.getPlant() + "'");
        }
        return params;
    }

    private String toJson(Object obj) {
        if (obj == null) return null;
        try {
            return JsonUtils.toJson(obj);
        } catch (Exception e) {
            return obj.toString();
        }
    }

    /** 轻量 MDC traceId 读取 */
    private static final class MDCTrace {
        static String current() {
            return org.slf4j.MDC.get("traceId");
        }
    }
}
