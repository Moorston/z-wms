package com.xwms.core.event.listener;

import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 集成通知事件消费者
 *
 * <p>监听Topic: wms-integration-notify 事件类型： - ERP_NOTIFY: 通知ERP（入库/出库完成） - TMS_NOTIFY: 通知TMS（发运确认） -
 * WCS_NOTIFY: 通知WCS（设备指令） - ECOMMERCE_NOTIFY: 通知电商平台（发货状态） - EXPRESS_NOTIFY: 通知快递商（揽收请求）
 *
 * <p>处理逻辑： 1. 调用外部系统API（通过API平台） 2. 失败重试（3次） 3. 最终失败记录并告警
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IntegrationNotifyEventListener extends BaseEventListener {

    @KafkaListener(
            topics = "wms-integration-notify",
            containerFactory = "integrationKafkaListenerFactory",
            groupId = "wms-integration-group")
    public void onIntegrationNotify(ConsumerRecord<String, Object> record, Acknowledgment ack) {
        long startTime = System.currentTimeMillis();
        beforeProcess(record);

        try {
            Map<String, Object> data = toMap(record.value());
            String eventType = getEventType(data);
            String targetSystem = (String) data.get("targetSystem");
            log.info(
                    "处理集成通知: type={}, target={}, bizNo={}",
                    eventType,
                    targetSystem,
                    data.get("bizNo"));

            switch (eventType) {
                case "ERP_NOTIFY" -> notifyErp(data);
                case "TMS_NOTIFY" -> notifyTms(data);
                case "WCS_NOTIFY" -> notifyWcs(data);
                case "ECOMMERCE_NOTIFY" -> notifyEcommerce(data);
                case "EXPRESS_NOTIFY" -> notifyExpress(data);
                default -> log.warn("未知集成通知类型: {}", eventType);
            }

            ack(ack);
            afterProcess(record, startTime, true);

        } catch (Exception e) {
            log.error("集成通知处理失败", e);
            afterProcess(record, startTime, false);
            throw e;
        }
    }

    private void notifyErp(Map<String, Object> data) {
        log.info("通知ERP: bizType={}, bizNo={}", data.get("bizType"), data.get("bizNo"));
        // 通过API平台调用ERP适配器
    }

    private void notifyTms(Map<String, Object> data) {
        log.info("通知TMS: orderNo={}, trackingNo={}", data.get("orderNo"), data.get("trackingNo"));
    }

    private void notifyWcs(Map<String, Object> data) {
        log.info("通知WCS: deviceId={}, command={}", data.get("deviceId"), data.get("command"));
    }

    private void notifyEcommerce(Map<String, Object> data) {
        log.info(
                "通知电商平台: platform={}, orderNo={}, status={}",
                data.get("platform"),
                data.get("orderNo"),
                data.get("status"));
    }

    private void notifyExpress(Map<String, Object> data) {
        log.info(
                "通知快递商: expressCode={}, trackingNo={}",
                data.get("expressCode"),
                data.get("trackingNo"));
    }
}
