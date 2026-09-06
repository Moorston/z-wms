package com.xwms.common.statemachine.listener;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 状态机事件发布器 状态变更时发布Kafka事件，供其他服务订阅
 *
 * <p>Topic: wms-state-events 消息格式: {machine, fromState, toState, event, timestamp, context}
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StateMachineEventPublisher {

    private static final String TOPIC = "wms-state-events";

    private final KafkaTemplate<String, Map<String, Object>> kafkaTemplate;

    /** 发布状态变更事件 */
    public void publish(
            String machine,
            String fromState,
            String toState,
            String event,
            Map<String, Object> context) {
        Map<String, Object> message = new HashMap<>();
        message.put("machine", machine);
        message.put("fromState", fromState);
        message.put("toState", toState);
        message.put("event", event);
        message.put("timestamp", LocalDateTime.now().toString());
        message.put("context", context != null ? context : new HashMap<>());

        try {
            String key =
                    machine
                            + ":"
                            + (context != null && context.get("bizId") != null
                                    ? context.get("bizId")
                                    : "unknown");
            kafkaTemplate.send(TOPIC, key, message);
            log.debug("[状态机事件] 已发布: {} {}->{} ({})", machine, fromState, toState, event);
        } catch (Exception e) {
            // 事件发布失败不影响主流程，只记录日志
            log.warn(
                    "[状态机事件] 发布失败: {} {}->{} ({}), error={}",
                    machine,
                    fromState,
                    toState,
                    event,
                    e.getMessage());
        }
    }
}
