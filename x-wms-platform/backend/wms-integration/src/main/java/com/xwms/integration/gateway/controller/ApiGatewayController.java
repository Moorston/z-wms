package com.xwms.integration.gateway.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.common.core.Result;
import com.xwms.integration.gateway.entity.*;
import com.xwms.integration.gateway.service.ApiGatewayService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/** API网关与平台 Controller */
@Tag(name = "API网关与平台", description = "API定义/密钥/限流/调用日志")
@RestController
@RequestMapping("/api/gateway")
@RequiredArgsConstructor
public class ApiGatewayController {

    private final ApiGatewayService apiGatewayService;

    // ============================================================
    // API定义
    // ============================================================

    @Operation(summary = "创建API")
    @PostMapping("/definition")
    public Result<ApiDefinition> createApi(@RequestBody ApiDefinition api) {
        return Result.success(apiGatewayService.createApi(api));
    }

    @Operation(summary = "更新API")
    @PutMapping("/definition")
    public Result<ApiDefinition> updateApi(@RequestBody ApiDefinition api) {
        return Result.success(apiGatewayService.updateApi(api));
    }

    @Operation(summary = "分页查询API")
    @GetMapping("/definition")
    public Result<Page<ApiDefinition>> pageApis(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String apiCategory,
            @RequestParam(required = false) String apiMethod,
            @RequestParam(required = false) Integer enabled) {
        return Result.success(
                apiGatewayService.pageApis(
                        new Page<>(page, size), apiCategory, apiMethod, enabled));
    }

    @Operation(summary = "按分类查询API")
    @GetMapping("/definition/category/{category}")
    public Result<List<ApiDefinition>> getApisByCategory(@PathVariable String category) {
        return Result.success(apiGatewayService.getApisByCategory(category));
    }

    @Operation(summary = "按编码查询API")
    @GetMapping("/definition/{code}")
    public Result<ApiDefinition> getApiByCode(@PathVariable String code) {
        return Result.success(apiGatewayService.getApiByCode(code));
    }

    @Operation(summary = "启用/禁用API")
    @PutMapping("/definition/{code}/toggle")
    public Result<ApiDefinition> toggleApi(
            @PathVariable String code, @RequestParam boolean enabled) {
        return Result.success(apiGatewayService.toggleApi(code, enabled));
    }

    // ============================================================
    // API密钥
    // ============================================================

    @Operation(summary = "创建API密钥")
    @PostMapping("/key")
    public Result<ApiKey> createApiKey(@RequestBody ApiKey apiKey) {
        return Result.success(apiGatewayService.createApiKey(apiKey));
    }

    @Operation(summary = "更新API密钥")
    @PutMapping("/key")
    public Result<ApiKey> updateApiKey(@RequestBody ApiKey apiKey) {
        return Result.success(apiGatewayService.updateApiKey(apiKey));
    }

    @Operation(summary = "分页查询API密钥")
    @GetMapping("/key")
    public Result<Page<ApiKey>> pageApiKeys(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String appType,
            @RequestParam(required = false) String status) {
        return Result.success(
                apiGatewayService.pageApiKeys(new Page<>(page, size), appType, status));
    }

    @Operation(summary = "按appKey查询")
    @GetMapping("/key/{appKey}")
    public Result<ApiKey> getApiKeyByAppKey(@PathVariable String appKey) {
        return Result.success(apiGatewayService.getApiKeyByAppKey(appKey));
    }

    @Operation(summary = "查询活跃密钥")
    @GetMapping("/key/active/list")
    public Result<List<ApiKey>> getActiveApiKeys() {
        return Result.success(apiGatewayService.getActiveApiKeys());
    }

    @Operation(summary = "重置密钥")
    @PutMapping("/key/{appKey}/reset")
    public Result<ApiKey> resetSecret(@PathVariable String appKey) {
        return Result.success(apiGatewayService.resetSecret(appKey));
    }

    @Operation(summary = "启用/禁用密钥")
    @PutMapping("/key/{appKey}/toggle")
    public Result<ApiKey> toggleApiKey(@PathVariable String appKey, @RequestParam boolean enabled) {
        return Result.success(apiGatewayService.toggleApiKey(appKey, enabled));
    }

    // ============================================================
    // API限流配置
    // ============================================================

    @Operation(summary = "创建限流配置")
    @PostMapping("/rate-limit")
    public Result<ApiRateLimit> createRateLimit(@RequestBody ApiRateLimit rateLimit) {
        return Result.success(apiGatewayService.createRateLimit(rateLimit));
    }

    @Operation(summary = "更新限流配置")
    @PutMapping("/rate-limit")
    public Result<ApiRateLimit> updateRateLimit(@RequestBody ApiRateLimit rateLimit) {
        return Result.success(apiGatewayService.updateRateLimit(rateLimit));
    }

    @Operation(summary = "分页查询限流配置")
    @GetMapping("/rate-limit")
    public Result<Page<ApiRateLimit>> pageRateLimits(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) Integer enabled) {
        return Result.success(
                apiGatewayService.pageRateLimits(new Page<>(page, size), targetType, enabled));
    }

    @Operation(summary = "按目标查询限流")
    @GetMapping("/rate-limit/target")
    public Result<ApiRateLimit> getRateLimitByTarget(
            @RequestParam String targetType, @RequestParam String targetValue) {
        return Result.success(apiGatewayService.getRateLimitByTarget(targetType, targetValue));
    }

    @Operation(summary = "查询所有启用限流")
    @GetMapping("/rate-limit/enabled")
    public Result<List<ApiRateLimit>> getAllEnabledRateLimits() {
        return Result.success(apiGatewayService.getAllEnabledRateLimits());
    }

    // ============================================================
    // API调用日志
    // ============================================================

    @Operation(summary = "分页查询调用日志")
    @GetMapping("/call-log")
    public Result<Page<ApiCallLog>> pageCallLogs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String apiCode,
            @RequestParam(required = false) String appKey,
            @RequestParam(required = false) String callStatus,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
                    LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
                    LocalDateTime endTime) {
        return Result.success(
                apiGatewayService.pageCallLogs(
                        new Page<>(page, size), apiCode, appKey, callStatus, startTime, endTime));
    }

    @Operation(summary = "按API查询日志")
    @GetMapping("/call-log/api/{apiCode}")
    public Result<List<ApiCallLog>> getCallLogsByApi(@PathVariable String apiCode) {
        return Result.success(apiGatewayService.getCallLogsByApi(apiCode));
    }

    @Operation(summary = "按appKey查询日志")
    @GetMapping("/call-log/app/{appKey}")
    public Result<List<ApiCallLog>> getCallLogsByAppKey(@PathVariable String appKey) {
        return Result.success(apiGatewayService.getCallLogsByAppKey(appKey));
    }
}
