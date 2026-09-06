package com.xwms.core.security.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.common.core.Result;
import com.xwms.core.security.entity.*;
import com.xwms.core.security.service.InventorySecurityService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/** 库存安全管理 Controller */
@Tag(name = "库存安全管理", description = "数据权限/操作审计/安全策略/访问日志")
@RestController
@RequestMapping("/api/security")
@RequiredArgsConstructor
public class InventorySecurityController {

    private final InventorySecurityService securityService;

    // ============================================================

    // 数据权限
    // ============================================================

    @Operation(summary = "创建数据权限")
    @PostMapping("/permission")
    public Result<DataPermission> createPermission(@RequestBody DataPermission permission) {
        return Result.success(securityService.createPermission(permission));
    }

    @Operation(summary = "按编码查询数据权限")
    @GetMapping("/permission/{permissionCode}")
    public Result<DataPermission> getPermissionByCode(@PathVariable String permissionCode) {
        return Result.success(securityService.getPermissionByCode(permissionCode));
    }

    @Operation(summary = "按角色查询数据权限")
    @GetMapping("/permission/role/{roleCode}")
    public Result<List<DataPermission>> getPermissionsByRole(@PathVariable String roleCode) {
        return Result.success(securityService.getPermissionsByRole(roleCode));
    }

    @Operation(summary = "按用户查询数据权限")
    @GetMapping("/permission/user/{userCode}")
    public Result<List<DataPermission>> getPermissionsByUser(@PathVariable String userCode) {
        return Result.success(securityService.getPermissionsByUser(userCode));
    }

    @Operation(summary = "按类型查询数据权限")
    @GetMapping("/permission/type/{permissionType}")
    public Result<List<DataPermission>> getPermissionsByType(@PathVariable String permissionType) {
        return Result.success(securityService.getPermissionsByType(permissionType));
    }

    @Operation(summary = "按资源类型查询数据权限")
    @GetMapping("/permission/resource/{resourceType}")
    public Result<List<DataPermission>> getPermissionsByResourceType(
            @PathVariable String resourceType) {
        return Result.success(securityService.getPermissionsByResourceType(resourceType));
    }

    @Operation(summary = "检查用户权限")
    @GetMapping("/permission/check")
    public Result<Boolean> hasPermission(
            @RequestParam String userCode,
            @RequestParam(required = false) String roleCode,
            @RequestParam String resourceType,
            @RequestParam String operation,
            @RequestParam(required = false) String warehouseCode) {
        return Result.success(
                securityService.hasPermission(
                        userCode, roleCode, resourceType, operation, warehouseCode));
    }

    @Operation(summary = "分页查询数据权限")
    @GetMapping("/permission/list")
    public Result<Page<DataPermission>> pagePermissions(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String permissionType,
            @RequestParam(required = false) String roleCode,
            @RequestParam(required = false) String userCode) {
        return Result.success(
                securityService.pagePermissions(
                        new Page<>(page, size), permissionType, roleCode, userCode));
    }

    // ============================================================

    // 操作审计
    // ============================================================

    @Operation(summary = "记录操作审计")
    @PostMapping("/audit")
    public Result<OperationAudit> recordAudit(@RequestBody OperationAudit audit) {
        return Result.success(
                securityService.recordAudit(
                        audit.getTraceId(),
                        audit.getUserCode(),
                        audit.getUserName(),
                        audit.getRoleCode(),
                        audit.getWarehouseCode(),
                        audit.getOwnerCode(),
                        audit.getModule(),
                        audit.getOperation(),
                        audit.getOperationType(),
                        audit.getBizType(),
                        audit.getBizNo(),
                        audit.getRequestUrl(),
                        audit.getRequestMethod(),
                        audit.getRequestParams(),
                        audit.getResponseData(),
                        audit.getBeforeData(),
                        audit.getAfterData(),
                        audit.getIpAddress(),
                        audit.getUserAgent(),
                        audit.getDeviceType(),
                        audit.getStatus(),
                        audit.getErrorMessage(),
                        audit.getDurationMs()));
    }

