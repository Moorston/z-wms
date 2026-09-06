package com.xwms.integration.api.async;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import com.xwms.common.trace.util.TraceIdUtil;
import com.xwms.integration.api.service.ApiPlatformService;
import com.xwms.integration.core.model.ApiRequest;
import com.xwms.integration.core.model.ApiResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 异步API消费者
 *
 * <p>监听Topic: wms-api-async 处理流程： 1. 从Kafka消费异步API请求 2. 标记任务为PROCESSING 3. 调用ApiPlatformService同步执行
 * 4. 标记任务成功/失败 5. 触发回调通知
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApiAsyncListener {

    private final ApiAsyncService asyncService;
    private final ApiPlatformService platformService;

    @KafkaListener(topics = "wms-api-async", groupId = "wms-api-async-group", concurrency = "3")
    public void onAsyncRequest(ConsumerRecord<String, Object> record, Acknowledgment ack) {
        String traceId = extractTraceId(record);
        TraceIdUtil.setTraceId(traceId != null ? traceId : TraceIdUtil.generateTraceId());

        String requestId = record.key();
        try {
            log.info("处理异步API请求: requestId={}", requestId);

            // 1. 标记处理中
            asyncService.markProcessing(requestId);

            // 2. 解析请求
            ApiRequest request = (ApiRequest) record.value();

            // 3. 同步执行（复用ApiPlatformService的核心逻辑）
            ApiResponse response = platformService.invoke(request);

            // 4. 标记结果
            if (response.isSuccess()) {
                asyncService.markSuccess(requestId, response.getData());
            } else {
                asyncService.markFailed(requestId, response.getMessage());
            }

            ack.acknowledge();

        } catch (Exception e) {
            log.error("异步API处理失败: requestId={}", requestId, e);
            asyncService.markFailed(requestId, e.getMessage());
            ack.acknowledge(); // 异步任务失败也提交，避免阻塞
        } finally {
            TraceIdUtil.clear();
        }
    }

    private String extractTraceId(ConsumerRecord<String, Object> record) {
        try {
            var header = record.headers().lastHeader(TraceIdUtil.TRACE_ID_HEADER);
            return header != null ? new String(header.value()) : null;
        } catch (Exception e) {
            return null;
        }
    }
}
