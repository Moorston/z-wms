package com.xwms.core.event.dlq;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 重试消费者
 *
 * <p>监听3个重试Topic，实现指数退避重试： - wms-retry-1: 延迟1分钟 - wms-retry-2: 延迟5分钟 - wms-retry-3: 延迟30分钟
 *
 * <p>重试策略： 1. 从重试Topic消费消息 2. 等待指定延迟时间 3. 重新发送到原始Topic 4. 如果再次失败，进入下一级重试或死信队列
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RetryMessageListener {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /** 第1次重试（延迟1分钟） */
    @KafkaListener(topics = "wms-retry-1", groupId = "wms-retry-group")
    public void onRetry1(ConsumerRecord<String, Object> record, Acknowledgment ack) {
        retryWithDelay(record, ack, 1, 60_000); // 1分钟
    }

    /** 第2次重试（延迟5分钟） */
    @KafkaListener(topics = "wms-retry-2", groupId = "wms-retry-group")
    public void onRetry2(ConsumerRecord<String, Object> record, Acknowledgment ack) {
        retryWithDelay(record, ack, 2, 300_000); // 5分钟
    }

    /** 第3次重试（延迟30分钟） */
    @KafkaListener(topics = "wms-retry-3", groupId = "wms-retry-group")
    public void onRetry3(ConsumerRecord<String, Object> record, Acknowledgment ack) {
        retryWithDelay(record, ack, 3, 1_800_000); // 30分钟
    }

    private void retryWithDelay(
            ConsumerRecord<String, Object> record,
            Acknowledgment ack,
            int retryLevel,
            long delayMs) {
        try {
            log.info("第{}次重试消息: key={}, 延迟{}ms", retryLevel, record.key(), delayMs);

            // 延迟处理（虚拟线程不阻塞其他请求）
            Thread.sleep(delayMs);

            // 获取原始Topic
            String originalTopic = extractOriginalTopic(record);
            log.info("重新发送到原始Topic: {} , key={}", originalTopic, record.key());

            // 重新发送到原始Topic
            kafkaTemplate.send(originalTopic, record.key(), record.value());

            ack.acknowledge();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("重试延迟被中断", e);
        } catch (Exception e) {
            log.error("重试发送失败，进入死信队列: key={}", record.key(), e);
            // 发送到死信队列
            kafkaTemplate.send("wms-dlq", record.key(), record.value());
            ack.acknowledge();
        }
    }

    private String extractOriginalTopic(ConsumerRecord<String, Object> record) {
        try {
            var header = record.headers().lastHeader("original-topic");
            return header != null ? new String(header.value()) : "wms-inventory-events";
        } catch (Exception e) {
            return "wms-inventory-events";
        }
    }
}
