package com.xwms.integration.adapter;

import java.util.Map;

import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import lombok.extern.slf4j.Slf4j;

/** 适配器基类 提供通用的HTTP调用、签名、重试能力 所有具体适配器（ERP/快递/电商）继承此类 */
@Slf4j
public abstract class AbstractIntegrationAdapter implements IntegrationAdapter {

    protected final RestTemplate restTemplate = new RestTemplate();

    /** 适配器基础URL */
    protected abstract String getBaseUrl();

    /** 鉴权头（子类实现） */
    protected abstract HttpHeaders buildAuthHeaders();

    /** 通用POST调用 */
    protected Map<String, Object> doPost(String path, Object body) {
        String url = getBaseUrl() + path;
        HttpHeaders headers = buildAuthHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Object> entity = new HttpEntity<>(body, headers);
        log.info("适配器调用: {} POST {}, body={}", getPluginId(), url, body);
        try {
            ResponseEntity<Map> response =
                    restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
            log.info("适配器响应: {} status={}", getPluginId(), response.getStatusCode());
            return response.getBody();
        } catch (Exception e) {
            log.error("适配器调用失败: {} {}, error={}", getPluginId(), url, e.getMessage());
            throw new RuntimeException("适配器调用失败: " + e.getMessage(), e);
        }
    }

    /** 通用GET调用 */
    protected Map<String, Object> doGet(String path, Map<String, String> params) {
        StringBuilder url = new StringBuilder(getBaseUrl() + path);
        if (params != null && !params.isEmpty()) {
            url.append("?");
            params.forEach((k, v) -> url.append(k).append("=").append(v).append("&"));
        }
        HttpHeaders headers = buildAuthHeaders();
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        log.info("适配器调用: {} GET {}", getPluginId(), url);
        try {
            ResponseEntity<Map> response =
                    restTemplate.exchange(url.toString(), HttpMethod.GET, entity, Map.class);
            return response.getBody();
        } catch (Exception e) {
            log.error("适配器调用失败: {} {}, error={}", getPluginId(), url, e.getMessage());
            throw new RuntimeException("适配器调用失败: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean healthCheck() {
        try {
            doGet("/health", null);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
