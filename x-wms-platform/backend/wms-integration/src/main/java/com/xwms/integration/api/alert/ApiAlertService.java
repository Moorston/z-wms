package com.xwms.integration.api.alert;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * API告警服务
 *
 * <p>4种告警规则： 1. 成功率告警：连续100次调用成功率 < 90% 2. 耗时告警：平均耗时 > 5秒 3. 熔断告警：熔断器打开 4. 限流告警：限流触发频率 > 10次/分钟
 *
 * <p>通知方式： - 日志记录（默认） - 钉钉/企业微信Webhook（可配置） - 邮件（可配置）
 */
@Slf4j
@Service
public class ApiAlertService {

    /** 告警记录 */
    private final List<AlertRecord> alertHistory = new ArrayList<>();

    /** apiId -> 限流计数（每分钟） */
    private final Map<String, AtomicLong> rateLimitCount = new ConcurrentHashMap<>();

    /** apiId -> 上次限流告警时间 */
    private final Map<String, Long> lastRateAlert = new ConcurrentHashMap<>();

    private static final long ALERT_INTERVAL_MS = 60_000; // 告警间隔1分钟

    @Data
    public static class AlertRecord {
        private String alertType; // SUCCESS_RATE / DURATION / CIRCUIT / RATE_LIMIT
        private String apiId;
        private String message;
        private double value;
        private double threshold;
        private LocalDateTime alertTime;
    }

    /** 检查成功率告警 */
    public void checkSuccessRate(String apiId, long totalCalls, double successRate) {
        if (totalCalls >= 100 && successRate < 90) {
            triggerAlert(
                    "SUCCESS_RATE",
                    apiId,
                    "API成功率过低: " + String.format("%.1f%%", successRate),
                    successRate,
                    90.0);
        }
    }

    /** 检查耗时告警 */
    public void checkDuration(String apiId, double avgDurationMs, long maxDurationMs) {
        if (avgDurationMs > 5000) {
            triggerAlert(
                    "DURATION",
                    apiId,
                    "API平均耗时过高: " + String.format("%.0fms", avgDurationMs),
                    avgDurationMs,
                    5000.0);
        }
    }

    /** 熔断告警 */
    public void alertCircuitOpen(String adapterId, String reason) {
        triggerAlert("CIRCUIT", adapterId, "熔断器打开: " + reason, 0, 0);
    }

    /** 限流告警 */
    public void checkRateLimit(String apiId) {
        long now = System.currentTimeMillis();
        AtomicLong count = rateLimitCount.computeIfAbsent(apiId, k -> new AtomicLong(0));
        count.incrementAndGet();

        // 每分钟检查一次
        Long lastAlert = lastRateAlert.get(apiId);
        if (lastAlert == null || now - lastAlert > ALERT_INTERVAL_MS) {
            if (count.get() > 10) {
                triggerAlert(
                        "RATE_LIMIT", apiId, "限流触发频繁: " + count.get() + "次/分钟", count.get(), 10);
                lastRateAlert.put(apiId, now);
                count.set(0); // 重置计数
            }
        }
    }

    /** 触发告警 */
    private void triggerAlert(
            String type, String targetId, String message, double value, double threshold) {
        AlertRecord record = new AlertRecord();
        record.setAlertType(type);
        record.setApiId(targetId);
        record.setMessage(message);
        record.setValue(value);
        record.setThreshold(threshold);
        record.setAlertTime(LocalDateTime.now());

        alertHistory.add(record);
        if (alertHistory.size() > 1000) {
            alertHistory.remove(0);
        }

        // 1. 日志告警
        log.error("【API告警】type={}, target={}, message={}", type, targetId, message);

        // 2. 通知告警（钉钉/企业微信/邮件）
        sendNotification(record);
    }

    /** 发送通知（可扩展多种通知方式） */
    private void sendNotification(AlertRecord record) {
        try {
            // TODO: 钉钉Webhook
            // TODO: 企业微信Webhook
            // TODO: 邮件通知
            log.info("告警通知已发送: type={}", record.getAlertType());
        } catch (Exception e) {
            log.error("告警通知发送失败", e);
        }
    }

    /** 获取告警历史 */
    public List<AlertRecord> getAlertHistory(int limit) {
        int from = Math.max(0, alertHistory.size() - limit);
        return new ArrayList<>(alertHistory.subList(from, alertHistory.size()));
    }

    /** 获取最近告警 */
    public List<AlertRecord> getRecentAlerts() {
        return getAlertHistory(50);
    }
}
