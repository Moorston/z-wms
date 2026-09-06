package com.xwms.integration.api;

import com.xwms.integration.api.auth.ApiAuthService;
import com.xwms.integration.api.idempotent.ApiIdempotentService;
import com.xwms.integration.api.monitor.ApiMonitorService;
import com.xwms.integration.api.ratelimit.ApiRateLimiter;
import com.xwms.integration.api.registry.ApiRegistry;
import com.xwms.integration.api.service.ApiPlatformService;
import com.xwms.integration.api.transform.DataTransformEngine;
import com.xwms.integration.core.circuitbreaker.CircuitBreakerService;
import com.xwms.integration.core.model.ApiDefinition;
import com.xwms.integration.core.model.ApiRequest;
import com.xwms.integration.core.model.ApiResponse;
import com.xwms.integration.core.retry.RetryExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationContext;
import org.springframework.kafka.core.KafkaTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * API平台服务单元测试
 * 核心测试：API不存在/鉴权失败/限流/幂等/熔断/正常调用
 */
class ApiPlatformServiceTest {

    @Mock private ApiRegistry registry;
    @Mock private ApiAuthService authService;
    @Mock private ApiRateLimiter rateLimiter;
    @Mock private ApiIdempotentService idempotentService;
    @Mock private CircuitBreakerService circuitBreaker;
    @Mock private DataTransformEngine transformEngine;
    @Mock private RetryExecutor retryExecutor;
    @Mock private ApiMonitorService monitorService;
    @Mock private ApplicationContext applicationContext;
    @Mock private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private ApiPlatformService platformService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testInvoke_apiNotFound() {
        when(registry.get(anyString(), anyString())).thenReturn(null);
        ApiRequest req = createRequest();
        ApiResponse resp = platformService.invoke(req);
        assertEquals(404, resp.getCode());
    }

    @Test
    void testInvoke_apiDisabled() {
        ApiDefinition def = new ApiDefinition();
        def.setStatus("DISABLED");
        when(registry.get(anyString(), anyString())).thenReturn(def);

        ApiResponse resp = platformService.invoke(createRequest());
        assertEquals(403, resp.getCode());
    }

    @Test
    void testInvoke_authFailed() {
        ApiDefinition def = new ApiDefinition();
        def.setStatus("ENABLED");
        def.setAuthType("API_KEY");
        when(registry.get(anyString(), anyString())).thenReturn(def);
        when(authService.authenticate(any(), anyString())).thenReturn(false);

        ApiResponse resp = platformService.invoke(createRequest());
        assertEquals(401, resp.getCode());
    }

    @Test
    void testInvoke_rateLimited() {
        ApiDefinition def = createEnabledDef();
        def.setRateLimit(new ApiDefinition.RateLimitConfig());
        when(registry.get(anyString(), anyString())).thenReturn(def);
        when(authService.authenticate(any(), anyString())).thenReturn(true);
        when(rateLimiter.tryAcquire(anyString(), anyInt(), anyInt(), anyString())).thenReturn(false);

        ApiResponse resp = platformService.invoke(createRequest());
        assertEquals(429, resp.getCode());
    }

    @Test
    void testInvoke_circuitOpen() {
        ApiDefinition def = createEnabledDef();
        def.setAdapterId("test-adapter");
        when(registry.get(anyString(), anyString())).thenReturn(def);
        when(authService.authenticate(any(), anyString())).thenReturn(true);
        when(idempotentService.tryAcquire(anyString())).thenReturn(true);
        when(circuitBreaker.allowRequest(anyString())).thenReturn(false);

        ApiResponse resp = platformService.invoke(createRequest());
        assertEquals(503, resp.getCode());
    }

    @Test
    void testInvoke_async() {
        ApiDefinition def = createEnabledDef();
        def.setAsync(true);
        def.setAdapterId("test-adapter");
        when(registry.get(anyString(), anyString())).thenReturn(def);
        when(authService.authenticate(any(), anyString())).thenReturn(true);
        when(idempotentService.tryAcquire(anyString())).thenReturn(true);
        when(circuitBreaker.allowRequest(anyString())).thenReturn(true);
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(null);

        ApiResponse resp = platformService.invoke(createRequest());
        assertEquals(200, resp.getCode());
        assertNotNull(resp.getRequestId());
    }

    private ApiDefinition createEnabledDef() {
        ApiDefinition def = new ApiDefinition();
        def.setStatus("ENABLED");
        def.setAuthType("NONE");
        def.setApiId("test-api");
        return def;
    }

    private ApiRequest createRequest() {
        ApiRequest req = new ApiRequest();
        req.setPath("/test");
        req.setMethod("POST");
        req.setAppId("test-app");
        return req;
    }
}
