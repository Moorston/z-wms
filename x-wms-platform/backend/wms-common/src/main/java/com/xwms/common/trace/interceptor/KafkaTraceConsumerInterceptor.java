package com.xwms.common.trace.interceptor;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerInterceptor;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.header.Header;

import com.xwms.common.tenant.context.OwnerContext;
import com.xwms.common.trace.util.TraceIdUtil;

/**
 * Kafka消费者TraceId拦截器 从Kafka消息头中提取TraceId/SpanId和ownerCode，设置到MDC和OwnerContext
 * 注意：此拦截器在poll()返回后调用，需要在@KafkaListener方法中继续保持
 */
public class KafkaTraceConsumerInterceptor implements ConsumerInterceptor<Object, Object> {

    @Override
    public ConsumerRecords<Object, Object> onConsume(ConsumerRecords<Object, Object> records) {
        // 从第一条记录中提取TraceId和ownerCode（同一批消息通常来自同一生产者）
        records.forEach(
                record -> {
                    // 1. TraceId透传
                    Header traceHeader = record.headers().lastHeader(TraceIdUtil.TRACE_ID_HEADER);
                    if (traceHeader != null) {
                        String traceId = new String(traceHeader.value(), StandardCharsets.UTF_8);
                        TraceIdUtil.setTraceId(traceId);

                        Header spanHeader = record.headers().lastHeader(TraceIdUtil.SPAN_ID_HEADER);
                        if (spanHeader != null) {
                            String spanId = new String(spanHeader.value(), StandardCharsets.UTF_8);
                            TraceIdUtil.setSpanId(spanId);
                        } else {
                            TraceIdUtil.setSpanId(TraceIdUtil.generateSpanId());
                        }
                    } else {
                        // 没有TraceId则生成新的
                        TraceIdUtil.initTraceId();
                    }

                    // 2. 货主编码透传（多租户隔离）
                    Header ownerHeader =
                            record.headers().lastHeader(KafkaTraceProducerInterceptor.OWNER_HEADER);
                    if (ownerHeader != null) {
                        String ownerCode = new String(ownerHeader.value(), StandardCharsets.UTF_8);
                        OwnerContext.set(ownerCode);
                    }
                });
        return records;
    }

    @Override
    public void onCommit(Map offsets) {
        // 无需处理
    }

    @Override
    public void close() {
        // 消费结束清除MDC和OwnerContext
        TraceIdUtil.clear();
        OwnerContext.clear();
    }

    @Override
    public void configure(Map<String, ?> configs) {
        // 无需配置
    }
}
