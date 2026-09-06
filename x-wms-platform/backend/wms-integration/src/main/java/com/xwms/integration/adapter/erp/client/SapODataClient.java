package com.xwms.integration.adapter.erp.client;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import com.xwms.integration.adapter.erp.config.SapErpConfig;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * SAP OData V2 HTTP 客户端
 *
 * <p>封装与 SAP S/4HANA Gateway 的底层交互，承担：
 *
 * <ul>
 *   <li>Basic Auth 头构造（账号密码来自 {@link SapErpConfig}，禁止硬编码）
 *   <li>SAP CSRF Token 预取与写回（写操作必需）
 *   <li>超时与连接池管理（超时参数 Nacos 外置）
 *   <li>请求 traceId 透传（MDC 与 {@code X-Request-ID} 双写）
 *   <li>错误分类与异常语义化（区分 4xx 业务错误 / 5xx 服务端错误 / 网络超时）
 * </ul>
 *
 * <p>本类只负责"HTTP 层"，不做幂等/重试/日志落库——这些由 {@code SapErpAdapter} 编排。 分离后便于单元测试（用 {@code
 * MockRestServiceServer} 覆盖）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SapODataClient {

    private final SapErpConfig sapErpConfig;

    /** 自定义 RestTemplate（使用 Nacos 配置的超时） */
    private final RestTemplate restTemplate = buildRestTemplate();

    /**
     * 构造带超时配置的 RestTemplate。 注：RestTemplate 的超时由 {@link SimpleClientHttpRequestFactory} 决定，与 Nacos
     * 刷新解耦—— 如需运行时刷新超时，可在此处改用 OpenFeign 或懒加载重建。
     */
    private static RestTemplate buildRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        // 默认值兜底，实际由 SapErpConfig 注入后覆盖
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(30000);
        return new RestTemplate(factory);
    }

    /** 供测试注入：允许外部替换超时参数 */
    public void refreshTimeout() {
        SimpleClientHttpRequestFactory factory =
                (SimpleClientHttpRequestFactory) restTemplate.getRequestFactory();
        factory.setConnectTimeout(sapErpConfig.getTimeout().getConnect());
        factory.setReadTimeout(sapErpConfig.getTimeout().getRead());
    }

    // ============================================================
    // 对外方法
    // ============================================================

    /**
     * 带 CSRF Token 的 POST 调用（用于所有写操作：过账、库存同步）。
     *
     * @param path 相对路径（以 / 开头）
     * @param payload 请求体
     * @return 响应体（JSON Map）
     * @throws SapODataException SAP 返回错误（4xx/5xx）或网络错误时抛出
     */
    public Map<String, Object> postWithCsrf(String path, Object payload) {
        String url = sapErpConfig.getBaseUrl() + path;
        String traceId = resolveTraceId();

        // 1. 预取 CSRF Token（写操作必需）
        HttpHeaders csrfHeaders = buildHeaders(traceId, true);
        ResponseEntity<Map> csrfResponse = executeGet(url + "/~change", csrfHeaders);
        String csrfToken = extractCsrfToken(csrfResponse);
        if (csrfToken == null || csrfToken.isEmpty()) {
            throw new SapODataException("无法获取 SAP CSRF Token: " + path, null, null);
        }

        // 2. 写请求携带 CSRF Token
        HttpHeaders writeHeaders = buildHeaders(traceId, false);
        writeHeaders.set("X-CSRF-Token", csrfToken);
        writeHeaders.set("X-Requested-With", "XMLHttpRequest");
        writeHeaders.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Object> entity = new HttpEntity<>(payload, writeHeaders);
        ResponseEntity<Map> response = exchange(url, HttpMethod.POST, entity, traceId);
        return response.getBody();
    }

    /**
     * 普通 GET 调用（用于主数据拉取）。
     *
     * @param path 相对路径
     * @param params 查询参数（如 $top / $skip / $filter），可为空
     * @return 响应体
     */
    public Map<String, Object> getWithParams(String path, Map<String, String> params) {
        String url = buildUrlWithParams(sapErpConfig.getBaseUrl() + path, params);
        HttpHeaders headers = buildHeaders(resolveTraceId(), false);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<Map> response = executeGet(url, headers);
        return response.getBody();
    }

    // ============================================================
    // 内部实现
    // ============================================================

    private ResponseEntity<Map> executeGet(String url, HttpHeaders headers) {
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        return exchange(url, HttpMethod.GET, entity, resolveTraceId());
    }

    private ResponseEntity<Map> exchange(
            String url, HttpMethod method, HttpEntity<?> entity, String traceId) {
        long start = System.currentTimeMillis();
        log.debug("SAP OData 请求: {} {} traceId={}", method, url, traceId);
        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, method, entity, Map.class);
            long cost = System.currentTimeMillis() - start;
            log.info(
                    "SAP OData 响应: {} {} status={} cost={}ms",
                    method,
                    url,
                    response.getStatusCode(),
                    cost);
            return response;
        } catch (HttpClientErrorException e) {
            // 4xx 业务/认证错误：不重试，直接抛
            long cost = System.currentTimeMillis() - start;
            log.warn(
                    "SAP OData 业务错误: {} {} status={} body={} cost={}ms",
                    method,
                    url,
                    e.getStatusCode(),
                    e.getResponseBodyAsString(),
                    cost);
            throw new SapODataException(
                    "SAP 返回业务错误: HTTP " + e.getStatusCode(),
                    e.getStatusCode().value(),
                    e.getResponseBodyAsString());
        } catch (HttpServerErrorException e) {
            // 5xx 服务端错误：可重试
            long cost = System.currentTimeMillis() - start;
            log.error(
                    "SAP OData 服务端错误: {} {} status={} body={} cost={}ms",
                    method,
                    url,
                    e.getStatusCode(),
                    e.getResponseBodyAsString(),
                    cost);
            throw new SapODataException(
                    "SAP 服务端错误: HTTP " + e.getStatusCode(),
                    e.getStatusCode().value(),
                    e.getResponseBodyAsString());
        } catch (ResourceAccessException e) {
            // 网络错误（超时/连接拒绝）：可重试
            long cost = System.currentTimeMillis() - start;
            log.error(
                    "SAP OData 网络错误: {} {} error={} cost={}ms", method, url, e.getMessage(), cost);
            throw new SapODataException("SAP 网络错误: " + e.getMessage(), null, e.getMessage());
        }
    }

    /** 构造通用请求头 */
    private HttpHeaders buildHeaders(String traceId, boolean fetchCsrf) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        // Basic Auth
        String auth = sapErpConfig.getUsername() + ":" + sapErpConfig.getPassword();
        headers.set(
                "Authorization",
                "Basic "
                        + Base64.getEncoder()
                                .encodeToString(auth.getBytes(StandardCharsets.UTF_8)));

        // Trace
        if (traceId != null) {
            headers.set("X-Request-ID", traceId);
        }

        // CSRF（预取模式）
        if (fetchCsrf) {
            headers.set("X-CSRF-Token", "Fetch");
            headers.set("X-Requested-With", "XMLHttpRequest");
        }

        // SAP 要求：OData V2 默认 JSON 格式
        headers.set("Prefer", "odata.version=4.0");
        return headers;
    }

    /** 从响应头中提取 CSRF Token */
    private String extractCsrfToken(ResponseEntity<Map> response) {
        List<String> tokens = response.getHeaders().get("X-CSRF-Token");
        if (tokens == null || tokens.isEmpty()) {
            return null;
        }
        return tokens.get(0);
    }

    /** 解析 traceId：优先 MDC，否则生成 UUID */
    private String resolveTraceId() {
        String traceId = MDC.get("traceId");
        if (traceId == null || traceId.isEmpty()) {
            traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
            MDC.put("traceId", traceId);
        }
        return traceId;
    }

    /** URL 参数拼接（仅 GET 用，POST 用 body） */
    private String buildUrlWithParams(String baseUrl, Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return baseUrl;
        }
        StringBuilder sb = new StringBuilder(baseUrl);
        sb.append(baseUrl.contains("?") ? "&" : "?");
        boolean first = true;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (!first) {
                sb.append("&");
            }
            sb.append(entry.getKey()).append("=").append(entry.getValue());
            first = false;
        }
        return sb.toString();
    }

    // ============================================================
    // 异常类型
    // ============================================================

    /**
     * SAP 调用异常
     *
     * <p>区分：
     *
     * <ul>
     *   <li>{@code httpStatus == null} — 网络错误（可重试）
     *   <li>{@code httpStatus >= 500} — 服务端错误（可重试）
     *   <li>{@code 400 <= httpStatus < 500} — 业务/认证错误（不可重试）
     * </ul>
     */
    public static class SapODataException extends RuntimeException {
        private final Integer httpStatus;
        private final String responseBody;

        public SapODataException(String message, Integer httpStatus, String responseBody) {
            super(message);
            this.httpStatus = httpStatus;
            this.responseBody = responseBody;
        }

        public Integer getHttpStatus() {
            return httpStatus;
        }

        public String getResponseBody() {
            return responseBody;
        }

        public boolean isRetryable() {
            if (httpStatus == null) return true; // 网络错误
            return httpStatus >= 500; // 5xx 可重试，4xx 不可
        }
    }
}
