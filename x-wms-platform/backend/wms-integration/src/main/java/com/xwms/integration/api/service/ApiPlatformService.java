package com.xwms.integration.api.service;

import java.util.Map;
import java.util.UUID;

import org.springframework.context.ApplicationContext;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.xwms.integration.adapter.IntegrationAdapter;
import com.xwms.integration.api.auth.ApiAuthService;
import com.xwms.integration.api.idempotent.ApiIdempotentService;
import com.xwms.integration.api.monitor.ApiMonitorService;
import com.xwms.integration.api.ratelimit.ApiRateLimiter;
import com.xwms.integration.api.registry.ApiRegistry;
import com.xwms.integration.api.transform.DataTransformEngine;
import com.xwms.integration.core.circuitbreaker.CircuitBreakerService;
import com.xwms.integration.core.model.ApiDefinition;
import com.xwms.integration.core.model.ApiRequest;
import com.xwms.integration.core.model.ApiResponse;
import com.xwms.integration.core.retry.RetryExecutor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** API平台编排服务 统一调度：鉴权→限流→幂等→熔断→转换→适配器调用→重试→监控 这是API平台的核心大脑 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApiPlatformService {

    private final ApiRegistry registry;
    private final ApiAuthService authService;
    private final ApiRateLimiter rateLimiter;
    private final ApiIdempotentService idempotentService;
    private final CircuitBreakerService circuitBreaker;
    private final DataTransformEngine transformEngine;
    private final RetryExecutor retryExecutor;
    private final ApiMonitorService monitorService;
    private final ApplicationContext applicationContext;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    /** 同步API调用（核心流程） */
    public ApiResponse invoke(ApiRequest request) {
        long startTime = System.currentTimeMillis();
        String requestId =
                request.getRequestId() != null
                        ? request.getRequestId()
                        : UUID.randomUUID().toString();
        request.setRequestId(requestId);

        try {
            // 1. API注册检查
            ApiDefinition apiDef = registry.get(request.getPath(), request.getMethod());
            if (apiDef == null) {
                return ApiResponse.error(
                        404, "API不存在: " + request.getMethod() + " " + request.getPath());
            }
            if (!"ENABLED".equals(apiDef.getStatus())) {
                return ApiResponse.error(403, "API已禁用: " + apiDef.getApiName());
            }

            // 2. 鉴权
            if (!authService.authenticate(request, apiDef.getAuthType())) {
                monitorService.recordCall(apiDef.getApiId(), false, 0, "AUTH_FAILED");
                return ApiResponse.error(401, "鉴权失败");
            }

            // 3. 限流
            String rateLimitKey = request.getAppId() + ":" + apiDef.getApiId();
            var rl = apiDef.getRateLimit();
            if (rl != null
                    && !rateLimiter.tryAcquire(
                            rateLimitKey, rl.getQps(), rl.getBurst(), rl.getAlgorithm())) {
                monitorService.recordCall(apiDef.getApiId(), false, 0, "RATE_LIMITED");
                return ApiResponse.error(429, "请求过于频繁，请稍后重试");
            }

            // 4. 幂等检查
            String idempotentKey =
                    request.getAppId()
                            + ":"
                            + apiDef.getApiId()
                            + ":"
                            + (request.getBody() != null ? request.getBody().hashCode() : "none");
            if (!idempotentService.tryAcquire(idempotentKey)) {
                Object cached = idempotentService.getCachedResult(idempotentKey);
                if (cached != null) {
                    return ApiResponse.success(cached);
                }
                if (idempotentService.isProcessing(idempotentKey)) {
                    return ApiResponse.error(409, "请求处理中，请稍后查询结果");
                }
            }

            // 5. 熔断检查
            if (!circuitBreaker.allowRequest(apiDef.getAdapterId())) {
                monitorService.recordCall(apiDef.getApiId(), false, 0, "CIRCUIT_OPEN");
                return ApiResponse.error(503, "服务暂不可用（熔断器打开），请稍后重试");
            }

            // 6. 异步API走Kafka
            if (apiDef.isAsync()) {
                kafkaTemplate.send("wms-api-async", requestId, request);
                idempotentService.markSuccess(idempotentKey, "ACCEPTED");
                return ApiResponse.success(Map.of("requestId", requestId, "status", "ACCEPTED"));
            }

            // 7. 数据转换（请求）
            Object transformedBody = request.getBody();
            if (apiDef.getTransform() != null && !apiDef.getTransform().isUseDefault()) {
                transformedBody =
                        transformEngine.transformRequest(
                                request.getBody(), apiDef.getTransform().getRequestMapping());
            }
            final Object finalBody = transformedBody;

            // 8. 适配器调用（带重试）
            ApiResponse response =
                    retryExecutor.executeWithRetry(
                            () -> {
                                IntegrationAdapter adapter = getAdapter(apiDef.getAdapterId());
                                if (adapter == null) {
                                    throw new RuntimeException("适配器不存在: " + apiDef.getAdapterId());
                                }
                                Map<String, Object> result =
                                        adapter.pushOrder(
                                                Map.of(
                                                        "method",
                                                        apiDef.getAdapterMethod(),
                                                        "body",
                                                        finalBody));
                                return ApiResponse.success(result);
                            },
                            apiDef.getRetry());

            // 9. 成功处理
            circuitBreaker.recordSuccess(apiDef.getAdapterId());
            idempotentService.markSuccess(idempotentKey, response.getData());
            long duration = System.currentTimeMillis() - startTime;
            response.setDurationMs(duration);
            response.setRequestId(requestId);
            monitorService.recordCall(apiDef.getApiId(), true, duration, null);
            return response;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("API调用异常: path={}, error={}", request.getPath(), e.getMessage(), e);
            circuitBreaker.recordFailure(request.getPath());
            idempotentService.markFailed(request.getAppId() + ":" + request.getPath());
            monitorService.recordCall(request.getPath(), false, duration, e.getMessage());
            return ApiResponse.error(500, "API调用失败: " + e.getMessage());
        }
    }

    /** 获取适配器Bean */
    private IntegrationAdapter getAdapter(String adapterId) {
        Map<String, IntegrationAdapter> beans =
                applicationContext.getBeansOfType(IntegrationAdapter.class);
        return beans.values().stream()
                .filter(a -> adapterId.equals(a.getPluginId()))
                .findFirst()
                .orElse(null);
    }
}
