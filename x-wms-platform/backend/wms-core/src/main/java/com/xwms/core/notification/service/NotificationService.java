package com.xwms.core.notification.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.common.exception.BizException;
import com.xwms.core.notification.entity.*;
import com.xwms.core.notification.enums.NotifyStatus;
import com.xwms.core.notification.mapper.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 消息推送与通知核心服务 包含: 模板管理/通知发送/规则触发/用户设置/重试机制 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotifyTemplateMapper templateMapper;
    private final NotifyRecordMapper recordMapper;
    private final NotifyRuleMapper ruleMapper;
    private final UserNotifySettingMapper settingMapper;

    private static final AtomicInteger RECORD_SEQ = new AtomicInteger(0);
    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final Pattern VAR_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");
    private static final ExpressionParser SPEL_PARSER = new SpelExpressionParser();

    // ============================================================

    // 1. 通知模板管理
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public NotifyTemplate createTemplate(NotifyTemplate template) {
        template.setEnabled(1);
        templateMapper.insert(template);
        log.info("创建通知模板: {}", template.getTemplateCode());
        return template;
    }

    public NotifyTemplate getTemplate(Long id) {
        NotifyTemplate tpl = templateMapper.selectById(id);
        if (tpl == null) throw new BizException("模板不存在: " + id);
        return tpl;
    }

    public NotifyTemplate getTemplateByCode(String code) {
        return templateMapper.selectByCode(code);
    }

    public Page<NotifyTemplate> pageTemplates(
            Page<NotifyTemplate> page, String type, String channel) {
        LambdaQueryWrapper<NotifyTemplate> wrapper = new LambdaQueryWrapper<>();
        if (type != null) wrapper.eq(NotifyTemplate::getNotifyType, type);
        if (channel != null) wrapper.eq(NotifyTemplate::getChannel, channel);
        wrapper.orderByDesc(NotifyTemplate::getCreatedAt);
        return templateMapper.selectPage(page, wrapper);
    }

    // ============================================================

    // 2. 发送通知 (核心方法)
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public NotifyRecord sendNotification(
            String templateCode,
            Map<String, Object> variables,
            String receiverType,
            String receiverId,
            String receiverName,
            String businessType,
            String businessNo,
            String priority) {
        // 1. 获取模板
        NotifyTemplate template = templateMapper.selectByCode(templateCode);
        if (template == null) {
            throw new BizException("通知模板不存在: " + templateCode);
        }

        // 2. 渲染模板
        String title = renderTemplate(template.getTitleTemplate(), variables);
        String content = renderTemplate(template.getContentTemplate(), variables);

        // 3. 创建通知记录
        NotifyRecord record = new NotifyRecord();
        record.setRecordNo(generateRecordNo());
        record.setTemplateCode(templateCode);
        record.setNotifyType(template.getNotifyType());
        record.setChannel(template.getChannel());
        record.setTitle(title);
        record.setContent(content);
        record.setSender("SYSTEM");
        record.setReceiverType(receiverType);
        record.setReceiverId(receiverId);
        record.setReceiverName(receiverName);
        record.setBusinessType(businessType);
        record.setBusinessNo(businessNo);
        record.setPriority(priority != null ? priority : "NORMAL");
        record.setStatus(NotifyStatus.PENDING.getCode());
        record.setRetryCount(0);
        record.setMaxRetry(3);
        record.setOwnerCodeCol(template.getOwnerCodeCol());
        record.setWarehouseCodeCol(template.getWarehouseCodeCol());
        recordMapper.insert(record);

        // 4. 站内信直接发送成功, 其他渠道异步发送
        if ("IN_APP".equals(template.getChannel())) {
            record.setStatus(NotifyStatus.SENT.getCode());
            record.setSendTime(LocalDateTime.now());
            recordMapper.updateById(record);
        }

        log.info(
                "发送通知: {}, 模板={}, 接收者={}, 渠道={}",
                record.getRecordNo(),
                templateCode,
                receiverName,
                template.getChannel());
        return record;
    }

    /** 渲染模板, 替换${变量}占位符 */
    private String renderTemplate(String template, Map<String, Object> variables) {
        if (template == null || variables == null) return template;
        Matcher matcher = VAR_PATTERN.matcher(template);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String varName = matcher.group(1);
            Object value = variables.get(varName);
            matcher.appendReplacement(
                    sb, value != null ? Matcher.quoteReplacement(value.toString()) : "");
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    // ============================================================

    // 3. 事件驱动通知 (规则触发)
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public void triggerEvent(
            String eventType,
            Map<String, Object> eventData,
            String receiverType,
            String receiverId,
            String receiverName) {
        // 查询该事件的所有启用规则
        List<NotifyRule> rules = ruleMapper.selectByEventType(eventType);
        for (NotifyRule rule : rules) {
            // 条件判断
            if (rule.getConditionExpr() != null && !rule.getConditionExpr().isEmpty()) {
                if (!evaluateCondition(rule.getConditionExpr(), eventData)) {
                    continue;
                }
            }
            // 发送通知
            sendNotification(
                    rule.getTemplateCode(),
                    eventData,
                    rule.getReceiverType() != null ? rule.getReceiverType() : receiverType,
                    rule.getReceiverId() != null ? rule.getReceiverId() : receiverId,
                    receiverName,
                    eventType,
                    eventData.get("businessNo") != null
                            ? eventData.get("businessNo").toString()
                            : null,
                    "NORMAL");
        }
        log.info("事件触发通知: {}, 匹配规则数={}", eventType, rules.size());
    }

    /** 评估Spring EL条件表达式 */
    private boolean evaluateCondition(String expr, Map<String, Object> data) {
        try {
            StandardEvaluationContext context = new StandardEvaluationContext();
            data.forEach(context::setVariable);
            Boolean result = SPEL_PARSER.parseExpression(expr).getValue(context, Boolean.class);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.warn("条件表达式评估失败: {}, 错误: {}", expr, e.getMessage());
            return false;
        }
    }

    // ============================================================

    // 4. 通知记录查询与操作
    // ============================================================

    public Page<NotifyRecord> pageRecords(
            Page<NotifyRecord> page, String receiverId, String status) {
        LambdaQueryWrapper<NotifyRecord> wrapper = new LambdaQueryWrapper<>();
        if (receiverId != null) wrapper.eq(NotifyRecord::getReceiverId, receiverId);
        if (status != null) wrapper.eq(NotifyRecord::getStatus, status);
        wrapper.orderByDesc(NotifyRecord::getCreatedTime);
        return recordMapper.selectPage(page, wrapper);
    }

    public List<NotifyRecord> getMyNotifications(String receiverId) {
        return recordMapper.selectByReceiver(receiverId);
    }

    public int getUnreadCount(String receiverId) {
        return recordMapper.countUnread(receiverId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void markAsRead(Long recordId) {
        NotifyRecord record = recordMapper.selectById(recordId);
        if (record == null) throw new BizException("通知不存在");
        record.setStatus(NotifyStatus.READ.getCode());
        record.setReadTime(LocalDateTime.now());
        recordMapper.updateById(record);
    }

    @Transactional(rollbackFor = Exception.class)
    public void markAllAsRead(String receiverId) {
        List<NotifyRecord> records =
                recordMapper.selectList(
                        new LambdaQueryWrapper<NotifyRecord>()
                                .eq(NotifyRecord::getReceiverId, receiverId)
                                .eq(NotifyRecord::getStatus, "SENT"));
        for (NotifyRecord r : records) {
            r.setStatus(NotifyStatus.READ.getCode());
            r.setReadTime(LocalDateTime.now());
            recordMapper.updateById(r);
        }
    }

    // ============================================================

    // 5. 失败重试 (PowerJob定时触发)
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public int retryFailedNotifications() {
        List<NotifyRecord> pending = recordMapper.selectPending();
        int success = 0;
        for (NotifyRecord record : pending) {
            try {
                // 模拟发送
                record.setStatus(NotifyStatus.SENDING.getCode());
                recordMapper.updateById(record);

                // 实际发送逻辑(调用短信/邮件/钉钉等API)
                // ...

                record.setStatus(NotifyStatus.SENT.getCode());
                record.setSendTime(LocalDateTime.now());
                recordMapper.updateById(record);
                success++;
            } catch (Exception e) {
                record.setRetryCount(record.getRetryCount() + 1);
                record.setErrorMsg(e.getMessage());
                if (record.getRetryCount() >= record.getMaxRetry()) {
                    record.setStatus(NotifyStatus.FAILED.getCode());
                }
                recordMapper.updateById(record);
                log.error(
                        "通知重试失败: {}, 次数={}, 错误={}",
                        record.getRecordNo(),
                        record.getRetryCount(),
                        e.getMessage());
            }
        }
        log.info("通知重试完成: 待发送={}, 成功={}", pending.size(), success);
        return success;
    }

    // ============================================================

    // 6. 通知规则管理
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public NotifyRule createRule(NotifyRule rule) {
        rule.setEnabled(1);
        ruleMapper.insert(rule);
        log.info("创建通知规则: {}", rule.getRuleCode());
        return rule;
    }

    public List<NotifyRule> getRulesByEvent(String eventType) {
        return ruleMapper.selectByEventType(eventType);
    }

    // ============================================================

    // 7. 用户通知设置
    // ============================================================

    public List<UserNotifySetting> getUserSettings(Long userId) {
        return settingMapper.selectByUserId(userId);
    }

    @Transactional(rollbackFor = Exception.class)
    public UserNotifySetting updateUserSetting(UserNotifySetting setting) {
        UserNotifySetting existing =
                settingMapper.selectOne(
                        new LambdaQueryWrapper<UserNotifySetting>()
                                .eq(UserNotifySetting::getUserId, setting.getUserId())
                                .eq(UserNotifySetting::getNotifyType, setting.getNotifyType())
                                .eq(UserNotifySetting::getChannel, setting.getChannel()));
        if (existing == null) {
            settingMapper.insert(setting);
        } else {
            setting.setId(existing.getId());
            settingMapper.updateById(setting);
        }
        return setting;
    }

    // ============================================================

    // 工具方法
    // ============================================================

    private String generateRecordNo() {
        return "NTF"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", RECORD_SEQ.incrementAndGet() % 1000);
    }

    /** 便捷方法: 库存不足告警 */
    public void sendStockLowAlert(
            String sku,
            String productName,
            String warehouse,
            BigDecimal currentQty,
            BigDecimal safetyStock,
            String receiverId,
            String receiverName) {
        Map<String, Object> vars = new HashMap<>();
        vars.put("sku", sku);
        vars.put("productName", productName);
        vars.put("warehouse", warehouse);
        vars.put("currentQty", currentQty);
        vars.put("safetyStock", safetyStock);
        sendNotification(
                "STOCK_LOW_ALERT",
                vars,
                "USER",
                receiverId,
                receiverName,
                "INVENTORY",
                sku,
                "HIGH");
    }

    /** 便捷方法: 质检不合格通知 */
    public void sendQcFailedAlert(
            String qcNo,
            String sku,
            String productName,
            String warehouse,
            String receiverId,
            String receiverName) {
        Map<String, Object> vars = new HashMap<>();
        vars.put("qcNo", qcNo);
        vars.put("sku", sku);
        vars.put("productName", productName);
        vars.put("warehouse", warehouse);
        sendNotification(
                "QC_FAILED_ALERT", vars, "USER", receiverId, receiverName, "QC", qcNo, "HIGH");
    }

    /** 便捷方法: 预约逾期通知 */
    public void sendAppointmentOverdueAlert(
            String appointNo,
            String vehicleNo,
            String warehouse,
            String receiverId,
            String receiverName) {
        Map<String, Object> vars = new HashMap<>();
        vars.put("appointNo", appointNo);
        vars.put("vehicleNo", vehicleNo);
        vars.put("warehouse", warehouse);
        sendNotification(
                "APPOINTMENT_OVERDUE",
                vars,
                "USER",
                receiverId,
                receiverName,
                "APPOINTMENT",
                appointNo,
                "NORMAL");
    }
}
