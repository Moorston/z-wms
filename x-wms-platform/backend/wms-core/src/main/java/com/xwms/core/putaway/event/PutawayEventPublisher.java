package com.xwms.core.putaway.event;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 上架事件发布服务 通过Kafka发布上架领域事件，实现异步解耦
 *
 * <p>Topic定义： - wms.putaway.task.created 上架任务生成 - wms.putaway.task.completed 上架完成 -
 * wms.putaway.task.exception 上架异常 - wms.putaway.task.override 人工覆盖
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PutawayEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public static final String TOPIC_TASK_CREATED = "wms.putaway.task.created";
    public static final String TOPIC_TASK_COMPLETED = "wms.putaway.task.completed";
    public static final String TOPIC_TASK_EXCEPTION = "wms.putaway.task.exception";
    public static final String TOPIC_TASK_OVERRIDE = "wms.putaway.task.override";

    /** 发布上架任务生成事件 */
    public void publishTaskCreated(PutawayEvent event) {
        event.setEventType("TASK_CREATED");
        publish(TOPIC_TASK_CREATED, event);
    }

    /** 发布上架完成事件 */
    public void publishTaskCompleted(PutawayEvent event) {
        event.setEventType("TASK_COMPLETED");
        publish(TOPIC_TASK_COMPLETED, event);
    }

    /** 发布上架异常事件 */
    public void publishTaskException(PutawayEvent event) {
        event.setEventType("TASK_EXCEPTION");
        publish(TOPIC_TASK_EXCEPTION, event);
    }

    /** 发布人工覆盖事件 */
    public void publishTaskOverride(PutawayEvent event) {
        event.setEventType("TASK_OVERRIDE");
        publish(TOPIC_TASK_OVERRIDE, event);
    }

    /** 构建事件对象 */
    public PutawayEvent buildEvent(String taskNo, String warehouseCode, String operator) {
        return PutawayEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventTime(LocalDateTime.now())
                .taskNo(taskNo)
                .warehouseCode(warehouseCode)
                .operator(operator)
                .sourceService("wms-core")
                .build();
    }

    /** 异步发布事件 */
    private void publish(String topic, PutawayEvent event) {
        if (event.getEventId() == null) {
            event.setEventId(UUID.randomUUID().toString());
        }
        if (event.getEventTime() == null) {
            event.setEventTime(LocalDateTime.now());
        }

        String key = event.getTaskNo() != null ? event.getTaskNo() : event.getEventId();

        try {
            CompletableFuture<SendResult<String, Object>> future =
                    kafkaTemplate.send(topic, key, event);
            future.whenComplete(
                    (result, ex) -> {
                        if (ex != null) {
                            log.error(
                                    "上架事件发布失败: topic={}, taskNo={}, error={}",
                                    topic,
                                    event.getTaskNo(),
                                    ex.getMessage());
                        } else {
                            log.debug(
                                    "上架事件发布成功: topic={}, taskNo={}, partition={}",
                                    topic,
                                    event.getTaskNo(),
                                    result != null ? result.getRecordMetadata().partition() : -1);
                        }
                    });
        } catch (Exception e) {
            log.error(
                    "上架事件发布异常: topic={}, taskNo={}, error={}",
                    topic,
                    event.getTaskNo(),
                    e.getMessage());
        }
    }
}
