package com.xwms.core.event.listener;

import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import com.xwms.core.inventory.service.InventoryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 库存事件消费者
 *
 * <p>监听Topic: wms-inventory-events 事件类型： - INVENTORY_DEDUCT: 库存扣减（出库） - INVENTORY_ADD: 库存增加（入库/上架）
 * - INVENTORY_ADJUST: 库存调整 - INVENTORY_FREEZE: 库存冻结 - INVENTORY_UNFREEZE: 库存解冻 -
 * INVENTORY_TRANSFER: 库存调拨
 *
 * <p>处理逻辑： 1. 更新库存流水表（InventoryTransaction） 2. 同步Redis库存缓存 3. 触发库存预警检查 4. 发布库存变动通知
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryEventListener extends BaseEventListener {

    private final InventoryService inventoryService;

    @KafkaListener(
            topics = "wms-inventory-events",
            containerFactory = "inventoryKafkaListenerFactory",
            groupId = "wms-inventory-group")
    public void onInventoryEvent(ConsumerRecord<String, Object> record, Acknowledgment ack) {
        long startTime = System.currentTimeMillis();
        beforeProcess(record);

        try {
            Map<String, Object> data = toMap(record.value());
            String eventType = getEventType(data);
            log.info("处理库存事件: type={}, data={}", eventType, data);

            switch (eventType) {
                case "INVENTORY_DEDUCT" -> handleDeduct(data);
                case "INVENTORY_ADD" -> handleAdd(data);
                case "INVENTORY_ADJUST" -> handleAdjust(data);
                case "INVENTORY_FREEZE" -> handleFreeze(data);
                case "INVENTORY_UNFREEZE" -> handleUnfreeze(data);
                case "INVENTORY_TRANSFER" -> handleTransfer(data);
                default -> log.warn("未知库存事件类型: {}", eventType);
            }

            ack(ack);
            afterProcess(record, startTime, true);

        } catch (Exception e) {
            log.error("库存事件处理失败, 将发送到死信队列", e);
            // 失败不提交offset，等待重试或进入DLQ
            afterProcess(record, startTime, false);
            throw e; // 抛出异常触发重试机制
        }
    }

    private void handleDeduct(Map<String, Object> data) {
        log.info(
                "库存扣减事件: sku={}, qty={}, location={}",
                data.get("sku"),
                data.get("qty"),
                data.get("locationCode"));
        // 1. 记录库存流水
        // 2. 更新Redis库存缓存
        // 3. 检查库存预警
    }

    private void handleAdd(Map<String, Object> data) {
        log.info(
                "库存增加事件: sku={}, qty={}, location={}",
                data.get("sku"),
                data.get("qty"),
                data.get("locationCode"));
    }

    private void handleAdjust(Map<String, Object> data) {
        log.info(
                "库存调整事件: sku={}, beforeQty={}, afterQty={}, reason={}",
                data.get("sku"),
                data.get("beforeQty"),
                data.get("afterQty"),
                data.get("reason"));
    }

    private void handleFreeze(Map<String, Object> data) {
        log.info(
                "库存冻结事件: sku={}, qty={}, reason={}",
                data.get("sku"),
                data.get("qty"),
                data.get("reason"));
    }

    private void handleUnfreeze(Map<String, Object> data) {
        log.info("库存解冻事件: sku={}, qty={}", data.get("sku"), data.get("qty"));
    }

    private void handleTransfer(Map<String, Object> data) {
        log.info(
                "库存调拨事件: sku={}, fromLocation={}, toLocation={}, qty={}",
                data.get("sku"),
                data.get("fromLocation"),
                data.get("toLocation"),
                data.get("qty"));
    }
}
