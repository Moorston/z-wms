package com.xwms.integration.api.controller;

import java.time.LocalDateTime;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import com.xwms.integration.api.alert.ApiAlertService;
import com.xwms.integration.api.async.ApiAsyncService;
import com.xwms.integration.api.log.ApiCallLogService;
import com.xwms.integration.api.monitor.ApiMonitorService;
import com.xwms.integration.api.registry.ApiRegistry;
import com.xwms.integration.api.service.ApiPlatformService;
import com.xwms.integration.core.model.ApiDefinition;
import com.xwms.integration.core.model.ApiRequest;
import com.xwms.integration.core.model.ApiResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * API平台统一入口控制器 所有外部系统调用都通过此入口，由ApiPlatformService编排 支持通配符路径：/api/external/**
 *
 * <p>管理接口： - /admin/apis API列表/注册/注销/启用禁用 - /admin/monitor 监控统计 - /admin/alert 告警记录 - /admin/log
 * 调用日志查询 - /admin/async/{id} 异步任务查询
 */
@Slf4j
@RestController
@RequestMapping("/api/external")
@RequiredArgsConstructor
public class ApiGatewayController {

    private final ApiPlatformService platformService;
    private final ApiRegistry registry;
    private final ApiMonitorService monitorService;
    private final ApiAlertService alertService;
    private final ApiCallLogService callLogService;
    private final ApiAsyncService asyncService;

    // ==================== 通用API入口 ====================

    @PostMapping("/**")
    public ApiResponse handlePost(
            @RequestBody(required = false) Object body, HttpServletRequest request) {
        return invoke("POST", extractPath(request), body, request);
    }

    @GetMapping("/**")
    public ApiResponse handleGet(
            @RequestParam Map<String, Object> params, HttpServletRequest request) {
        ApiRequest req = buildRequest("GET", extractPath(request), null, params, request);
        return platformService.invoke(req);
    }

    @PutMapping("/**")
    public ApiResponse handlePut(
            @RequestBody(required = false) Object body, HttpServletRequest request) {
        return invoke("PUT", extractPath(request), body, request);
    }

    @DeleteMapping("/**")
    public ApiResponse handleDelete(HttpServletRequest request) {
        ApiRequest req = buildRequest("DELETE", extractPath(request), null, Map.of(), request);
        return platformService.invoke(req);
    }

    private ApiResponse invoke(
            String method, String path, Object body, HttpServletRequest request) {
        ApiRequest req = buildRequest(method, path, body, Map.of(), request);
        return platformService.invoke(req);
    }

    private ApiRequest buildRequest(
            String method,
            String path,
            Object body,
            Map<String, Object> params,
            HttpServletRequest request) {
        ApiRequest req = new ApiRequest();
        req.setMethod(method);
        req.setPath(path);
        req.setBody(body);
        req.setParams(params);
        req.setClientIp(request.getRemoteAddr());
        req.setAppId(request.getHeader("X-App-Id"));
        var auth = new ApiRequest.AuthInfo();
        auth.setApiKey(request.getHeader("X-API-Key"));
        auth.setSignature(request.getHeader("X-Signature"));
        auth.setTimestamp(request.getHeader("X-Timestamp"));
        auth.setNonce(request.getHeader("X-Nonce"));
        auth.setToken(request.getHeader("Authorization"));
        req.setAuthInfo(auth);
        return req;
    }

    private String extractPath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.substring("/api/external".length());
    }

    // ==================== API管理接口 ====================

    @GetMapping("/admin/apis")
    public ApiResponse listApis() {
        return ApiResponse.success(registry.getAll());
    }

    @GetMapping("/admin/apis/{apiId}")
    public ApiResponse getApi(@PathVariable String apiId) {
        return ApiResponse.success(registry.getById(apiId));
    }

    @PostMapping("/admin/apis/register")
    public ApiResponse registerApi(@RequestBody ApiDefinition definition) {
        registry.register(definition);
        return ApiResponse.success("API注册成功: " + definition.getApiId());
    }

    @DeleteMapping("/admin/apis/{apiId}")
    public ApiResponse unregisterApi(@PathVariable String apiId) {
        registry.unregister(apiId);
        return ApiResponse.success("API注销成功: " + apiId);
    }

    @PutMapping("/admin/apis/{apiId}/status")
    public ApiResponse updateApiStatus(@PathVariable String apiId, @RequestParam String status) {
        ApiDefinition def = registry.getById(apiId);
        if (def == null) return ApiResponse.error(404, "API不存在");
        def.setStatus(status);
        registry.register(def);
        return ApiResponse.success("API状态更新: " + apiId + " -> " + status);
    }

    // ==================== 监控接口 ====================

    @GetMapping("/admin/monitor")
    public ApiResponse getMonitor() {
        return ApiResponse.success(monitorService.getAllStats());
    }

    @GetMapping("/admin/monitor/{apiId}")
    public ApiResponse getApiMonitor(@PathVariable String apiId) {
        return ApiResponse.success(monitorService.getStats(apiId));
    }

    @PostMapping("/admin/monitor/reset")
    public ApiResponse resetMonitor() {
        monitorService.resetStats();
        return ApiResponse.success("监控统计已重置");
    }

    // ==================== 告警接口 ====================

    @GetMapping("/admin/alert")
    public ApiResponse getAlerts(@RequestParam(defaultValue = "50") int limit) {
        return ApiResponse.success(alertService.getAlertHistory(limit));
    }

    // ==================== 调用日志接口 ====================

    @GetMapping("/admin/log")
    public ApiResponse queryLogs(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            @RequestParam(required = false) String apiId,
            @RequestParam(required = false) String appId,
            @RequestParam(required = false) Boolean success,
            @RequestParam(defaultValue = "100") int limit) {
        return ApiResponse.success(
                callLogService.queryLogs(start, end, apiId, appId, success, limit));
    }

    @GetMapping("/admin/log/slow")
    public ApiResponse querySlowLogs(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            @RequestParam(defaultValue = "5000") long thresholdMs,
            @RequestParam(defaultValue = "50") int limit) {
        return ApiResponse.success(callLogService.querySlowCalls(start, end, thresholdMs, limit));
    }

    @GetMapping("/admin/log/errors")
    public ApiResponse queryErrorStats(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return ApiResponse.success(callLogService.queryErrorStats(start, end));
    }

    // ==================== 异步任务接口 ====================

    @GetMapping("/admin/async/{requestId}")
    public ApiResponse getAsyncTask(@PathVariable String requestId) {
        ApiAsyncService.AsyncTask task = asyncService.getTask(requestId);
        if (task == null) return ApiResponse.error(404, "异步任务不存在: " + requestId);
        return ApiResponse.success(task);
    }
}
