package com.xwms.base.audit.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.base.audit.entity.*;
import com.xwms.base.audit.enums.RiskLevel;
import com.xwms.base.audit.mapper.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 系统监控与安全审计核心服务 包含: 操作日志/登录日志/安全审计/系统监控 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final OperationLogMapper operationLogMapper;
    private final LoginLogMapper loginLogMapper;
    private final SecurityAuditMapper securityAuditMapper;
    private final MonitorMetricMapper monitorMetricMapper;

    private static final AtomicInteger LOG_SEQ = new AtomicInteger(0);
    private static final AtomicInteger AUDIT_SEQ = new AtomicInteger(0);
    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    // ============================================================
    // 1. 操作日志
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public OperationLog recordOperation(
            String userId,
            String userName,
            String module,
            String operation,
            String method,
            String requestUrl,
            String requestMethod,
            String requestParams,
            String responseResult,
            String ipAddress,
            String userAgent,
            Long costTime,
            String status,
            String errorMsg,
            String businessType,
            String businessNo,
            String traceId) {
        OperationLog opLog = new OperationLog();
        opLog.setLogNo(generateLogNo());
        opLog.setUserId(userId);
        opLog.setUserName(userName);
        opLog.setModule(module);
        opLog.setOperation(operation);
        opLog.setMethod(method);
        opLog.setRequestUrl(requestUrl);
        opLog.setRequestMethod(requestMethod);
        opLog.setRequestParams(requestParams);
        opLog.setResponseResult(responseResult);
        opLog.setIpAddress(ipAddress);
        opLog.setUserAgent(userAgent);
        opLog.setCostTime(costTime);
        opLog.setStatus(status);
        opLog.setErrorMsg(errorMsg);
        opLog.setBusinessType(businessType);
        opLog.setBusinessNo(businessNo);
        opLog.setTraceId(traceId);
        operationLogMapper.insert(opLog);
        return opLog;
    }

    public Page<OperationLog> pageOperationLogs(
            Page<OperationLog> page,
            String userId,
            String module,
            String status,
            String businessType,
            String businessNo) {
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();
        if (userId != null) wrapper.eq(OperationLog::getUserId, userId);
        if (module != null) wrapper.eq(OperationLog::getModule, module);
        if (status != null) wrapper.eq(OperationLog::getStatus, status);
        if (businessType != null) wrapper.eq(OperationLog::getBusinessType, businessType);
        if (businessNo != null) wrapper.eq(OperationLog::getBusinessNo, businessNo);
        wrapper.orderByDesc(OperationLog::getCreatedTime);
        return operationLogMapper.selectPage(page, wrapper);
    }

    public List<OperationLog> getOperationLogsByUser(String userId) {
        return operationLogMapper.selectByUserId(userId);
    }

    public List<OperationLog> getOperationLogsByBusiness(String businessType, String businessNo) {
        return operationLogMapper.selectByBusiness(businessType, businessNo);
    }

    // ============================================================
    // 2. 登录日志
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public LoginLog recordLogin(
            String userId,
            String userName,
            String loginType,
            String loginStatus,
            String failReason,
            String ipAddress,
            String userAgent,
            String deviceType,
            String browser,
            String os,
            String location,
            String sessionId) {
        LoginLog loginLog = new LoginLog();
        loginLog.setLogNo(generateLogNo());
        loginLog.setUserId(userId);
        loginLog.setUserName(userName);
        loginLog.setLoginType(loginType);
        loginLog.setLoginStatus(loginStatus);
        loginLog.setFailReason(failReason);
        loginLog.setIpAddress(ipAddress);
        loginLog.setUserAgent(userAgent);
        loginLog.setDeviceType(deviceType);
        loginLog.setBrowser(browser);
        loginLog.setOs(os);
        loginLog.setLocation(location);
        loginLog.setSessionId(sessionId);
        loginLogMapper.insert(loginLog);

        // 登录失败自动创建安全审计
        if ("FAILED".equals(loginStatus) && "LOGIN".equals(loginType)) {
            createSecurityAudit(
                    "LOGIN_ABNORMAL",
                    userId,
                    userName,
                    RiskLevel.MEDIUM.getCode(),
                    "登录失败: " + failReason,
                    "USER",
                    userId,
                    "LOGIN",
                    null,
                    null,
                    ipAddress);
        }
        return loginLog;
    }

    public Page<LoginLog> pageLoginLogs(
            Page<LoginLog> page, String userId, String loginStatus, String deviceType) {
        LambdaQueryWrapper<LoginLog> wrapper = new LambdaQueryWrapper<>();
        if (userId != null) wrapper.eq(LoginLog::getUserId, userId);
        if (loginStatus != null) wrapper.eq(LoginLog::getLoginStatus, loginStatus);
        if (deviceType != null) wrapper.eq(LoginLog::getDeviceType, deviceType);
        wrapper.orderByDesc(LoginLog::getCreatedTime);
        return loginLogMapper.selectPage(page, wrapper);
    }

    public List<LoginLog> getLoginLogsByUser(String userId) {
        return loginLogMapper.selectByUserId(userId);
    }

    // ============================================================
    // 3. 安全审计
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public SecurityAudit createSecurityAudit(
            String auditType,
            String userId,
            String userName,
            String riskLevel,
            String description,
            String resourceType,
            String resourceId,
            String action,
            String beforeValue,
            String afterValue,
            String ipAddress) {
        SecurityAudit audit = new SecurityAudit();
        audit.setAuditNo(generateAuditNo());
        audit.setAuditType(auditType);
        audit.setUserId(userId);
        audit.setUserName(userName);
        audit.setRiskLevel(riskLevel);
        audit.setDescription(description);
        audit.setResourceType(resourceType);
        audit.setResourceId(resourceId);
        audit.setAction(action);
        audit.setBeforeValue(beforeValue);
        audit.setAfterValue(afterValue);
        audit.setIpAddress(ipAddress);
        audit.setStatus("PENDING");
        securityAuditMapper.insert(audit);
        log.warn(
                "安全审计: {}, 类型={}, 风险={}, 用户={}",
                audit.getAuditNo(),
                auditType,
                riskLevel,
                userName);
        return audit;
    }

    public Page<SecurityAudit> pageSecurityAudits(
            Page<SecurityAudit> page,
            String auditType,
            String riskLevel,
            String status,
            String userId) {
        LambdaQueryWrapper<SecurityAudit> wrapper = new LambdaQueryWrapper<>();
        if (auditType != null) wrapper.eq(SecurityAudit::getAuditType, auditType);
        if (riskLevel != null) wrapper.eq(SecurityAudit::getRiskLevel, riskLevel);
        if (status != null) wrapper.eq(SecurityAudit::getStatus, status);
        if (userId != null) wrapper.eq(SecurityAudit::getUserId, userId);
        wrapper.orderByDesc(SecurityAudit::getCreatedTime);
        return securityAuditMapper.selectPage(page, wrapper);
    }

    public List<SecurityAudit> getPendingAudits() {
        return securityAuditMapper.selectPending();
    }

    @Transactional(rollbackFor = Exception.class)
    public SecurityAudit handleAudit(
            Long auditId, String handledBy, String handleRemark, boolean resolved) {
        SecurityAudit audit = securityAuditMapper.selectById(auditId);
        if (audit == null) throw new com.xwms.common.exception.BizException("审计记录不存在");
        audit.setStatus(resolved ? "RESOLVED" : "IGNORED");
        audit.setHandledBy(handledBy);
        audit.setHandledTime(LocalDateTime.now());
        audit.setHandleRemark(handleRemark);
        securityAuditMapper.updateById(audit);
        log.info("处理安全审计: {}, 结果={}", audit.getAuditNo(), resolved ? "已解决" : "已忽略");
        return audit;
    }

    // ============================================================
    // 4. 系统监控
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public MonitorMetric collectMetric(
            String metricName,
            String metricCategory,
            BigDecimal metricValue,
            String metricUnit,
            BigDecimal thresholdWarn,
            BigDecimal thresholdCritical,
            String instanceId) {
        // 判断状态
        String status = "NORMAL";
        if (thresholdCritical != null && metricValue.compareTo(thresholdCritical) >= 0) {
            status = "CRITICAL";
        } else if (thresholdWarn != null && metricValue.compareTo(thresholdWarn) >= 0) {
            status = "WARN";
        }

        MonitorMetric metric = new MonitorMetric();
        metric.setMetricName(metricName);
        metric.setMetricCategory(metricCategory);
        metric.setMetricValue(metricValue);
        metric.setMetricUnit(metricUnit);
        metric.setThresholdWarn(thresholdWarn);
        metric.setThresholdCritical(thresholdCritical);
        metric.setStatus(status);
        metric.setInstanceId(instanceId);
        metric.setCollectedTime(LocalDateTime.now());
        monitorMetricMapper.insert(metric);

        // 严重告警自动创建安全审计
        if ("CRITICAL".equals(status)) {
            createSecurityAudit(
                    "SYSTEM_ALERT",
                    "SYSTEM",
                    "系统监控",
                    RiskLevel.HIGH.getCode(),
                    "系统指标严重告警: " + metricName + "=" + metricValue + metricUnit,
                    "SYSTEM",
                    metricName,
                    "ALERT",
                    null,
                    null,
                    null);
        }
        return metric;
    }

    public List<MonitorMetric> getMetricHistory(String metricName, LocalDateTime startTime) {
        return monitorMetricMapper.selectByNameAndTimeRange(metricName, startTime);
    }

    public List<MonitorMetric> getAbnormalMetrics() {
        return monitorMetricMapper.selectAbnormal();
    }

    // ============================================================
    // 工具方法
    // ============================================================

    private String generateLogNo() {
        return "LOG"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", LOG_SEQ.incrementAndGet() % 1000);
    }

    private String generateAuditNo() {
        return "AUD"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", AUDIT_SEQ.incrementAndGet() % 1000);
    }
}
