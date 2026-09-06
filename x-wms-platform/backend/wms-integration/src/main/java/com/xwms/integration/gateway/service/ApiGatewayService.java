package com.xwms.integration.gateway.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.integration.gateway.entity.*;
import com.xwms.integration.gateway.mapper.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** API网关与平台核心服务 包含: API定义/API密钥/API限流/API调用日志 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApiGatewayService {

    private final ApiDefinitionMapper apiDefinitionMapper;
    private final ApiCallLogMapper apiCallLogMapper;
    private final ApiKeyMapper apiKeyMapper;
    private final ApiRateLimitMapper apiRateLimitMapper;

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final AtomicInteger LOG_SEQ = new AtomicInteger(0);
    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    // ============================================================
    // 1. API定义管理
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public ApiDefinition createApi(ApiDefinition api) {
        if (api.getAuthRequired() == null) api.setAuthRequired(1);
        if (api.getRateLimit() == null) api.setRateLimit(100);
        if (api.getTimeout() == null) api.setTimeout(30);
        if (api.getEnabled() == null) api.setEnabled(1);
        if (api.getVersion() == null) api.setVersion("v1");
        apiDefinitionMapper.insert(api);
        log.info("创建API: {} {}", api.getApiMethod(), api.getApiPath());
        return api;
    }

    @Transactional(rollbackFor = Exception.class)
    public ApiDefinition updateApi(ApiDefinition api) {
        apiDefinitionMapper.updateById(api);
        return api;
    }

    public Page<ApiDefinition> pageApis(
            Page<ApiDefinition> page, String apiCategory, String apiMethod, Integer enabled) {
        LambdaQueryWrapper<ApiDefinition> wrapper = new LambdaQueryWrapper<>();
        if (apiCategory != null) wrapper.eq(ApiDefinition::getApiCategory, apiCategory);
        if (apiMethod != null) wrapper.eq(ApiDefinition::getApiMethod, apiMethod);
        if (enabled != null) wrapper.eq(ApiDefinition::getEnabled, enabled);
        wrapper.orderByAsc(ApiDefinition::getApiCategory).orderByAsc(ApiDefinition::getApiCode);
        return apiDefinitionMapper.selectPage(page, wrapper);
    }

    public List<ApiDefinition> getApisByCategory(String category) {
        return apiDefinitionMapper.selectByCategory(category);
    }

    public ApiDefinition getApiByCode(String apiCode) {
        return apiDefinitionMapper.selectByCode(apiCode);
    }

    public ApiDefinition getApiByPathAndMethod(String path, String method) {
        return apiDefinitionMapper.selectByPathAndMethod(path, method);
    }

    /** 启用/禁用API */
    @Transactional(rollbackFor = Exception.class)
    public ApiDefinition toggleApi(String apiCode, boolean enabled) {
        ApiDefinition api = apiDefinitionMapper.selectByCode(apiCode);
        if (api == null) throw new RuntimeException("API不存在: " + apiCode);
        api.setEnabled(enabled ? 1 : 0);
        apiDefinitionMapper.updateById(api);
        log.info("API{}: {}", enabled ? "启用" : "禁用", apiCode);
        return api;
    }

    // ============================================================
    // 2. API密钥管理
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public ApiKey createApiKey(ApiKey apiKey) {
        if (apiKey.getAppKey() == null) {
            apiKey.setAppKey(generateAppKey());
        }
        if (apiKey.getAppSecret() == null) {
            apiKey.setAppSecret(generateAppSecret());
        }
        if (apiKey.getStatus() == null) apiKey.setStatus("ACTIVE");
        if (apiKey.getRateLimit() == null) apiKey.setRateLimit(100);
        apiKeyMapper.insert(apiKey);
        log.info("创建API密钥: {}={}", apiKey.getAppKey(), apiKey.getAppName());
        return apiKey;
    }

    @Transactional(rollbackFor = Exception.class)
    public ApiKey updateApiKey(ApiKey apiKey) {
        apiKeyMapper.updateById(apiKey);
        return apiKey;
    }

    public Page<ApiKey> pageApiKeys(Page<ApiKey> page, String appType, String status) {
        LambdaQueryWrapper<ApiKey> wrapper = new LambdaQueryWrapper<>();
        if (appType != null) wrapper.eq(ApiKey::getAppType, appType);
        if (status != null) wrapper.eq(ApiKey::getStatus, status);
        wrapper.orderByAsc(ApiKey::getAppName);
        return apiKeyMapper.selectPage(page, wrapper);
    }

    public ApiKey getApiKeyByAppKey(String appKey) {
        return apiKeyMapper.selectByAppKey(appKey);
    }

    public List<ApiKey> getActiveApiKeys() {
        return apiKeyMapper.selectActiveKeys();
    }

    /** 重置密钥 */
    @Transactional(rollbackFor = Exception.class)
    public ApiKey resetSecret(String appKey) {
        ApiKey apiKey = apiKeyMapper.selectByAppKey(appKey);
        if (apiKey == null) throw new RuntimeException("API密钥不存在: " + appKey);
        apiKey.setAppSecret(generateAppSecret());
        apiKeyMapper.updateById(apiKey);
        log.info("重置API密钥: {}", appKey);
        return apiKey;
    }

    /** 启用/禁用密钥 */
    @Transactional(rollbackFor = Exception.class)
    public ApiKey toggleApiKey(String appKey, boolean enabled) {
        ApiKey apiKey = apiKeyMapper.selectByAppKey(appKey);
        if (apiKey == null) throw new RuntimeException("API密钥不存在: " + appKey);
        apiKey.setStatus(enabled ? "ACTIVE" : "DISABLED");
        apiKeyMapper.updateById(apiKey);
        log.info("API密钥{}: {}", enabled ? "启用" : "禁用", appKey);
        return apiKey;
    }

    // ============================================================
    // 3. API限流配置
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public ApiRateLimit createRateLimit(ApiRateLimit rateLimit) {
        if (rateLimit.getLimitQps() == null) rateLimit.setLimitQps(100);
        if (rateLimit.getBurstSize() == null) rateLimit.setBurstSize(10);
        if (rateLimit.getWindowType() == null) rateLimit.setWindowType("SLIDING");
        if (rateLimit.getEnabled() == null) rateLimit.setEnabled(1);
        apiRateLimitMapper.insert(rateLimit);
        log.info("创建限流配置: {}={}", rateLimit.getTargetType(), rateLimit.getTargetValue());
        return rateLimit;
    }

    @Transactional(rollbackFor = Exception.class)
    public ApiRateLimit updateRateLimit(ApiRateLimit rateLimit) {
        apiRateLimitMapper.updateById(rateLimit);
        return rateLimit;
    }

    public Page<ApiRateLimit> pageRateLimits(
            Page<ApiRateLimit> page, String targetType, Integer enabled) {
        LambdaQueryWrapper<ApiRateLimit> wrapper = new LambdaQueryWrapper<>();
        if (targetType != null) wrapper.eq(ApiRateLimit::getTargetType, targetType);
        if (enabled != null) wrapper.eq(ApiRateLimit::getEnabled, enabled);
        wrapper.orderByAsc(ApiRateLimit::getTargetType).orderByAsc(ApiRateLimit::getTargetValue);
        return apiRateLimitMapper.selectPage(page, wrapper);
    }

    public ApiRateLimit getRateLimitByTarget(String targetType, String targetValue) {
        return apiRateLimitMapper.selectByTarget(targetType, targetValue);
    }

    public List<ApiRateLimit> getAllEnabledRateLimits() {
        return apiRateLimitMapper.selectAllEnabled();
    }

    // ============================================================
    // 4. API调用日志
    // ============================================================

    /** 记录调用日志 */
    @Transactional(rollbackFor = Exception.class)
    public ApiCallLog recordCallLog(
            String apiCode,
            String apiPath,
            String apiMethod,
            String appKey,
            String requestIp,
            String requestHeaders,
            String requestBody,
            Integer responseStatus,
            String responseBody,
            Long durationMs,
            String callStatus,
            String errorMsg,
            String traceId) {
        ApiCallLog logEntry = new ApiCallLog();
        logEntry.setLogNo(generateLogNo());
        logEntry.setApiCode(apiCode);
        logEntry.setApiPath(apiPath);
        logEntry.setApiMethod(apiMethod);
        logEntry.setAppKey(appKey);
        logEntry.setRequestIp(requestIp);
        logEntry.setRequestHeaders(requestHeaders);
        logEntry.setRequestBody(requestBody);
        logEntry.setResponseStatus(responseStatus);
        logEntry.setResponseBody(responseBody);
        logEntry.setDurationMs(durationMs);
        logEntry.setCallStatus(callStatus);
        logEntry.setErrorMsg(errorMsg);
        logEntry.setTraceId(traceId);
        logEntry.setCallTime(LocalDateTime.now());
        apiCallLogMapper.insert(logEntry);
        return logEntry;
    }

    public Page<ApiCallLog> pageCallLogs(
            Page<ApiCallLog> page,
            String apiCode,
            String appKey,
            String callStatus,
            LocalDateTime startTime,
            LocalDateTime endTime) {
        LambdaQueryWrapper<ApiCallLog> wrapper = new LambdaQueryWrapper<>();
        if (apiCode != null) wrapper.eq(ApiCallLog::getApiCode, apiCode);
        if (appKey != null) wrapper.eq(ApiCallLog::getAppKey, appKey);
        if (callStatus != null) wrapper.eq(ApiCallLog::getCallStatus, callStatus);
        if (startTime != null) wrapper.ge(ApiCallLog::getCallTime, startTime);
        if (endTime != null) wrapper.le(ApiCallLog::getCallTime, endTime);
        wrapper.orderByDesc(ApiCallLog::getCallTime);
        return apiCallLogMapper.selectPage(page, wrapper);
    }

    public List<ApiCallLog> getCallLogsByApi(String apiCode) {
        return apiCallLogMapper.selectByApiCode(apiCode);
    }

    public List<ApiCallLog> getCallLogsByAppKey(String appKey) {
        return apiCallLogMapper.selectByAppKey(appKey);
    }

    // ============================================================
    // 工具方法
    // ============================================================

    private String generateAppKey() {
        byte[] bytes = new byte[16];
        RANDOM.nextBytes(bytes);
        return "AK"
                + Base64.getUrlEncoder()
                        .withoutPadding()
                        .encodeToString(bytes)
                        .substring(0, 20)
                        .toUpperCase();
    }

    private String generateAppSecret() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String generateLogNo() {
        return "APILOG"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", LOG_SEQ.incrementAndGet() % 1000);
    }
}
