package com.xwms.integration.external.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.integration.external.entity.*;
import com.xwms.integration.external.enums.MessageStatus;
import com.xwms.integration.external.es.document.ApiCallLogDocument;
import com.xwms.integration.external.es.document.CallbackRecordDocument;
import com.xwms.integration.external.es.document.IntegrationMessageDocument;
import com.xwms.integration.external.es.event.EsSyncEvent;
import com.xwms.integration.external.mapper.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** WCS/TMS/ERP集成核心服务 包含: 外部系统配置/接口调用日志/消息队列/回调记录 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IntegrationService {

    private final ExternalSystemMapper externalSystemMapper;
    private final ApiCallLogMapper apiCallLogMapper;
    private final IntegrationMessageMapper integrationMessageMapper;
    private final CallbackRecordMapper callbackRecordMapper;

    private final ApplicationEventPublisher applicationEventPublisher;

    private static final AtomicInteger LOG_SEQ = new AtomicInteger(0);
    private static final AtomicInteger CALLBACK_SEQ = new AtomicInteger(0);
    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    // ============================================================
    // 1. 外部系统配置
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public ExternalSystem createSystem(ExternalSystem system) {
        externalSystemMapper.insert(system);
        log.info("创建外部系统: {}={}", system.getSystemCode(), system.getSystemName());
        return system;
    }

    @Transactional(rollbackFor = Exception.class)
    public ExternalSystem updateSystem(ExternalSystem system) {
        externalSystemMapper.updateById(system);
        return system;
    }

    public Page<ExternalSystem> pageSystems(
            Page<ExternalSystem> page, String systemType, String status) {
        LambdaQueryWrapper<ExternalSystem> wrapper = new LambdaQueryWrapper<>();
        if (systemType != null) wrapper.eq(ExternalSystem::getSystemType, systemType);
        if (status != null) wrapper.eq(ExternalSystem::getStatus, status);
        wrapper.orderByAsc(ExternalSystem::getSystemType).orderByAsc(ExternalSystem::getSystemCode);
        return externalSystemMapper.selectPage(page, wrapper);
    }

    public List<ExternalSystem> getActiveSystemsByType(String systemType) {
        return externalSystemMapper.selectActiveByType(systemType);
    }

    public ExternalSystem getSystemByCode(String systemCode) {
        return externalSystemMapper.selectByCode(systemCode);
    }

    // ============================================================
    // 2. 接口调用日志
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public ApiCallLog recordApiCall(
            String systemCode,
            String apiName,
            String apiUrl,
            String httpMethod,
            String requestHeaders,
            String requestBody,
            Integer responseStatus,
            String responseBody,
            Long costTime,
            String status,
            String errorMsg,
            String traceId,
            String businessType,
            String businessNo,
            String direction) {
        ExternalSystem system = externalSystemMapper.selectByCode(systemCode);
        ApiCallLog log = new ApiCallLog();
        log.setLogNo(generateLogNo());
        log.setSystemCode(systemCode);
        log.setSystemName(system != null ? system.getSystemName() : null);
        log.setApiName(apiName);
        log.setApiUrl(apiUrl);
        log.setHttpMethod(httpMethod);
        log.setRequestHeaders(requestHeaders);
        log.setRequestBody(requestBody);
        log.setResponseStatus(responseStatus);
        log.setResponseBody(responseBody);
        log.setCostTime(costTime);
        log.setStatus(status);
        log.setErrorMsg(errorMsg);
        log.setTraceId(traceId);
        log.setBusinessType(businessType);
        log.setBusinessNo(businessNo);
        log.setDirection(direction);
        apiCallLogMapper.insert(log);
        publishEsSync(List.of(ApiCallLogDocument.fromEntity(log)), "wms-api-call-log");
        return log;
    }

    public Page<ApiCallLog> pageApiLogs(
            Page<ApiCallLog> page,
            String systemCode,
            String status,
            String businessType,
            String businessNo,
            String direction) {
        LambdaQueryWrapper<ApiCallLog> wrapper = new LambdaQueryWrapper<>();
        if (systemCode != null) wrapper.eq(ApiCallLog::getSystemCode, systemCode);
        if (status != null) wrapper.eq(ApiCallLog::getStatus, status);
        if (businessType != null) wrapper.eq(ApiCallLog::getBusinessType, businessType);
        if (businessNo != null) wrapper.eq(ApiCallLog::getBusinessNo, businessNo);
        if (direction != null) wrapper.eq(ApiCallLog::getDirection, direction);
        wrapper.orderByDesc(ApiCallLog::getCreatedTime);
        return apiCallLogMapper.selectPage(page, wrapper);
    }

    public List<ApiCallLog> getApiLogsBySystem(String systemCode) {
        return apiCallLogMapper.selectBySystem(systemCode);
    }

    // ============================================================
    // 3. 集成消息
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public IntegrationMessage createMessage(
            String systemCode,
            String messageType,
            String topic,
            String payload,
            String businessType,
            String businessNo,
            String direction) {
        IntegrationMessage message = new IntegrationMessage();
        message.setMessageId(UUID.randomUUID().toString().replace("-", ""));
        message.setSystemCode(systemCode);
        message.setMessageType(messageType);
        message.setTopic(topic);
        message.setPayload(payload);
        message.setStatus(MessageStatus.PENDING.getCode());
        message.setRetryCount(0);
        message.setMaxRetry(5);
        message.setNextRetryTime(LocalDateTime.now());
        message.setBusinessType(businessType);
        message.setBusinessNo(businessNo);
        message.setDirection(direction);
        integrationMessageMapper.insert(message);
        log.info("创建集成消息: {}, 系统={}, 类型={}", message.getMessageId(), systemCode, messageType);
        publishEsSync(
                List.of(IntegrationMessageDocument.fromEntity(message)), "wms-integration-message");
        return message;
    }

    public Page<IntegrationMessage> pageMessages(
            Page<IntegrationMessage> page,
            String systemCode,
            String messageType,
            String status,
            String direction) {
        LambdaQueryWrapper<IntegrationMessage> wrapper = new LambdaQueryWrapper<>();
        if (systemCode != null) wrapper.eq(IntegrationMessage::getSystemCode, systemCode);
        if (messageType != null) wrapper.eq(IntegrationMessage::getMessageType, messageType);
        if (status != null) wrapper.eq(IntegrationMessage::getStatus, status);
        if (direction != null) wrapper.eq(IntegrationMessage::getDirection, direction);
        wrapper.orderByDesc(IntegrationMessage::getCreatedTime);
        return integrationMessageMapper.selectPage(page, wrapper);
    }

    public List<IntegrationMessage> getPendingMessages() {
        return integrationMessageMapper.selectPendingMessages();
    }

    @Transactional(rollbackFor = Exception.class)
    public IntegrationMessage updateMessageStatus(Long messageId, String status, String errorMsg) {
        IntegrationMessage message = integrationMessageMapper.selectById(messageId);
        if (message == null) throw new RuntimeException("消息不存在");
        message.setStatus(status);
        if ("SENT".equals(status)) {
            message.setSentTime(LocalDateTime.now());
        } else if ("CONSUMED".equals(status)) {
            message.setConsumedTime(LocalDateTime.now());
        } else if ("FAILED".equals(status)) {
            message.setRetryCount(message.getRetryCount() + 1);
            message.setErrorMsg(errorMsg);
            if (message.getRetryCount() < message.getMaxRetry()) {
                message.setNextRetryTime(
                        LocalDateTime.now().plusSeconds(30L * message.getRetryCount()));
                message.setStatus(MessageStatus.PENDING.getCode());
            }
        }
        integrationMessageMapper.updateById(message);
        publishEsSync(
                List.of(IntegrationMessageDocument.fromEntity(message)), "wms-integration-message");
        return message;
    }

    // ============================================================
    // 4. 回调记录
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public CallbackRecord createCallback(
            String systemCode,
            String callbackUrl,
            String callbackType,
            String requestBody,
            String businessType,
            String businessNo) {
        CallbackRecord callback = new CallbackRecord();
        callback.setCallbackNo(generateCallbackNo());
        callback.setSystemCode(systemCode);
        callback.setCallbackUrl(callbackUrl);
        callback.setCallbackType(callbackType);
        callback.setRequestBody(requestBody);
        callback.setStatus("PENDING");
        callback.setRetryCount(0);
        callback.setMaxRetry(5);
        callback.setNextRetryTime(LocalDateTime.now());
        callback.setBusinessType(businessType);
        callback.setBusinessNo(businessNo);
        callbackRecordMapper.insert(callback);
        log.info("创建回调: {}, 系统={}, 类型={}", callback.getCallbackNo(), systemCode, callbackType);
        publishEsSync(List.of(CallbackRecordDocument.fromEntity(callback)), "wms-callback-record");
        return callback;
    }

    public Page<CallbackRecord> pageCallbacks(
            Page<CallbackRecord> page, String systemCode, String status, String callbackType) {
        LambdaQueryWrapper<CallbackRecord> wrapper = new LambdaQueryWrapper<>();
        if (systemCode != null) wrapper.eq(CallbackRecord::getSystemCode, systemCode);
        if (status != null) wrapper.eq(CallbackRecord::getStatus, status);
        if (callbackType != null) wrapper.eq(CallbackRecord::getCallbackType, callbackType);
        wrapper.orderByDesc(CallbackRecord::getCreatedTime);
        return callbackRecordMapper.selectPage(page, wrapper);
    }

    public List<CallbackRecord> getPendingCallbacks() {
        return callbackRecordMapper.selectPendingCallbacks();
    }

    @Transactional(rollbackFor = Exception.class)
    public CallbackRecord updateCallbackStatus(
            Long callbackId,
            String status,
            Integer responseStatus,
            String responseBody,
            Long costTime,
            String errorMsg) {
        CallbackRecord callback = callbackRecordMapper.selectById(callbackId);
        if (callback == null) throw new RuntimeException("回调记录不存在");
        callback.setStatus(status);
        callback.setResponseStatus(responseStatus);
        callback.setResponseBody(responseBody);
        callback.setCostTime(costTime);
        if ("SUCCESS".equals(status)) {
            // 成功不做额外处理
        } else if ("FAILED".equals(status)) {
            callback.setRetryCount(callback.getRetryCount() + 1);
            callback.setErrorMsg(errorMsg);
            if (callback.getRetryCount() < callback.getMaxRetry()) {
                callback.setNextRetryTime(
                        LocalDateTime.now().plusSeconds(30L * callback.getRetryCount()));
                callback.setStatus("PENDING");
            }
        }
        callbackRecordMapper.updateById(callback);
        publishEsSync(List.of(CallbackRecordDocument.fromEntity(callback)), "wms-callback-record");
        return callback;
    }

    // ============================================================
    // ES 同步
    // ============================================================

    /**
     * 发布 ES 同步事件（异步，不阻塞主流程）
     *
     * <p>ES 同步是最终一致性：MySQL 写入成功后发布事件，ES 同步失败不阻塞业务。
     */
    private void publishEsSync(List<Object> documents, String index) {
        try {
            applicationEventPublisher.publishEvent(new EsSyncEvent(this, documents, index));
        } catch (Exception e) {
            log.error("发布 ES 同步事件失败: index={}", index, e);
        }
    }

    // ============================================================
    // 工具方法
    // ============================================================

    private String generateLogNo() {
        return "APILOG"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", LOG_SEQ.incrementAndGet() % 1000);
    }

    private String generateCallbackNo() {
        return "CB"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", CALLBACK_SEQ.incrementAndGet() % 1000);
    }
}
