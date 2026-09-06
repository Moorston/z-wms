package com.xwms.core.event.listener;

import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.support.Acknowledgment;

import com.xwms.common.tenant.context.OwnerContext;
import com.xwms.common.trace.util.TraceIdUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * Kafka事件消费者基类 核心能力： 1. TraceId透传（从消息头提取TraceId设置MDC） 2. 货主上下文透传（从消息头提取ownerCode设置OwnerContext） 3.
 * 统一异常处理（失败发送到死信队列） 4. 手动提交offset（处理成功才提交） 5. 消费耗时统计
 */
@Slf4j
public abstract class BaseEventListener {

    private static final String OWNER_HEADER = "X-Owner-Code";

    /** 处理消息前的准备工作（TraceId/货主上下文/日志） */
    protected void beforeProcess(ConsumerRecord<String, ?> record) {
        // 1. 从消息头提取TraceId
        String traceId = extractHeader(record, TraceIdUtil.TRACE_ID_HEADER);
        if (traceId != null) {
            TraceIdUtil.setTraceId(traceId);
        } else {
            TraceIdUtil.initTraceId();
        }
        String spanId = extractHeader(record, TraceIdUtil.SPAN_ID_HEADER);
        if (spanId != null) {
            TraceIdUtil.setSpanId(spanId);
        } else {
            TraceIdUtil.setSpanId(TraceIdUtil.generateSpanId());
        }

        // 2. 从消息头提取货主编码（多租户隔离）
        String ownerCode = extractHeader(record, OWNER_HEADER);
        if (ownerCode != null && !ownerCode.isEmpty()) {
            OwnerContext.set(ownerCode);
        }

        log.info(
                "消费消息开始: topic={}, partition={}, offset={}, key={}, owner={}",
                record.topic(),
                record.partition(),
                record.offset(),
                record.key(),
                OwnerContext.get());
    }

    /** 处理消息后的清理工作 */
    protected void afterProcess(ConsumerRecord<String, ?> record, long startTime, boolean success) {
        long cost = System.currentTimeMillis() - startTime;
        log.info(
                "消费消息结束: topic={}, offset={}, success={}, cost={}ms",
                record.topic(),
                record.offset(),
                success,
                cost);
        TraceIdUtil.clear();
        OwnerContext.clear(); // 必须清除，防止线程池复用导致数据串货主
    }

    /** 提交offset */
    protected void ack(Acknowledgment acknowledgment) {
        if (acknowledgment != null) {
            acknowledgment.acknowledge();
        }
    }

    /** 从消息头提取值 */
    private String extractHeader(ConsumerRecord<String, ?> record, String headerKey) {
        try {
            var headers = record.headers();
            if (headers != null) {
                var header = headers.lastHeader(headerKey);
                if (header != null) {
                    return new String(header.value());
                }
            }
        } catch (Exception e) {
            log.warn("提取消息头失败: {}", headerKey, e);
        }
        return null;
    }

    /** 转换事件数据为Map（JsonDeserializer默认返回LinkedHashMap） */
    @SuppressWarnings("unchecked")
    protected Map<String, Object> toMap(Object value) {
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }
        return null;
    }

    /** 获取事件类型 */
    protected String getEventType(Map<String, Object> data) {
        if (data == null) return "UNKNOWN";
        Object eventType = data.get("eventType");
        return eventType != null ? eventType.toString() : "UNKNOWN";
    }
}
