package com.xwms.common.trace.interceptor;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.apache.kafka.clients.producer.ProducerInterceptor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.header.internals.RecordHeaders;

import com.xwms.common.tenant.context.OwnerContext;
import com.xwms.common.trace.util.TraceIdUtil;

/** Kafka生产者TraceId拦截器 将当前MDC中的TraceId/SpanId和OwnerContext中的ownerCode写入Kafka消息头， 实现异步链路追踪和多租户隔离透传 */
public class KafkaTraceProducerInterceptor implements ProducerInterceptor<Object, Object> {

    public static final String OWNER_HEADER = "X-Owner-Code";

    @Override
    public ProducerRecord<Object, Object> onSend(ProducerRecord<Object, Object> record) {
        RecordHeaders headers = (RecordHeaders) record.headers();

        // 1. TraceId透传
        String traceId = TraceIdUtil.getTraceId();
        String spanId = TraceIdUtil.getSpanId();
        if (traceId != null) {
            headers.add(TraceIdUtil.TRACE_ID_HEADER, traceId.getBytes(StandardCharsets.UTF_8));
            if (spanId != null) {
                headers.add(TraceIdUtil.SPAN_ID_HEADER, spanId.getBytes(StandardCharsets.UTF_8));
            }
        }

        // 2. 货主编码透传（多租户隔离）
        String ownerCode = OwnerContext.get();
        if (ownerCode != null) {
            headers.add(OWNER_HEADER, ownerCode.getBytes(StandardCharsets.UTF_8));
        }

        return record;
    }

    @Override
    public void onAcknowledgement(RecordMetadata metadata, Exception exception) {
        // 无需处理
    }

    @Override
    public void close() {
        // 无需处理
    }

    @Override
    public void configure(Map<String, ?> configs) {
        // 无需配置
    }
}
