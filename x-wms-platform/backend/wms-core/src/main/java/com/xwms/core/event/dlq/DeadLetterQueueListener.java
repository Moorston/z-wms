package com.xwms.core.event.dlq;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import com.xwms.common.trace.util.TraceIdUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 死信队列消费者
 *
 * <p>监听Topic: wms-dlq（所有失败消息的最终归宿）
 *
 * <p>处理策略： 1. 记录失败消息到数据库（DLQ_MESSAGE表） 2. 支持手动重试（管理员触发） 3. 支持自动重试（指数退避，最多3次） 4.
 * 超过最大重试次数标记为永久失败，告警通知
 *
 * <p>重试Topic设计： - wms-retry-1: 1分钟后重试 - wms-retry-2: 5分钟后重试 - wms-retry-3: 30分钟后重试 - wms-dlq: 最终死信
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeadLetterQueueListener {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /** 消息重试计数（内存，生产环境应持久化） */
    private final Map<String, AtomicInteger> retryCountMap = new ConcurrentHashMap<>();

    private static final int MAX_RETRY = 3;

    @KafkaListener(
            topics = "wms-dlq",
            containerFactory = "dlqKafkaListenerFactory",
            groupId = "wms-dlq-group")
    public void onDeadLetter(ConsumerRecord<String, Object> record, Acknowledgment ack) {
        String traceId = extractTraceId(record);
        TraceIdUtil.setTraceId(traceId != null ? traceId : TraceIdUtil.generateTraceId());

        try {
            String messageKey = record.key();
            String originalTopic = extractOriginalTopic(record);
            log.error(
                    "收到死信消息: key={}, originalTopic={}, partition={}, offset={}",
                    messageKey,
                    originalTopic,
                    record.partition(),
                    record.offset());

            // 1. 记录到死信消息表
            saveDeadLetterMessage(record, originalTopic);

            // 2. 检查重试次数
            AtomicInteger retryCount =
                    retryCountMap.computeIfAbsent(messageKey, k -> new AtomicInteger(0));
            int currentRetry = retryCount.incrementAndGet();

            if (currentRetry <= MAX_RETRY) {
                // 3. 发送到重试Topic（延迟由消费者端控制）
                String retryTopic = "wms-retry-" + currentRetry;
                log.info("消息第{}次重试: key={}, retryTopic={}", currentRetry, messageKey, retryTopic);
                kafkaTemplate.send(retryTopic, messageKey, record.value());
            } else {
                // 4. 超过最大重试次数，标记永久失败
                log.error("消息超过最大重试次数，标记永久失败: key={}, retryCount={}", messageKey, currentRetry);
                markAsPermanentFailure(messageKey, record);
                // 触发告警
                triggerAlert(messageKey, originalTopic, record.value());
            }

            ack.acknowledge();

        } catch (Exception e) {
            log.error("死信消息处理失败", e);
        } finally {
            TraceIdUtil.clear();
        }
    }

    /** 保存死信消息到数据库 */
    private void saveDeadLetterMessage(
            ConsumerRecord<String, Object> record, String originalTopic) {
        // TODO: 插入DLQ_MESSAGE表
        // fields: message_key, original_topic, message_value, error_msg, retry_count, status,
        // created_at
        log.info("保存死信消息: key={}, topic={}", record.key(), originalTopic);
    }

    /** 标记为永久失败 */
    private void markAsPermanentFailure(String messageKey, ConsumerRecord<String, Object> record) {
        // TODO: 更新DLQ_MESSAGE状态为PERMANENT_FAILED
        log.error("永久失败消息: key={}", messageKey);
    }

    /** 触发告警 */
    private void triggerAlert(String messageKey, String topic, Object message) {
        // TODO: 发送告警（钉钉/企业微信/邮件）
        log.error(
                "告警: 死信消息永久失败, key={}, topic={}, time={}", messageKey, topic, LocalDateTime.now());
    }

    private String extractTraceId(ConsumerRecord<String, Object> record) {
        try {
            var header = record.headers().lastHeader(TraceIdUtil.TRACE_ID_HEADER);
            return header != null ? new String(header.value()) : null;
        } catch (Exception e) {
            return null;
        }
    }

    private String extractOriginalTopic(ConsumerRecord<String, Object> record) {
        try {
            var header = record.headers().lastHeader("original-topic");
            return header != null ? new String(header.value()) : "unknown";
        } catch (Exception e) {
            return "unknown";
        }
    }
}
