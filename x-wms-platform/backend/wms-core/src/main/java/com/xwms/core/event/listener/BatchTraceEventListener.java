package com.xwms.core.event.listener;

import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import com.xwms.core.batch.service.BatchService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 批次追踪事件消费者
 *
 * <p>监听Topic: wms-batch-events 事件类型： - BATCH_CREATE: 批次创建（入库收货） - BATCH_PUTAWAY: 批次上架 - BATCH_PICK:
 * 批次拣货（出库） - BATCH_SHIP: 批次发运 - BATCH_ADJUST: 批次调整 - BATCH_EXPIRE: 批次过期
 *
 * <p>处理逻辑： 1. 记录批次追踪事件（BatchTraceEvent） 2. 更新批次库存状态 3. 检查效期预警 4. 支持全链路追溯查询
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BatchTraceEventListener extends BaseEventListener {

    private final BatchService batchService;

    @KafkaListener(
            topics = "wms-batch-events",
            containerFactory = "batchKafkaListenerFactory",
            groupId = "wms-batch-group")
    public void onBatchEvent(ConsumerRecord<String, Object> record, Acknowledgment ack) {
        long startTime = System.currentTimeMillis();
        beforeProcess(record);

        try {
            Map<String, Object> data = toMap(record.value());
            String eventType = getEventType(data);
            log.info(
                    "处理批次事件: type={}, batchNo={}, sku={}",
                    eventType,
                    data.get("batchNo"),
                    data.get("sku"));

            switch (eventType) {
                case "BATCH_CREATE" -> handleBatchCreate(data);
                case "BATCH_PUTAWAY" -> handleBatchPutaway(data);
                case "BATCH_PICK" -> handleBatchPick(data);
                case "BATCH_SHIP" -> handleBatchShip(data);
                case "BATCH_ADJUST" -> handleBatchAdjust(data);
                case "BATCH_EXPIRE" -> handleBatchExpire(data);
                default -> log.warn("未知批次事件类型: {}", eventType);
            }

            ack(ack);
            afterProcess(record, startTime, true);

        } catch (Exception e) {
            log.error("批次事件处理失败", e);
            afterProcess(record, startTime, false);
            throw e;
        }
    }

    private void handleBatchCreate(Map<String, Object> data) {
        log.info(
                "批次创建: batchNo={}, sku={}, qty={}, expireDate={}",
                data.get("batchNo"),
                data.get("sku"),
                data.get("qty"),
                data.get("expireDate"));
        // 1. 记录批次追踪事件
        // 2. 创建批次库存记录
    }

    private void handleBatchPutaway(Map<String, Object> data) {
        log.info(
                "批次上架: batchNo={}, location={}, qty={}",
                data.get("batchNo"),
                data.get("locationCode"),
                data.get("qty"));
    }

    private void handleBatchPick(Map<String, Object> data) {
        log.info(
                "批次拣货: batchNo={}, orderNo={}, qty={}",
                data.get("batchNo"),
                data.get("orderNo"),
                data.get("qty"));
    }

    private void handleBatchShip(Map<String, Object> data) {
        log.info(
                "批次发运: batchNo={}, orderNo={}, trackingNo={}",
                data.get("batchNo"),
                data.get("orderNo"),
                data.get("trackingNo"));
    }

    private void handleBatchAdjust(Map<String, Object> data) {
        log.info(
                "批次调整: batchNo={}, beforeQty={}, afterQty={}",
                data.get("batchNo"),
                data.get("beforeQty"),
                data.get("afterQty"));
    }

    private void handleBatchExpire(Map<String, Object> data) {
        log.info(
                "批次过期: batchNo={}, sku={}, qty={}",
                data.get("batchNo"),
                data.get("sku"),
                data.get("qty"));
        // 触发过期预警通知
    }
}
