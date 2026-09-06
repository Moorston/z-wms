package com.xwms.base.audit.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.base.audit.entity.*;
import com.xwms.base.audit.service.AuditService;
import com.xwms.common.core.Result;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/** 系统监控与安全审计 Controller */
@Tag(name = "系统监控与安全审计", description = "操作日志/登录日志/安全审计/系统监控")
@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;

    // ============================================================
    // 操作日志
    // ============================================================

    @Operation(summary = "记录操作日志")
    @PostMapping("/operation")
    public Result<OperationLog> recordOperation(@RequestBody OperationLog log) {
        return Result.success(
                auditService.recordOperation(
                        log.getUserId(),
                        log.getUserName(),
                        log.getModule(),
                        log.getOperation(),
                        log.getMethod(),
                        log.getRequestUrl(),
                        log.getRequestMethod(),
                        log.getRequestParams(),
                        log.getResponseResult(),
                        log.getIpAddress(),
                        log.getUserAgent(),
                        log.getCostTime(),
                        log.getStatus(),
                        log.getErrorMsg(),
                        log.getBusinessType(),
                        log.getBusinessNo(),
                        log.getTraceId()));
    }

    @Operation(summary = "分页查询操作日志")
    @GetMapping("/operation")
    public Result<Page<OperationLog>> pageOperationLogs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String businessType,
            @RequestParam(required = false) String businessNo) {
        return Result.success(
                auditService.pageOperationLogs(
                        new Page<>(page, size), userId, module, status, businessType, businessNo));
    }

    @Operation(summary = "查询用户操作日志")
    @GetMapping("/operation/user/{userId}")
    public Result<List<OperationLog>> getOperationLogsByUser(@PathVariable String userId) {
        return Result.success(auditService.getOperationLogsByUser(userId));
    }

    @Operation(summary = "查询业务操作日志")
    @GetMapping("/operation/business")
    public Result<List<OperationLog>> getOperationLogsByBusiness(
            @RequestParam String businessType, @RequestParam String businessNo) {
        return Result.success(auditService.getOperationLogsByBusiness(businessType, businessNo));
    }

    // ============================================================
    // 登录日志
    // ============================================================

    @Operation(summary = "记录登录日志")
    @PostMapping("/login")
    public Result<LoginLog> recordLogin(@RequestBody LoginLog log) {
        return Result.success(
                auditService.recordLogin(
                        log.getUserId(),
                        log.getUserName(),
                        log.getLoginType(),
                        log.getLoginStatus(),
                        log.getFailReason(),
                        log.getIpAddress(),
                        log.getUserAgent(),
                        log.getDeviceType(),
                        log.getBrowser(),
                        log.getOs(),
                        log.getLocation(),
                        log.getSessionId()));
    }

    @Operation(summary = "分页查询登录日志")
    @GetMapping("/login")
    public Result<Page<LoginLog>> pageLoginLogs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String loginStatus,
            @RequestParam(required = false) String deviceType) {
        return Result.success(
                auditService.pageLoginLogs(
                        new Page<>(page, size), userId, loginStatus, deviceType));
    }

    @Operation(summary = "查询用户登录日志")
    @GetMapping("/login/user/{userId}")
    public Result<List<LoginLog>> getLoginLogsByUser(@PathVariable String userId) {
        return Result.success(auditService.getLoginLogsByUser(userId));
    }

    // ============================================================
    // 安全审计
    // ============================================================

    @Operation(summary = "创建安全审计")
    @PostMapping("/security")
    public Result<SecurityAudit> createSecurityAudit(@RequestBody SecurityAudit audit) {
        return Result.success(
                auditService.createSecurityAudit(
                        audit.getAuditType(),
                        audit.getUserId(),
                        audit.getUserName(),
                        audit.getRiskLevel(),
                        audit.getDescription(),
                        audit.getResourceType(),
                        audit.getResourceId(),
                        audit.getAction(),
                        audit.getBeforeValue(),
                        audit.getAfterValue(),
                        audit.getIpAddress()));
    }

    @Operation(summary = "分页查询安全审计")
    @GetMapping("/security")
    public Result<Page<SecurityAudit>> pageSecurityAudits(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String auditType,
            @RequestParam(required = false) String riskLevel,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String userId) {
        return Result.success(
                auditService.pageSecurityAudits(
                        new Page<>(page, size), auditType, riskLevel, status, userId));
    }

    @Operation(summary = "查询待处理审计")
    @GetMapping("/security/pending")
    public Result<List<SecurityAudit>> getPendingAudits() {
        return Result.success(auditService.getPendingAudits());
    }

    @Operation(summary = "处理安全审计")
    @PutMapping("/security/{id}/handle")
    public Result<SecurityAudit> handleAudit(
            @PathVariable Long id,
            @RequestParam String handledBy,
            @RequestParam(required = false) String handleRemark,
            @RequestParam(defaultValue = "true") boolean resolved) {
        return Result.success(auditService.handleAudit(id, handledBy, handleRemark, resolved));
    }

    // ============================================================
    // 系统监控
    // ============================================================

    @Operation(summary = "采集系统指标")
    @PostMapping("/monitor/collect")
    public Result<MonitorMetric> collectMetric(@RequestBody MonitorMetric metric) {
        return Result.success(
                auditService.collectMetric(
                        metric.getMetricName(),
                        metric.getMetricCategory(),
                        metric.getMetricValue(),
                        metric.getMetricUnit(),
                        metric.getThresholdWarn(),
                        metric.getThresholdCritical(),
                        metric.getInstanceId()));
    }

    @Operation(summary = "查询指标历史")
    @GetMapping("/monitor/history")
    public Result<List<MonitorMetric>> getMetricHistory(
            @RequestParam String metricName,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    LocalDateTime startTime) {
        return Result.success(auditService.getMetricHistory(metricName, startTime));
    }

    @Operation(summary = "查询异常指标")
    @GetMapping("/monitor/abnormal")
    public Result<List<MonitorMetric>> getAbnormalMetrics() {
        return Result.success(auditService.getAbnormalMetrics());
    }
}
