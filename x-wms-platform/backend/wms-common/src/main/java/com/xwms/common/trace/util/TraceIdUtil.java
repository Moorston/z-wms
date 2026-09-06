package com.xwms.common.trace.util;

import java.util.UUID;

import org.slf4j.MDC;

/**
 * TraceId工具类 基于MDC（Mapped Diagnostic Context）实现全链路TraceId透传
 *
 * <p>TraceId传递链路： HTTP请求 → MDC → 日志输出 → Feign调用 → 下游服务 → Kafka消息 → 消费者
 */
public class TraceIdUtil {

    public static final String TRACE_ID = "traceId";
    public static final String SPAN_ID = "spanId";
    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    public static final String SPAN_ID_HEADER = "X-Span-Id";

    /** 生成TraceId（UUID去掉横线，32位） */
    public static String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /** 生成SpanId（短UUID，16位） */
    public static String generateSpanId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    /** 获取当前TraceId */
    public static String getTraceId() {
        return MDC.get(TRACE_ID);
    }

    /** 获取当前SpanId */
    public static String getSpanId() {
        return MDC.get(SPAN_ID);
    }

    /** 设置TraceId到MDC */
    public static void setTraceId(String traceId) {
        MDC.put(TRACE_ID, traceId);
    }

    /** 设置SpanId到MDC */
    public static void setSpanId(String spanId) {
        MDC.put(SPAN_ID, spanId);
    }

    /**
     * 初始化TraceId（如果不存在则生成）
     *
     * @return TraceId
     */
    public static String initTraceId() {
        String traceId = MDC.get(TRACE_ID);
        if (traceId == null || traceId.isEmpty()) {
            traceId = generateTraceId();
            MDC.put(TRACE_ID, traceId);
        }
        // 每次请求生成新的SpanId
        MDC.put(SPAN_ID, generateSpanId());
        return traceId;
    }

    /** 从请求头获取或生成TraceId */
    public static String initTraceId(String traceIdFromHeader) {
        if (traceIdFromHeader != null && !traceIdFromHeader.isEmpty()) {
            MDC.put(TRACE_ID, traceIdFromHeader);
        } else {
            MDC.put(TRACE_ID, generateTraceId());
        }
        MDC.put(SPAN_ID, generateSpanId());
        return MDC.get(TRACE_ID);
    }

    /** 清除MDC（请求结束时调用，防止内存泄漏） */
    public static void clear() {
        MDC.remove(TRACE_ID);
        MDC.remove(SPAN_ID);
    }

    /** 异步线程传递TraceId 用法：CompletableFuture.runAsync(TraceIdUtil.wrap(() -> {...})) */
    public static Runnable wrap(Runnable runnable) {
        String traceId = getTraceId();
        String spanId = getSpanId();
        return () -> {
            try {
                if (traceId != null) MDC.put(TRACE_ID, traceId);
                if (spanId != null) MDC.put(SPAN_ID, spanId);
                runnable.run();
            } finally {
                clear();
            }
        };
    }
}
