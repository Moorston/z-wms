package com.xwms.core.event.listener;

import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * AI事件消费者
 *
 * <p>监听Topic: wms-ai-events 事件类型： - OCR_REQUEST: 单据OCR识别请求 - FORECAST_REQUEST: 需求预测请求 -
 * REPORT_GENERATE: 自动化报表生成 - RAG_QUESTION: 智能问答请求 - AIOPS_ALERT: AIOps告警 - VOICE_PICK: 语音拣货指令 -
 * DRONE_INVENTORY: 无人机盘点结果 - PACKAGE_VIDEO: 打包视频分析结果
 *
 * <p>处理逻辑： 1. 转发到AI平台（独立Python服务） 2. 异步处理，不阻塞主流程 3. 结果回调更新业务数据
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiEventListener extends BaseEventListener {

    @KafkaListener(
            topics = "wms-ai-events",
            containerFactory = "aiKafkaListenerFactory",
            groupId = "wms-ai-group")
    public void onAiEvent(ConsumerRecord<String, Object> record, Acknowledgment ack) {
        long startTime = System.currentTimeMillis();
        beforeProcess(record);

        try {
            Map<String, Object> data = toMap(record.value());
            String eventType = getEventType(data);
            log.info("处理AI事件: type={}, requestId={}", eventType, data.get("requestId"));

            switch (eventType) {
                case "OCR_REQUEST" -> handleOcrRequest(data);
                case "FORECAST_REQUEST" -> handleForecastRequest(data);
                case "REPORT_GENERATE" -> handleReportGenerate(data);
                case "RAG_QUESTION" -> handleRagQuestion(data);
                case "AIOPS_ALERT" -> handleAiopsAlert(data);
                case "VOICE_PICK" -> handleVoicePick(data);
                case "DRONE_INVENTORY" -> handleDroneInventory(data);
                case "PACKAGE_VIDEO" -> handlePackageVideo(data);
                default -> log.warn("未知AI事件类型: {}", eventType);
            }

            ack(ack);
            afterProcess(record, startTime, true);

        } catch (Exception e) {
            log.error("AI事件处理失败", e);
            afterProcess(record, startTime, false);
            // AI事件失败不阻塞主流程，记录日志即可
            ack.acknowledge();
        }
    }

    private void handleOcrRequest(Map<String, Object> data) {
        log.info("OCR识别请求: docType={}, fileUrl={}", data.get("docType"), data.get("fileUrl"));
        // 调用AI平台OCR服务，识别结果回写入库单/出库单
    }

    private void handleForecastRequest(Map<String, Object> data) {
        log.info("需求预测请求: sku={}, period={}", data.get("sku"), data.get("period"));
    }

    private void handleReportGenerate(Map<String, Object> data) {
        log.info("报表生成请求: reportType={}, params={}", data.get("reportType"), data.get("params"));
    }

    private void handleRagQuestion(Map<String, Object> data) {
        log.info("智能问答: question={}, sessionId={}", data.get("question"), data.get("sessionId"));
    }

    private void handleAiopsAlert(Map<String, Object> data) {
        log.info(
                "AIOps告警: alertType={}, severity={}, message={}",
                data.get("alertType"),
                data.get("severity"),
                data.get("message"));
    }

    private void handleVoicePick(Map<String, Object> data) {
        log.info("语音拣货: operator={}, command={}", data.get("operator"), data.get("command"));
    }

    private void handleDroneInventory(Map<String, Object> data) {
        log.info(
                "无人机盘点: warehouse={}, area={}, resultCount={}",
                data.get("warehouse"),
                data.get("area"),
                data.get("resultCount"));
    }

    private void handlePackageVideo(Map<String, Object> data) {
        log.info(
                "打包视频分析: orderNo={}, videoUrl={}, result={}",
                data.get("orderNo"),
                data.get("videoUrl"),
                data.get("result"));
    }
}
