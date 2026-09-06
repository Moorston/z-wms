package com.xwms.integration.external.controller;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.common.core.Result;
import com.xwms.integration.external.entity.*;
import com.xwms.integration.external.service.IntegrationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/** WCS/TMS/ERP集成 Controller */
@Tag(name = "WCS/TMS/ERP集成", description = "外部系统配置/接口日志/消息队列/回调记录")
@RestController
@RequestMapping("/api/integration")
@RequiredArgsConstructor
public class IntegrationController {

    private final IntegrationService integrationService;

    // ============================================================
    // 外部系统配置
    // ============================================================

    @Operation(summary = "创建外部系统")
    @PostMapping("/system")
    public Result<ExternalSystem> createSystem(@RequestBody ExternalSystem system) {
        return Result.success(integrationService.createSystem(system));
    }

    @Operation(summary = "更新外部系统")
    @PutMapping("/system")
    public Result<ExternalSystem> updateSystem(@RequestBody ExternalSystem system) {
        return Result.success(integrationService.updateSystem(system));
    }

    @Operation(summary = "分页查询外部系统")
    @GetMapping("/system")
    public Result<Page<ExternalSystem>> pageSystems(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String systemType,
            @RequestParam(required = false) String status) {
        return Result.success(
                integrationService.pageSystems(new Page<>(page, size), systemType, status));
    }

    @Operation(summary = "按类型查询活跃系统")
    @GetMapping("/system/type/{type}")
    public Result<List<ExternalSystem>> getActiveSystemsByType(@PathVariable String type) {
        return Result.success(integrationService.getActiveSystemsByType(type));
    }

    @Operation(summary = "按编码查询系统")
    @GetMapping("/system/{code}")
    public Result<ExternalSystem> getSystemByCode(@PathVariable String code) {
        return Result.success(integrationService.getSystemByCode(code));
    }

    // ============================================================
    // 接口调用日志
    // ============================================================

    @Operation(summary = "记录接口调用")
    @PostMapping("/api-log")
    public Result<ApiCallLog> recordApiCall(@RequestBody ApiCallLog log) {
        return Result.success(
                integrationService.recordApiCall(
                        log.getSystemCode(),
                        log.getApiName(),
                        log.getApiUrl(),
                        log.getHttpMethod(),
                        log.getRequestHeaders(),
                        log.getRequestBody(),
                        log.getResponseStatus(),
                        log.getResponseBody(),
                        log.getCostTime(),
                        log.getStatus(),
                        log.getErrorMsg(),
                        log.getTraceId(),
                        log.getBusinessType(),
                        log.getBusinessNo(),
                        log.getDirection()));
    }

    @Operation(summary = "分页查询接口日志")
    @GetMapping("/api-log")
    public Result<Page<ApiCallLog>> pageApiLogs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String systemCode,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String businessType,
            @RequestParam(required = false) String businessNo,
            @RequestParam(required = false) String direction) {
        return Result.success(
                integrationService.pageApiLogs(
                        new Page<>(page, size),
                        systemCode,
                        status,
                        businessType,
                        businessNo,
                        direction));
    }

    @Operation(summary = "查询系统接口日志")
    @GetMapping("/api-log/system/{systemCode}")
    public Result<List<ApiCallLog>> getApiLogsBySystem(@PathVariable String systemCode) {
        return Result.success(integrationService.getApiLogsBySystem(systemCode));
    }

    // ============================================================
    // 集成消息
    // ============================================================

    @Operation(summary = "创建集成消息")
    @PostMapping("/message")
    public Result<IntegrationMessage> createMessage(@RequestBody IntegrationMessage message) {
        return Result.success(
                integrationService.createMessage(
                        message.getSystemCode(),
                        message.getMessageType(),
                        message.getTopic(),
                        message.getPayload(),
                        message.getBusinessType(),
                        message.getBusinessNo(),
                        message.getDirection()));
    }

    @Operation(summary = "分页查询消息")
    @GetMapping("/message")
    public Result<Page<IntegrationMessage>> pageMessages(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String systemCode,
            @RequestParam(required = false) String messageType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String direction) {
        return Result.success(
                integrationService.pageMessages(
                        new Page<>(page, size), systemCode, messageType, status, direction));
    }

    @Operation(summary = "查询待发送消息")
    @GetMapping("/message/pending")
    public Result<List<IntegrationMessage>> getPendingMessages() {
        return Result.success(integrationService.getPendingMessages());
    }

    @Operation(summary = "更新消息状态")
    @PutMapping("/message/{id}/status")
    public Result<IntegrationMessage> updateMessageStatus(
            @PathVariable Long id,
            @RequestParam String status,
            @RequestParam(required = false) String errorMsg) {
        return Result.success(integrationService.updateMessageStatus(id, status, errorMsg));
    }

    // ============================================================
    // 回调记录
    // ============================================================

    @Operation(summary = "创建回调")
    @PostMapping("/callback")
    public Result<CallbackRecord> createCallback(@RequestBody CallbackRecord callback) {
        return Result.success(
                integrationService.createCallback(
                        callback.getSystemCode(),
                        callback.getCallbackUrl(),
                        callback.getCallbackType(),
                        callback.getRequestBody(),
                        callback.getBusinessType(),
                        callback.getBusinessNo()));
    }

    @Operation(summary = "分页查询回调")
    @GetMapping("/callback")
    public Result<Page<CallbackRecord>> pageCallbacks(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String systemCode,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String callbackType) {
        return Result.success(
                integrationService.pageCallbacks(
                        new Page<>(page, size), systemCode, status, callbackType));
    }

    @Operation(summary = "查询待回调")
    @GetMapping("/callback/pending")
    public Result<List<CallbackRecord>> getPendingCallbacks() {
        return Result.success(integrationService.getPendingCallbacks());
    }

    @Operation(summary = "更新回调状态")
    @PutMapping("/callback/{id}/status")
    public Result<CallbackRecord> updateCallbackStatus(
            @PathVariable Long id,
            @RequestParam String status,
            @RequestParam(required = false) Integer responseStatus,
            @RequestParam(required = false) String responseBody,
            @RequestParam(required = false) Long costTime,
            @RequestParam(required = false) String errorMsg) {
        return Result.success(
                integrationService.updateCallbackStatus(
                        id, status, responseStatus, responseBody, costTime, errorMsg));
    }
}
