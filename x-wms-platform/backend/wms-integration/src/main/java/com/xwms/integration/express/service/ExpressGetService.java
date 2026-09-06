package com.xwms.integration.express.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;

import com.xwms.common.resilience.FallbackHandler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 快递单批量获取服务 方案：Kafka异步削峰 + 虚拟线程并发调用外部API + 令牌桶限流 决策依据：快递单获取是高并发外部IO，需要持久化+重试+死信，Kafka比纯线程池更可靠 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExpressGetService {

    private final KafkaTemplate<String, Map<String, Object>> kafkaTemplate;

    /** 虚拟线程执行器（Java 21，无需调优线程池大小） */
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    /** 批量聚合缓冲区 */
    private final List<Map<String, Object>> buffer = new CopyOnWriteArrayList<>();

    private volatile long lastFlush = System.currentTimeMillis();
    private static final int BATCH_SIZE = 100;
    private static final long FLUSH_INTERVAL_MS = 3000;

    /** 发送快递单获取请求到Kafka 限流降级：熔断(30%异常率)+限流(10/s) 注：Kafka异步削峰是第一道防线，Sentinel是第二道防线 */
    @SentinelResource(value = "expressApi", fallback = "sendGetRequestFallback")
    public void sendGetRequest(Map<String, Object> request) {
        kafkaTemplate.send("wms-express-get", request.get("orderNo").toString(), request);
    }

    /** 快递单获取降级方法 */
    public void sendGetRequestFallback(Map<String, Object> request, BlockException e) {
        log.error("[快递单获取降级] orderNo={}, error={}", request.get("orderNo"), e.getMessage());
        FallbackHandler.simple("快递单获取", e);
        // 降级：直接写入死信队列，待人工处理
        kafkaTemplate.send("wms-express-get-dlq", request);
    }

    /** Kafka消费者：批量聚合后并发调用快递API */
    @KafkaListener(
            topics = "wms-express-get",
            groupId = "express-get-group",
            concurrency = "3",
            batch = "true")
    public void onMessage(List<Map<String, Object>> messages) {
        buffer.addAll(messages);
        // 达到批量大小或时间间隔，触发批量获取
        if (buffer.size() >= BATCH_SIZE
                || System.currentTimeMillis() - lastFlush > FLUSH_INTERVAL_MS) {
            flush();
        }
    }

    /** 批量刷出：虚拟线程并发调用快递API */
    private void flush() {
        List<Map<String, Object>> batch = new ArrayList<>(buffer);
        buffer.clear();
        lastFlush = System.currentTimeMillis();

        if (batch.isEmpty()) return;
        log.info("批量获取快递单: size={}", batch.size());

        // 虚拟线程并发，每个请求独立调用外部API
        List<CompletableFuture<Void>> futures =
                batch.stream()
                        .map(req -> CompletableFuture.runAsync(() -> getExpressNo(req), executor))
                        .toList();

        // 等待全部完成（虚拟线程不阻塞平台线程）
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }

    /** 调用快递API获取单号（含重试+熔断+限流） */
    private void getExpressNo(Map<String, Object> req) {
        int maxRetry = 3;
        for (int i = 0; i < maxRetry; i++) {
            try {
                // TODO: 调用具体快递API（顺丰/京东/菜鸟等适配器）
                // 令牌桶限流保护外部接口
                log.info("获取快递单: orderNo={}, attempt={}", req.get("orderNo"), i + 1);
                // 成功后写回单号，触发打印事件
                return;
            } catch (Exception e) {
                log.warn(
                        "获取快递单失败: orderNo={}, attempt={}, error={}",
                        req.get("orderNo"),
                        i + 1,
                        e.getMessage());
                if (i == maxRetry - 1) {
                    // 3次失败→死信队列→人工介入
                    kafkaTemplate.send("wms-express-get-dlq", req);
                    log.error("快递单获取进入死信: orderNo={}", req.get("orderNo"));
                }
                try {
                    Thread.sleep(1000L * (i + 1));
                } catch (InterruptedException ignored) {
                }
            }
        }
    }

    /** 批量获取快递单号（发送到Kafka异步处理，返回批次ID） */
    public String batchGetTrackingNo(List<Map<String, Object>> orders) {
        String batchId = "BATCH-" + System.currentTimeMillis();
        for (Map<String, Object> order : orders) {
            order.put("batchId", batchId);
            sendGetRequest(order);
        }
        log.info("批量获取快递单号已提交: batchId={}, size={}", batchId, orders.size());
        return batchId;
    }

    /** 查询批量获取结果（占位，TODO: 从结果存储查询） */
    public Map<String, Object> getBatchResult(String batchId) {
        log.info("查询批量获取结果: batchId={}", batchId);
        // TODO: 从结果存储（Redis/DB）查询批次状态与各单号结果
        return Map.of("batchId", batchId, "status", "PROCESSING");
    }

    /** 同步获取单个快递单号（占位，TODO: 同步调用外部快递API） */
    public Map<String, Object> getTrackingNoSync(Map<String, Object> order) {
        log.info("同步获取快递单号: orderNo={}", order.get("orderNo"));
        // TODO: 同步调用外部快递API获取单号（非Kafka异步链路）
        return Map.of("orderNo", order.get("orderNo"), "trackingNo", "", "status", "PENDING");
    }

    /** 批量打印快递单（占位，TODO: 生成打印任务） */
    public String batchPrint(List<String> orderNos) {
        String printTaskId = "PRINT-" + System.currentTimeMillis();
        log.info("批量打印快递单: printTaskId={}, size={}", printTaskId, orderNos.size());
        // TODO: 生成打印任务下发到打印服务
        return printTaskId;
    }
}
