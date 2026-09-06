package com.xwms.core.event;

import java.util.Map;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 事件发布服务 统一发布WMS领域事件到Kafka，用于： 1. 异步通知下游（analytics/integration/AI） 2. 事件溯源（批次追踪/库存流水） 3. 系统解耦 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventPublisherService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /** 发布库存事件 */
    public void publishInventoryEvent(
            String eventType, String aggregateId, Map<String, Object> payload) {
        publish("wms-inventory-events", aggregateId, eventType, payload);
    }

    /** 发布批次事件 */
    public void publishBatchEvent(
            String eventType, String aggregateId, Map<String, Object> payload) {
        publish("wms-batch-events", aggregateId, eventType, payload);
    }

    /** 发布订单事件 */
    public void publishOrderEvent(
            String eventType, String aggregateId, Map<String, Object> payload) {
        publish("wms-order-inbound", aggregateId, eventType, payload);
    }

    /** 发布集成通知事件 */
    public void publishIntegrationEvent(
            String eventType, String aggregateId, Map<String, Object> payload) {
        publish("wms-integration-notify", aggregateId, eventType, payload);
    }

    /** 发布AI事件 */
    public void publishAiEvent(String eventType, String aggregateId, Map<String, Object> payload) {
        publish("wms-ai-events", aggregateId, eventType, payload);
    }

    private void publish(String topic, String key, String eventType, Map<String, Object> payload) {
        Map<String, Object> event =
                Map.of(
                        "eventId",
                        java.util.UUID.randomUUID().toString(),
                        "eventType",
                        eventType,
                        "aggregateId",
                        key,
                        "eventTime",
                        java.time.LocalDateTime.now().toString(),
                        "payload",
                        payload != null ? payload : Map.of());
        kafkaTemplate.send(topic, key, event);
        log.debug("事件发布: topic={}, type={}, key={}", topic, eventType, key);
    }
}
