package com.xwms.integration.express.service;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.xwms.integration.express.service.ExpressGetService;

import org.springframework.kafka.core.KafkaTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

/**
 * ExpressGetService fallback 回归测试
 *
 * <p>覆盖 R2: @SentinelResource fallback。验证
 * {@code sendGetRequestFallback()} 将失败请求写入死信队列 Kafka topic。
 */
@ExtendWith(MockitoExtension.class)
class ExpressGetServiceFallbackTest {

    private static final String DLQ_TOPIC = "wms-express-get-dlq";

    @Mock private KafkaTemplate<String, Map<String, Object>> kafkaTemplate;

    private ExpressGetService service;

    @BeforeEach
    void setUp() {
        service = new ExpressGetService(kafkaTemplate);
    }

    @Test
    void sendGetRequestFallback_writesToDLQTopic() {
        // Given
        Map<String, Object> request = new HashMap<>();
        request.put("orderNo", "EXP-20260904-001");
        BlockException blockException = new BlockException("testFallback") {};

        // When
        service.sendGetRequestFallback(request, blockException);

        // Then
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(kafkaTemplate).send(eq(DLQ_TOPIC), captor.capture());
        assertEquals(request, captor.getValue(), "应发送原始请求对象到死信队列");
    }
}
