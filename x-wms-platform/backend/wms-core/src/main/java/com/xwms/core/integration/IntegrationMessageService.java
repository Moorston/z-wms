package com.xwms.core.integration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 集成消息管理服务 负责WMS与外部系统之间的消息管理，包括： 1. 消息发送（同步/异步） 2. 消息接收（回调/轮询） 3. 消息重试（失败自动重试） 4. 死信队列（超过重试次数的消息）
 * 5. 消息日志（完整记录所有集成消息） 6. 消息监控（成功率/失败率/响应时间）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IntegrationMessageService {

    private static final AtomicInteger SEQ = new AtomicInteger(0);
    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    // 最大重试次数
    private static final int MAX_RETRY_COUNT = 3;
    // 重试间隔（秒）
    private static final long RETRY_INTERVAL_SECONDS = 60;

    // ============================================================

    // 1. 消息发送
    // ============================================================

    /**
     * 同步发送消息
     *
     * @param systemCode 目标系统编码
     * @param messageType 消息类型
     * @param bizNo 业务单号
     * @param payload 消息内容（JSON）
     * @return 发送结果
     */
    public IntegrationResult sendMessageSync(
            String systemCode, String messageType, String bizNo, String payload) {
        log.info("同步发送集成消息: system={}, type={}, bizNo={}", systemCode, messageType, bizNo);

        IntegrationMessage message = createMessage(systemCode, messageType, bizNo, payload, "SYNC");

        try {
            // 1. 调用外部系统接口
            IntegrationResult result = callExternalSystem(systemCode, messageType, payload);

            // 2. 更新消息状态
            if (result.isSuccess()) {
                message.setStatus("SUCCESS");
                message.setResponseData(result.getResponse());
                message.setFinishTime(LocalDateTime.now());
                message.setDurationMs(result.getDurationMs());
                log.info("同步消息发送成功: messageId={}, system={}", message.getMessageId(), systemCode);
            } else {
                message.setStatus("FAILED");
                message.setErrorMessage(result.getErrorMessage());
                message.setFinishTime(LocalDateTime.now());
                log.error(
                        "同步消息发送失败: messageId={}, error={}",
                        message.getMessageId(),
                        result.getErrorMessage());
            }

            // 3. 保存消息日志
            saveMessageLog(message);

            return result;
        } catch (Exception e) {
            log.error("同步消息发送异常: messageId={}, error={}", message.getMessageId(), e.getMessage());
            message.setStatus("FAILED");
            message.setErrorMessage(e.getMessage());
            message.setFinishTime(LocalDateTime.now());
            saveMessageLog(message);

            IntegrationResult result = new IntegrationResult();
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            return result;
        }
    }

    /** 异步发送消息 消息先保存到数据库，由定时任务或消息队列异步发送 */
    public String sendMessageAsync(
            String systemCode, String messageType, String bizNo, String payload) {
        log.info("异步发送集成消息: system={}, type={}, bizNo={}", systemCode, messageType, bizNo);

        IntegrationMessage message =
                createMessage(systemCode, messageType, bizNo, payload, "ASYNC");
        message.setStatus("PENDING");
        saveMessageLog(message);

        // TODO: 实际项目中发送到Kafka消息队列，由消费者异步处理
        log.info("异步消息已保存，等待发送: messageId={}", message.getMessageId());

        return message.getMessageId();
    }

    // ============================================================

    // 2. 消息接收
    // ============================================================

    /**
     * 接收外部系统消息
     *
     * @param systemCode 来源系统编码
     * @param messageType 消息类型
     * @param payload 消息内容
     * @return 处理结果
     */
    public IntegrationResult receiveMessage(String systemCode, String messageType, String payload) {
        log.info("接收外部系统消息: system={}, type={}", systemCode, messageType);

        IntegrationMessage message =
                createMessage(systemCode, messageType, null, payload, "RECEIVE");
        message.setDirection("INBOUND");

        try {
            // 1. 消息幂等校验
            if (isDuplicateMessage(systemCode, messageType, payload)) {
                log.warn("重复消息，跳过处理: system={}, type={}", systemCode, messageType);
                message.setStatus("DUPLICATE");
                saveMessageLog(message);

                IntegrationResult result = new IntegrationResult();
                result.setSuccess(true);
                result.setResponse("重复消息，已跳过");
                return result;
            }

            // 2. 处理消息
            IntegrationResult result = processInboundMessage(systemCode, messageType, payload);

            // 3. 更新消息状态
            if (result.isSuccess()) {
                message.setStatus("SUCCESS");
                message.setResponseData(result.getResponse());
            } else {
                message.setStatus("FAILED");
                message.setErrorMessage(result.getErrorMessage());
            }
            message.setFinishTime(LocalDateTime.now());
            saveMessageLog(message);

            return result;
        } catch (Exception e) {
            log.error(
                    "接收消息处理异常: system={}, type={}, error={}",
                    systemCode,
                    messageType,
                    e.getMessage());
            message.setStatus("FAILED");
            message.setErrorMessage(e.getMessage());
            message.setFinishTime(LocalDateTime.now());
            saveMessageLog(message);

            IntegrationResult result = new IntegrationResult();
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            return result;
        }
    }

    // ============================================================

    // 3. 消息重试
    // ============================================================

    /**
     * 重试失败消息 定时任务调用，重试所有状态为FAILED且未超过最大重试次数的消息
     *
     * @return 重试成功的消息数
     */
    public int retryFailedMessages() {
        log.info("开始重试失败消息");

        // 查询待重试的消息
        List<IntegrationMessage> failedMessages = queryPendingRetryMessages();
        log.info("找到{}条待重试消息", failedMessages.size());

        int successCount = 0;
        for (IntegrationMessage message : failedMessages) {
            try {
                // 检查是否达到重试间隔
                if (!canRetry(message)) {
                    continue;
                }

                // 重试发送
                IntegrationResult result =
                        callExternalSystem(
                                message.getSystemCode(),
                                message.getMessageType(),
                                message.getPayload());

                // 更新重试次数和状态
                message.setRetryCount(message.getRetryCount() + 1);
                message.setLastRetryTime(LocalDateTime.now());

                if (result.isSuccess()) {
                    message.setStatus("SUCCESS");
                    message.setResponseData(result.getResponse());
                    message.setFinishTime(LocalDateTime.now());
                    successCount++;
                    log.info(
                            "消息重试成功: messageId={}, retryCount={}",
                            message.getMessageId(),
                            message.getRetryCount());
                } else {
                    message.setErrorMessage(result.getErrorMessage());
                    if (message.getRetryCount() >= MAX_RETRY_COUNT) {
                        message.setStatus("DEAD_LETTER");
                        log.error("消息重试达到最大次数，进入死信队列: messageId={}", message.getMessageId());
                    } else {
                        message.setStatus("FAILED");
                    }
                }

                updateMessageLog(message);
            } catch (Exception e) {
                log.error("消息重试异常: messageId={}, error={}", message.getMessageId(), e.getMessage());
            }
        }

        log.info("失败消息重试完成: 总数={}, 成功={}", failedMessages.size(), successCount);
        return successCount;
    }

    /** 手动重试指定消息 */
    public boolean retryMessage(String messageId) {
        log.info("手动重试消息: messageId={}", messageId);

        IntegrationMessage message = queryMessageById(messageId);
        if (message == null) {
            log.error("消息不存在: messageId={}", messageId);
            return false;
        }

        try {
            IntegrationResult result =
                    callExternalSystem(
                            message.getSystemCode(),
                            message.getMessageType(),
                            message.getPayload());

            message.setRetryCount(message.getRetryCount() + 1);
            message.setLastRetryTime(LocalDateTime.now());

            if (result.isSuccess()) {
                message.setStatus("SUCCESS");
                message.setResponseData(result.getResponse());
                message.setFinishTime(LocalDateTime.now());
                log.info("手动重试成功: messageId={}", messageId);
            } else {
                message.setStatus("FAILED");
                message.setErrorMessage(result.getErrorMessage());
                log.error("手动重试失败: messageId={}, error={}", messageId, result.getErrorMessage());
            }

            updateMessageLog(message);
            return result.isSuccess();
        } catch (Exception e) {
            log.error("手动重试异常: messageId={}, error={}", messageId, e.getMessage());
            return false;
        }
    }

    // ============================================================

    // 4. 死信队列
    // ============================================================

    /** 查询死信消息 */
    public List<IntegrationMessage> queryDeadLetterMessages(String systemCode, String messageType) {
        log.info("查询死信消息: system={}, type={}", systemCode, messageType);

        // TODO: 实际项目中查询数据库
        List<IntegrationMessage> messages = new ArrayList<>();
        return messages;
    }

    /** 死信消息重新投递 */
    public boolean redeliverDeadLetter(String messageId) {
        log.info("死信消息重新投递: messageId={}", messageId);

        IntegrationMessage message = queryMessageById(messageId);
        if (message == null || !"DEAD_LETTER".equals(message.getStatus())) {
            log.error("消息不存在或不是死信消息: messageId={}", messageId);
            return false;
        }

        // 重置重试次数，状态改为PENDING
        message.setRetryCount(0);
        message.setStatus("PENDING");
        message.setErrorMessage(null);
        updateMessageLog(message);

        log.info("死信消息重新投递成功: messageId={}", messageId);
        return true;
    }

    // ============================================================

    // 5. 消息查询
    // ============================================================

    /** 根据消息ID查询 */
    public IntegrationMessage queryMessageById(String messageId) {
        // TODO: 实际项目中查询数据库
        return null;
    }

    /** 根据业务单号查询消息 */
    public List<IntegrationMessage> queryMessagesByBizNo(String bizNo) {
        // TODO: 实际项目中查询数据库
        return new ArrayList<>();
    }

    /** 分页查询消息日志 */
    public List<IntegrationMessage> queryMessages(
            String systemCode,
            String messageType,
            String status,
            LocalDateTime startTime,
            LocalDateTime endTime,
            int page,
            int size) {
        // TODO: 实际项目中分页查询数据库
        return new ArrayList<>();
    }

    // ============================================================

    // 6. 消息统计
    // ============================================================

    /** 获取消息统计数据 */
    public IntegrationStatistics getStatistics(LocalDateTime startTime, LocalDateTime endTime) {
        log.info("获取集成消息统计: {} ~ {}", startTime, endTime);

        // TODO: 实际项目中统计数据库
        IntegrationStatistics stats = new IntegrationStatistics();
        stats.setTotalCount(1000);
        stats.setSuccessCount(950);
        stats.setFailedCount(30);
        stats.setPendingCount(20);
        stats.setDeadLetterCount(5);
        stats.setSuccessRate(95.0);
        stats.setAvgDurationMs(200L);
        return stats;
    }

    // ============================================================

    // 7. 辅助方法
    // ============================================================

    private IntegrationMessage createMessage(
            String systemCode, String messageType, String bizNo, String payload, String sendType) {
        IntegrationMessage message = new IntegrationMessage();
        message.setMessageId(generateMessageId());
        message.setSystemCode(systemCode);
        message.setMessageType(messageType);
        message.setBizNo(bizNo);
        message.setPayload(payload);
        message.setSendType(sendType);
        message.setDirection("OUTBOUND");
        message.setStatus("PENDING");
        message.setRetryCount(0);
        message.setCreateTime(LocalDateTime.now());
        return message;
    }

    private IntegrationResult callExternalSystem(
            String systemCode, String messageType, String payload) {
        long startTime = System.currentTimeMillis();

        // TODO: 实际项目中通过Feign调用外部系统接口或wms-integration适配器
        // 模拟调用
        try {
            Thread.sleep(50); // 模拟网络延迟
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        IntegrationResult result = new IntegrationResult();
        result.setSuccess(true);
        result.setResponse("{\"code\":\"0\",\"message\":\"success\"}");
        result.setDurationMs(System.currentTimeMillis() - startTime);
        return result;
    }

    private boolean isDuplicateMessage(String systemCode, String messageType, String payload) {
        // TODO: 实际项目中根据系统编码+消息类型+消息内容哈希值判断是否重复
        return false;
    }

    private IntegrationResult processInboundMessage(
            String systemCode, String messageType, String payload) {
        // TODO: 实际项目中根据消息类型分发到对应的处理服务
        IntegrationResult result = new IntegrationResult();
        result.setSuccess(true);
        result.setResponse("{\"code\":\"0\",\"message\":\"processed\"}");
        return result;
    }

    private List<IntegrationMessage> queryPendingRetryMessages() {
        // TODO: 实际项目中查询状态为FAILED且重试次数<最大重试次数的消息
        return new ArrayList<>();
    }

    private boolean canRetry(IntegrationMessage message) {
        if (message.getLastRetryTime() == null) return true;
        long elapsedSeconds =
                java.time.Duration.between(message.getLastRetryTime(), LocalDateTime.now())
                        .getSeconds();
        return elapsedSeconds >= RETRY_INTERVAL_SECONDS;
    }

    private void saveMessageLog(IntegrationMessage message) {
        // TODO: 实际项目中保存到数据库
        log.debug("保存消息日志: messageId={}, status={}", message.getMessageId(), message.getStatus());
    }

    private void updateMessageLog(IntegrationMessage message) {
        // TODO: 实际项目中更新数据库
        log.debug(
                "更新消息日志: messageId={}, status={}, retryCount={}",
                message.getMessageId(),
                message.getStatus(),
                message.getRetryCount());
    }

    private String generateMessageId() {
        return "MSG"
                + NO_FMT.format(LocalDateTime.now())
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }

    // ============================================================

    // 8. 数据模型
    // ============================================================

    @Data
    public static class IntegrationMessage {
        private String messageId;
        private String systemCode;
        private String messageType;
        private String bizNo;
        private String payload;
        private String responseData;
        private String sendType; // SYNC同步/ASYNC异步/RECEIVE接收
        private String direction; // OUTBOUND出站/INBOUND入站
        private String status; // PENDING待发送/SUCCESS成功/FAILED失败/DEAD_LETTER死信/DUPLICATE重复
        private Integer retryCount;
        private LocalDateTime lastRetryTime;
        private String errorMessage;
        private LocalDateTime createTime;
        private LocalDateTime finishTime;
        private Long durationMs;
    }

    @Data
    public static class IntegrationResult {
        private boolean success;
        private String response;
        private String errorMessage;
        private Long durationMs;
    }

    @Data
    public static class IntegrationStatistics {
        private Integer totalCount;
        private Integer successCount;
        private Integer failedCount;
        private Integer pendingCount;
        private Integer deadLetterCount;
        private Double successRate;
        private Long avgDurationMs;
    }
}