    @Operation(summary = "按ID查询操作审计")
    @GetMapping("/audit/{auditId}")
    public Result<OperationAudit> getAuditById(@PathVariable String auditId) {
        return Result.success(securityService.getAuditById(auditId));
    }

    @Operation(summary = "按用户和时间查询操作审计")
    @GetMapping("/audit/user/{userCode}")
    public Result<List<OperationAudit>> getAuditsByUserAndTime(
            @PathVariable String userCode,
            @RequestParam LocalDateTime startTime,
            @RequestParam(defaultValue = "50") int limit) {
        return Result.success(securityService.getAuditsByUserAndTime(userCode, startTime, limit));
    }

    @Operation(summary = "按模块和时间查询操作审计")
    @GetMapping("/audit/module/{module}")
    public Result<List<OperationAudit>> getAuditsByModuleAndTime(
            @PathVariable String module,
            @RequestParam LocalDateTime startTime,
            @RequestParam(defaultValue = "50") int limit) {
        return Result.success(securityService.getAuditsByModuleAndTime(module, startTime, limit));
    }

    @Operation(summary = "按业务查询操作审计")
    @GetMapping("/audit/biz")
    public Result<List<OperationAudit>> getAuditsByBiz(
            @RequestParam String bizType, @RequestParam String bizNo) {
        return Result.success(securityService.getAuditsByBiz(bizType, bizNo));
    }

    @Operation(summary = "统计用户操作次数")
    @GetMapping("/audit/count")
    public Result<Integer> countAuditByUserAndOperation(
            @RequestParam String userCode,
            @RequestParam String operationType,
            @RequestParam LocalDateTime startTime) {
        return Result.success(
                securityService.countAuditByUserAndOperation(userCode, operationType, startTime));
    }

    @Operation(summary = "分页查询操作审计")
    @GetMapping("/audit/list")
    public Result<Page<OperationAudit>> pageAudits(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String userCode,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String operationType,
            @RequestParam(required = false) String bizType,
            @RequestParam(required = false) String warehouseCode,
            @RequestParam(required = false) LocalDateTime startTime,
            @RequestParam(required = false) LocalDateTime endTime) {
        return Result.success(
                securityService.pageAudits(
                        new Page<>(page, size),
                        userCode,
                        module,
                        operationType,
                        bizType,
                        warehouseCode,
                        startTime,
                        endTime));
    }

    // ============================================================

    // 安全策略
    // ============================================================

    @Operation(summary = "创建安全策略")
    @PostMapping("/policy")
    public Result<SecurityPolicy> createPolicy(@RequestBody SecurityPolicy policy) {
        return Result.success(securityService.createPolicy(policy));
    }

    @Operation(summary = "按编码查询安全策略")
    @GetMapping("/policy/{policyCode}")
    public Result<SecurityPolicy> getPolicyByCode(@PathVariable String policyCode) {
        return Result.success(securityService.getPolicyByCode(policyCode));
    }

    @Operation(summary = "按类型查询启用的安全策略")
    @GetMapping("/policy/type/{policyType}")
    public Result<List<SecurityPolicy>> getEnabledPoliciesByType(@PathVariable String policyType) {
        return Result.success(securityService.getEnabledPoliciesByType(policyType));
    }

    @Operation(summary = "按范围查询安全策略")
    @GetMapping("/policy/scope")
    public Result<List<SecurityPolicy>> getPoliciesByScope(
            @RequestParam String policyScope, @RequestParam String scopeValue) {
        return Result.success(securityService.getPoliciesByScope(policyScope, scopeValue));
    }

    @Operation(summary = "查询所有启用的安全策略")
    @GetMapping("/policy/all-enabled")
    public Result<List<SecurityPolicy>> getAllEnabledPolicies() {
        return Result.success(securityService.getAllEnabledPolicies());
    }

    @Operation(summary = "获取适用的安全策略")
    @GetMapping("/policy/applicable")
    public Result<List<SecurityPolicy>> getApplicablePolicies(
            @RequestParam String policyType,
            @RequestParam(required = false) String policyScope,
            @RequestParam(required = false) String scopeValue) {
        return Result.success(
                securityService.getApplicablePolicies(policyType, policyScope, scopeValue));
    }

