package com.xwms.core.alert.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.core.alert.entity.*;
import com.xwms.core.alert.enums.AlertSeverity;
import com.xwms.core.alert.mapper.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 预警管理核心服务 包含: 预警规则/预警记录/预警通知/预警处理 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertService {

    private final AlertRuleMapper alertRuleMapper;
    private final AlertRecordMapper alertRecordMapper;
    private final AlertHandleMapper alertHandleMapper;

    private static final AtomicInteger ALERT_SEQ = new AtomicInteger(0);
    private static final AtomicInteger NOTIFY_SEQ = new AtomicInteger(0);
    private static final AtomicInteger HANDLE_SEQ = new AtomicInteger(0);
    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    // ============================================================
    // 1. 预警规则
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public AlertRule createRule(AlertRule rule) {
        alertRuleMapper.insert(rule);
        log.info("创建预警规则: {}={}", rule.getRuleCode(), rule.getRuleName());
        return rule;
    }

    @Transactional(rollbackFor = Exception.class)
    public AlertRule updateRule(AlertRule rule) {
        alertRuleMapper.updateById(rule);
        return rule;
    }

    public Page<AlertRule> pageRules(
            Page<AlertRule> page,
            String alertCategory,
            String alertType,
            String severity,
            Integer enabled) {
        LambdaQueryWrapper<AlertRule> wrapper = new LambdaQueryWrapper<>();
        if (alertCategory != null) wrapper.eq(AlertRule::getAlertType, alertCategory);
        if (alertType != null) wrapper.eq(AlertRule::getAlertType, alertType);
        if (severity != null) wrapper.eq(AlertRule::getAlertLevel, severity);
        if (enabled != null) wrapper.eq(AlertRule::getStatus, enabled);
        wrapper.orderByDesc(AlertRule::getAlertLevel).orderByAsc(AlertRule::getRuleCode);
        return alertRuleMapper.selectPage(page, wrapper);
    }

    public List<AlertRule> getEnabledRulesByCategory(String category) {
        return alertRuleMapper.selectEnabledByCategory(category);
    }

    public List<AlertRule> getAllEnabledRules() {
        return alertRuleMapper.selectAllEnabled();
    }

    // ============================================================
    // 2. 预警记录
    // ============================================================

    /** 触发预警 */
    @Transactional(rollbackFor = Exception.class)
    public AlertRecord triggerAlert(
            String ruleCode,
            String title,
            String content,
            String businessType,
            String businessNo,
            String warehouseCode,
            String locationCode,
            String skuCode,
            BigDecimal currentValue,
            BigDecimal thresholdValue,
            String severity) {
        // 检查是否已有相同预警(防重复)
        AlertRule rule =
                alertRuleMapper.selectOne(
                        new LambdaQueryWrapper<AlertRule>().eq(AlertRule::getRuleCode, ruleCode));
        if (rule != null) {
            AlertRecord existing =
                    alertRecordMapper.selectPendingByTypeAndBiz(rule.getAlertType(), businessNo);
            if (existing != null) {
                log.info("预警已存在, 跳过: {} {}", rule.getAlertType(), businessNo);
                return existing;
            }
        }

        AlertRecord alert = new AlertRecord();
        alert.setAlertId(generateAlertNo());
        alert.setRuleCode(ruleCode);
        alert.setRuleName(rule != null ? rule.getRuleName() : null);
        alert.setAlertCategory(rule != null ? rule.getAlertType() : null);
        alert.setAlertType(rule != null ? rule.getAlertType() : null);
        alert.setAlertLevel(severity != null ? severity : AlertSeverity.WARNING.getCode());
        alert.setAlertTitle(title);
        alert.setAlertContent(content);
        alert.setBusinessType(businessType);
        alert.setBusinessNo(businessNo);
        alert.setWarehouseCode(warehouseCode);
        alert.setLocationCode(locationCode);
        alert.setSkuCode(skuCode);
        alert.setCurrentValue(currentValue);
        alert.setThresholdValue(thresholdValue);
        alert.setStatus("PENDING");
        alert.setTriggerTime(LocalDateTime.now());
        alertRecordMapper.insert(alert);

        log.warn("触发预警: {} {} 严重程度={}", alert.getAlertId(), title, alert.getAlertLevel());

        // 发送通知
        if (rule != null && rule.getNotifyChannels() != null) {
            // TODO 按通道发送通知（邮件/短信/Webhook），待通知服务实现
        }
        return alert;
    }

    public Page<AlertRecord> pageAlerts(
            Page<AlertRecord> page,
            String status,
            String severity,
            String alertCategory,
            String alertType,
            String warehouseCode) {
        LambdaQueryWrapper<AlertRecord> wrapper = new LambdaQueryWrapper<>();
        if (status != null) wrapper.eq(AlertRecord::getStatus, status);
        if (severity != null) wrapper.eq(AlertRecord::getAlertLevel, severity);
        if (alertCategory != null) wrapper.eq(AlertRecord::getAlertType, alertCategory);
        if (alertType != null) wrapper.eq(AlertRecord::getAlertType, alertType);
        if (warehouseCode != null) wrapper.eq(AlertRecord::getWarehouseCode, warehouseCode);
        wrapper.orderByDesc(AlertRecord::getTriggerTime);
        return alertRecordMapper.selectPage(page, wrapper);
    }

    public List<AlertRecord> getPendingAlerts() {
        return alertRecordMapper.selectPendingAlerts();
    }

    public AlertRecord getAlertById(Long id) {
        return alertRecordMapper.selectById(id);
    }

    /** 确认预警 */
    @Transactional(rollbackFor = Exception.class)
    public AlertRecord ackAlert(Long alertId, String handleBy, String handleName, String remark) {
        AlertRecord alert = alertRecordMapper.selectById(alertId);
        if (alert == null) throw new RuntimeException("预警不存在");
        alert.setStatus("PROCESSING");
        alertRecordMapper.updateById(alert);
        recordHandle(alertId, alert.getAlertId(), "ACK", handleBy, handleName, remark);
        return alert;
    }

    /** 解决预警 */
    @Transactional(rollbackFor = Exception.class)
    public AlertRecord resolveAlert(
            Long alertId, String handleBy, String handleName, String remark) {
        AlertRecord alert = alertRecordMapper.selectById(alertId);
        if (alert == null) throw new RuntimeException("预警不存在");
        alert.setStatus("RESOLVED");
        alert.setResolveTime(LocalDateTime.now());
        alert.setResolvedBy(handleBy);
        alert.setResolveNote(remark);
        alertRecordMapper.updateById(alert);
        recordHandle(alertId, alert.getAlertId(), "RESOLVE", handleBy, handleName, remark);
        log.info("解决预警: {} by {}", alert.getAlertId(), handleBy);
        return alert;
    }

    /** 忽略预警 */
    @Transactional(rollbackFor = Exception.class)
    public AlertRecord ignoreAlert(
            Long alertId, String handleBy, String handleName, String remark) {
        AlertRecord alert = alertRecordMapper.selectById(alertId);
        if (alert == null) throw new RuntimeException("预警不存在");
        alert.setStatus("IGNORED");
        alertRecordMapper.updateById(alert);
        recordHandle(alertId, alert.getAlertId(), "IGNORE", handleBy, handleName, remark);
        return alert;
    }

    // ============================================================
    // 4. 预警处理
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public AlertHandle recordHandle(
            Long alertId,
            String alertNo,
            String handleType,
            String handleBy,
            String handleName,
            String remark) {
        AlertHandle handle = new AlertHandle();
        handle.setHandleId(generateHandleNo());
        handle.setAlertId(alertNo);
        handle.setHandleType(handleType);
        handle.setHandleBy(handleBy);
        handle.setHandleName(handleName);
        handle.setHandleNote(remark);
        handle.setHandleTime(LocalDateTime.now());
        alertHandleMapper.insert(handle);
        return handle;
    }

    public List<AlertHandle> getHandlesByAlertId(Long alertId) {
        return alertHandleMapper.selectByAlertId(String.valueOf(alertId));
    }

    // ============================================================
    // 工具方法
    // ============================================================

    private String generateAlertNo() {
        return "ALT"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", ALERT_SEQ.incrementAndGet() % 1000);
    }

    private String generateNotifyNo() {
        return "NTF"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", NOTIFY_SEQ.incrementAndGet() % 1000);
    }

    private String generateHandleNo() {
        return "AHD"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", HANDLE_SEQ.incrementAndGet() % 1000);
    }
}
