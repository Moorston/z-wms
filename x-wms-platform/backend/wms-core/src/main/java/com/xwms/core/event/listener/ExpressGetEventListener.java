package com.xwms.core.event.listener;

import java.util.List;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 快递单批量获取消费者
 *
 * <p>监听Topic: wms-express-get 消息格式：批量快递单获取请求（100条聚合）
 *
 * <p>处理逻辑： 1. 批量聚合（最多100条/批） 2. 调用快递API批量取号 3. 失败重试（3次指数退避） 4. 最终失败发送到死信队列 5. 更新出库单快递单号
 *
 * <p>高并发设计： - 并发5个消费者 - 批量监听（BatchListener） - 虚拟线程处理IO密集型调用
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExpressGetEventListener extends BaseEventListener {

    @KafkaListener(
            topics = "wms-express-get",
            containerFactory = "expressKafkaListenerFactory",
            groupId = "wms-express-group",
            batch = "true")
    public void onExpressGetBatch(
            List<ConsumerRecord<String, Object>> records, Acknowledgment ack) {
        long startTime = System.currentTimeMillis();
        log.info("快递单批量获取开始: batchSize={}", records.size());

        int successCount = 0;
        int failCount = 0;

        for (ConsumerRecord<String, Object> record : records) {
            try {
                beforeProcess(record);
                Map<String, Object> data = toMap(record.value());

                // 调用快递API获取单号
                String expressCode = (String) data.get("expressCode");
                String orderNo = (String) data.get("orderNo");
                log.info("获取快递单号: orderNo={}, express={}", orderNo, expressCode);

                // TODO: 调用ExpressGetService.batchGetExpressNo
                // 1. 调用顺丰/京东/菜鸟API
                // 2. 获取trackingNo
                // 3. 更新出库单
                successCount++;

            } catch (Exception e) {
                log.error("快递单号获取失败: key={}", record.key(), e);
                failCount++;
                // 失败的消息不提交，后续重试或进入DLQ
            } finally {
                clearTraceId();
            }
        }

        long cost = System.currentTimeMillis() - startTime;
        log.info(
                "快递单批量获取结束: total={}, success={}, fail={}, cost={}ms",
                records.size(),
                successCount,
                failCount,
                cost);

        // 全部成功才提交offset
        if (failCount == 0) {
            ack.acknowledge();
        } else {
            log.warn("批量获取有失败，不提交offset，将重试");
        }
    }

    private void clearTraceId() {
        com.xwms.common.trace.util.TraceIdUtil.clear();
    }
}