    @Operation(summary = "分页查询安全策略")
    @GetMapping("/policy/list")
    public Result<Page<SecurityPolicy>> pagePolicies(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String policyType,
            @RequestParam(required = false) String policyScope) {
        return Result.success(
                securityService.pagePolicies(new Page<>(page, size), policyType, policyScope));
    }

    // ============================================================

    // 访问日志
    // ============================================================

    @Operation(summary = "记录访问日志")
    @PostMapping("/access-log")
    public Result<AccessLog> recordAccessLog(@RequestBody AccessLog accessLog) {
        return Result.success(
                securityService.recordAccessLog(
                        accessLog.getTraceId(),
                        accessLog.getUserCode(),
                        accessLog.getUserName(),
                        accessLog.getWarehouseCode(),
                        accessLog.getAccessType(),
                        accessLog.getAccessUrl(),
                        accessLog.getAccessMethod(),
                        accessLog.getAccessParams(),
                        accessLog.getIpAddress(),
                        accessLog.getUserAgent(),
                        accessLog.getDeviceType(),
                        accessLog.getStatusCode(),
                        accessLog.getResponseTime(),
                        accessLog.getIsSuccess(),
                        accessLog.getErrorMessage(),
                        accessLog.getSessionId(),
                        accessLog.getTokenId()));
    }

    @Operation(summary = "按ID查询访问日志")
    @GetMapping("/access-log/{logId}")
    public Result<AccessLog> getAccessLogById(@PathVariable String logId) {
        return Result.success(securityService.getAccessLogById(logId));
    }

    @Operation(summary = "按用户和时间查询访问日志")
    @GetMapping("/access-log/user/{userCode}")
    public Result<List<AccessLog>> getAccessLogsByUserAndTime(
            @PathVariable String userCode,
            @RequestParam LocalDateTime startTime,
            @RequestParam(defaultValue = "50") int limit) {
        return Result.success(
                securityService.getAccessLogsByUserAndTime(userCode, startTime, limit));
    }

    @Operation(summary = "按类型和时间查询访问日志")
    @GetMapping("/access-log/type/{accessType}")
    public Result<List<AccessLog>> getAccessLogsByTypeAndTime(
            @PathVariable String accessType,
            @RequestParam LocalDateTime startTime,
            @RequestParam(defaultValue = "50") int limit) {
        return Result.success(
                securityService.getAccessLogsByTypeAndTime(accessType, startTime, limit));
    }

    @Operation(summary = "按IP和时间查询访问日志")
    @GetMapping("/access-log/ip/{ipAddress}")
    public Result<List<AccessLog>> getAccessLogsByIpAndTime(
            @PathVariable String ipAddress,
            @RequestParam LocalDateTime startTime,
            @RequestParam(defaultValue = "50") int limit) {
        return Result.success(
                securityService.getAccessLogsByIpAndTime(ipAddress, startTime, limit));
    }

    @Operation(summary = "统计登录成功次数")
    @GetMapping("/access-log/login-success")
    public Result<Integer> countLoginSuccess(
            @RequestParam String userCode, @RequestParam LocalDateTime startTime) {
        return Result.success(securityService.countLoginSuccess(userCode, startTime));
    }

    @Operation(summary = "统计登录失败次数")
    @GetMapping("/access-log/login-failed")
    public Result<Integer> countLoginFailed(
            @RequestParam String userCode, @RequestParam LocalDateTime startTime) {
        return Result.success(securityService.countLoginFailed(userCode, startTime));
    }

    @Operation(summary = "分页查询访问日志")
    @GetMapping("/access-log/list")
    public Result<Page<AccessLog>> pageAccessLogs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String userCode,
            @RequestParam(required = false) String accessType,
            @RequestParam(required = false) String ipAddress,
            @RequestParam(required = false) String warehouseCode,
            @RequestParam(required = false) LocalDateTime startTime,
            @RequestParam(required = false) LocalDateTime endTime) {
        return Result.success(
                securityService.pageAccessLogs(
                        new Page<>(page, size),
                        userCode,
                        accessType,
                        ipAddress,
                        warehouseCode,
                        startTime,
                        endTime));
    }
}
