package com.xwms.core.security.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.core.security.entity.*;
import com.xwms.core.security.mapper.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 库存安全管理核心服务 核心能力: 数据权限/操作审计/安全策略/访问日志 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventorySecurityService {

    private final DataPermissionMapper permissionMapper;
    private final OperationAuditMapper auditMapper;
    private final SecurityPolicyMapper policyMapper;
    private final AccessLogMapper accessLogMapper;

    private static final AtomicInteger SEQ = new AtomicInteger(0);
    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    // ============================================================

    // 1. 数据权限管理
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public DataPermission createPermission(DataPermission permission) {
        permission.setStatus("ACTIVE");
        if (permission.getCanView() == null) permission.setCanView("Y");
        if (permission.getCanCreate() == null) permission.setCanCreate("N");
        if (permission.getCanUpdate() == null) permission.setCanUpdate("N");
        if (permission.getCanDelete() == null) permission.setCanDelete("N");
        if (permission.getCanExport() == null) permission.setCanExport("N");
        if (permission.getCanApprove() == null) permission.setCanApprove("N");
        permissionMapper.insert(permission);
        log.info(
                "创建数据权限: code={}, name={}, type={}, role={}, user={}",
                permission.getPermissionCode(),
                permission.getPermissionName(),
                permission.getPermissionType(),
                permission.getRoleCode(),
                permission.getUserCode());
        return permission;
    }

    public DataPermission getPermissionByCode(String permissionCode) {
        return permissionMapper.selectByPermissionCode(permissionCode);
    }

    public List<DataPermission> getPermissionsByRole(String roleCode) {
        return permissionMapper.selectByRole(roleCode);
    }

    public List<DataPermission> getPermissionsByUser(String userCode) {
        return permissionMapper.selectByUser(userCode);
    }

    public List<DataPermission> getPermissionsByType(String permissionType) {
        return permissionMapper.selectByType(permissionType);
    }

    public List<DataPermission> getPermissionsByResourceType(String resourceType) {
        return permissionMapper.selectByResourceType(resourceType);
    }

    /** 检查用户是否有权限 */
    public boolean hasPermission(
            String userCode,
            String roleCode,
            String resourceType,
            String operation,
            String warehouseCode) {
        // 查询用户权限
        List<DataPermission> userPermissions = permissionMapper.selectByUser(userCode);
        // 查询角色权限
        List<DataPermission> rolePermissions = permissionMapper.selectByRole(roleCode);

        // 合并权限
        List<DataPermission> allPermissions = new ArrayList<>();
        allPermissions.addAll(userPermissions);
        allPermissions.addAll(rolePermissions);

        for (DataPermission permission : allPermissions) {
            if (!resourceType.equals(permission.getResourceType())) continue;
            if (warehouseCode != null
                    && permission.getWarehouseCode() != null
                    && !warehouseCode.equals(permission.getWarehouseCode())) continue;

            switch (operation) {
                case "VIEW":
                    if ("Y".equals(permission.getCanView())) return true;
                    break;
                case "CREATE":
                    if ("Y".equals(permission.getCanCreate())) return true;
                    break;
                case "UPDATE":
                    if ("Y".equals(permission.getCanUpdate())) return true;
                    break;
                case "DELETE":
                    if ("Y".equals(permission.getCanDelete())) return true;
                    break;
                case "EXPORT":
                    if ("Y".equals(permission.getCanExport())) return true;
                    break;
                case "APPROVE":
                    if ("Y".equals(permission.getCanApprove())) return true;
                    break;
                default:
                    // 未知操作类型，拒绝
                    break;
            }
        }
        return false;
    }

    public Page<DataPermission> pagePermissions(
            Page<DataPermission> page, String permissionType, String roleCode, String userCode) {
        LambdaQueryWrapper<DataPermission> wrapper = new LambdaQueryWrapper<>();
        if (permissionType != null) wrapper.eq(DataPermission::getPermissionType, permissionType);
        if (roleCode != null) wrapper.eq(DataPermission::getRoleCode, roleCode);
        if (userCode != null) wrapper.eq(DataPermission::getUserCode, userCode);
        wrapper.eq(DataPermission::getStatus, "ACTIVE");
        wrapper.orderByDesc(DataPermission::getCreatedTime);
        return permissionMapper.selectPage(page, wrapper);
    }

    // ============================================================

    // 2. 操作审计管理（核心）
    // ============================================================

    /** 记录操作审计 */
    @Transactional(rollbackFor = Exception.class)
    public OperationAudit recordAudit(
            String traceId,
            String userCode,
            String userName,
            String roleCode,
            String warehouseCode,
            String ownerCode,
            String module,
            String operation,
            String operationType,
            String bizType,
            String bizNo,
            String requestUrl,
            String requestMethod,
            String requestParams,
            String responseData,
            String beforeData,
            String afterData,
            String ipAddress,
            String userAgent,
            String deviceType,
            String status,
            String errorMessage,
            Long durationMs) {
        OperationAudit audit = new OperationAudit();
        audit.setAuditId(generateAuditId());
        audit.setTraceId(traceId);
        audit.setUserCode(userCode);
        audit.setUserName(userName);
        audit.setRoleCode(roleCode);
        audit.setWarehouseCode(warehouseCode);
        audit.setOwnerCode(ownerCode);
        audit.setModule(module);
        audit.setOperation(operation);
        audit.setOperationType(operationType);
        audit.setBizType(bizType);
        audit.setBizNo(bizNo);
        audit.setRequestUrl(requestUrl);
        audit.setRequestMethod(requestMethod);
        audit.setRequestParams(requestParams);
        audit.setResponseData(responseData);
        audit.setBeforeData(beforeData);
        audit.setAfterData(afterData);
        audit.setIpAddress(ipAddress);
        audit.setUserAgent(userAgent);
        audit.setDeviceType(deviceType);
        audit.setStatus(status != null ? status : "SUCCESS");
        audit.setErrorMessage(errorMessage);
        audit.setDurationMs(durationMs);
        audit.setOperationTime(LocalDateTime.now());
        auditMapper.insert(audit);
        log.debug(
                "记录操作审计: id={}, user={}, module={}, operation={}, status={}",
                audit.getAuditId(),
                userCode,
                module,
                operation,
                audit.getStatus());
        return audit;
    }

    public OperationAudit getAuditById(String auditId) {
        return auditMapper.selectByAuditId(auditId);
    }

    public List<OperationAudit> getAuditsByUserAndTime(
            String userCode, LocalDateTime startTime, int limit) {
        return auditMapper.selectByUserAndTime(userCode, startTime, limit);
    }

    public List<OperationAudit> getAuditsByModuleAndTime(
            String module, LocalDateTime startTime, int limit) {
        return auditMapper.selectByModuleAndTime(module, startTime, limit);
    }

    public List<OperationAudit> getAuditsByBiz(String bizType, String bizNo) {
        return auditMapper.selectByBiz(bizType, bizNo);
    }

    public int countAuditByUserAndOperation(
            String userCode, String operationType, LocalDateTime startTime) {
        return auditMapper.countByUserAndOperation(userCode, operationType, startTime);
    }

    public Page<OperationAudit> pageAudits(
            Page<OperationAudit> page,
            String userCode,
            String module,
            String operationType,
            String bizType,
            String warehouseCode,
            LocalDateTime startTime,
            LocalDateTime endTime) {
        LambdaQueryWrapper<OperationAudit> wrapper = new LambdaQueryWrapper<>();
        if (userCode != null) wrapper.eq(OperationAudit::getUserCode, userCode);
        if (module != null) wrapper.eq(OperationAudit::getModule, module);
        if (operationType != null) wrapper.eq(OperationAudit::getOperationType, operationType);
        if (bizType != null) wrapper.eq(OperationAudit::getBizType, bizType);
        if (warehouseCode != null) wrapper.eq(OperationAudit::getWarehouseCode, warehouseCode);
        if (startTime != null) wrapper.ge(OperationAudit::getOperationTime, startTime);
        if (endTime != null) wrapper.le(OperationAudit::getOperationTime, endTime);
        wrapper.orderByDesc(OperationAudit::getOperationTime);
        return auditMapper.selectPage(page, wrapper);
    }

    // ============================================================

    // 3. 安全策略管理
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public SecurityPolicy createPolicy(SecurityPolicy policy) {
        if (policy.getPriority() == null) policy.setPriority(100);
        if (policy.getIsEnabled() == null) policy.setIsEnabled("Y");
        if (policy.getPolicyScope() == null) policy.setPolicyScope("GLOBAL");
        policyMapper.insert(policy);
        log.info(
                "创建安全策略: code={}, name={}, type={}, scope={}",
                policy.getPolicyCode(),
                policy.getPolicyName(),
                policy.getPolicyType(),
                policy.getPolicyScope());
        return policy;
    }

    public SecurityPolicy getPolicyByCode(String policyCode) {
        return policyMapper.selectByPolicyCode(policyCode);
    }

    public List<SecurityPolicy> getEnabledPoliciesByType(String policyType) {
        return policyMapper.selectEnabledByType(policyType);
    }

    public List<SecurityPolicy> getPoliciesByScope(String policyScope, String scopeValue) {
        return policyMapper.selectByScope(policyScope, scopeValue);
    }

    public List<SecurityPolicy> getAllEnabledPolicies() {
        return policyMapper.selectAllEnabled();
    }

    /** 获取适用的安全策略 */
    public List<SecurityPolicy> getApplicablePolicies(
            String policyType, String policyScope, String scopeValue) {
        List<SecurityPolicy> result = new ArrayList<>();
        // 全局策略
        result.addAll(policyMapper.selectEnabledByType(policyType));
        // 范围策略
        if (policyScope != null && scopeValue != null) {
            result.addAll(policyMapper.selectByScope(policyScope, scopeValue));
        }
        // 按优先级排序
        result.sort(Comparator.comparing(SecurityPolicy::getPriority));
        return result;
    }

    public Page<SecurityPolicy> pagePolicies(
            Page<SecurityPolicy> page, String policyType, String policyScope) {
        LambdaQueryWrapper<SecurityPolicy> wrapper = new LambdaQueryWrapper<>();
        if (policyType != null) wrapper.eq(SecurityPolicy::getPolicyType, policyType);
        if (policyScope != null) wrapper.eq(SecurityPolicy::getPolicyScope, policyScope);
        wrapper.orderByDesc(SecurityPolicy::getCreatedTime);
        return policyMapper.selectPage(page, wrapper);
    }

    // ============================================================

    // 4. 访问日志管理
    // ============================================================

    /** 记录访问日志 */
    @Transactional(rollbackFor = Exception.class)
    public AccessLog recordAccessLog(
            String traceId,
            String userCode,
            String userName,
            String warehouseCode,
            String accessType,
            String accessUrl,
            String accessMethod,
            String accessParams,
            String ipAddress,
            String userAgent,
            String deviceType,
            Integer statusCode,
            Long responseTime,
            String isSuccess,
            String errorMessage,
            String sessionId,
            String tokenId) {
        AccessLog logEntry = new AccessLog();
        logEntry.setLogId(generateLogId());
        logEntry.setTraceId(traceId);
        logEntry.setUserCode(userCode);
        logEntry.setUserName(userName);
        logEntry.setWarehouseCode(warehouseCode);
        logEntry.setAccessType(accessType);
        logEntry.setAccessUrl(accessUrl);
        logEntry.setAccessMethod(accessMethod);
        logEntry.setAccessParams(accessParams);
        logEntry.setIpAddress(ipAddress);
        logEntry.setUserAgent(userAgent);
        logEntry.setDeviceType(deviceType);
        logEntry.setStatusCode(statusCode);
        logEntry.setResponseTime(responseTime);
        logEntry.setIsSuccess(isSuccess != null ? isSuccess : "Y");
        logEntry.setErrorMessage(errorMessage);
        logEntry.setSessionId(sessionId);
        logEntry.setTokenId(tokenId);
        logEntry.setAccessTime(LocalDateTime.now());
        accessLogMapper.insert(logEntry);
        log.debug(
                "记录访问日志: id={}, user={}, type={}, url={}, status={}",
                logEntry.getLogId(),
                userCode,
                accessType,
                accessUrl,
                logEntry.getIsSuccess());
        return logEntry;
    }

    public AccessLog getAccessLogById(String logId) {
        return accessLogMapper.selectByLogId(logId);
    }

    public List<AccessLog> getAccessLogsByUserAndTime(
            String userCode, LocalDateTime startTime, int limit) {
        return accessLogMapper.selectByUserAndTime(userCode, startTime, limit);
    }

    public List<AccessLog> getAccessLogsByTypeAndTime(
            String accessType, LocalDateTime startTime, int limit) {
        return accessLogMapper.selectByTypeAndTime(accessType, startTime, limit);
    }

    public List<AccessLog> getAccessLogsByIpAndTime(
            String ipAddress, LocalDateTime startTime, int limit) {
        return accessLogMapper.selectByIpAndTime(ipAddress, startTime, limit);
    }

    public int countLoginSuccess(String userCode, LocalDateTime startTime) {
        return accessLogMapper.countLoginSuccess(userCode, startTime);
    }

    public int countLoginFailed(String userCode, LocalDateTime startTime) {
        return accessLogMapper.countLoginFailed(userCode, startTime);
    }

    public Page<AccessLog> pageAccessLogs(
            Page<AccessLog> page,
            String userCode,
            String accessType,
            String ipAddress,
            String warehouseCode,
            LocalDateTime startTime,
            LocalDateTime endTime) {
        LambdaQueryWrapper<AccessLog> wrapper = new LambdaQueryWrapper<>();
        if (userCode != null) wrapper.eq(AccessLog::getUserCode, userCode);
        if (accessType != null) wrapper.eq(AccessLog::getAccessType, accessType);
        if (ipAddress != null) wrapper.eq(AccessLog::getIpAddress, ipAddress);
        if (warehouseCode != null) wrapper.eq(AccessLog::getWarehouseCode, warehouseCode);
        if (startTime != null) wrapper.ge(AccessLog::getAccessTime, startTime);
        if (endTime != null) wrapper.le(AccessLog::getAccessTime, endTime);
        wrapper.orderByDesc(AccessLog::getAccessTime);
        return accessLogMapper.selectPage(page, wrapper);
    }

    // ============================================================

    // 工具方法
    // ============================================================

    private String generateAuditId() {
        return "OA"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }

    private String generateLogId() {
        return "AL"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }
}
