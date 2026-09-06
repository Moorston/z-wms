package com.xwms.integration.external.es.document;

import java.time.LocalDateTime;

import com.xwms.integration.external.entity.IntegrationMessage;

import lombok.Data;

/** ES 文档：集成消息 */
@Data
public class IntegrationMessageDocument {

    private String messageId;
    private String systemCode;
    private String messageType;
    private String topic;
    private String payload;
    private String status;
    private Integer retryCount;
    private Integer maxRetry;
    private LocalDateTime nextRetryTime;
    private LocalDateTime sentTime;
    private LocalDateTime consumedTime;
    private String errorMsg;
    private String businessType;
    private String businessNo;
    private String direction;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;

    /** 从数据库实体转换为 ES 文档 */
    public static IntegrationMessageDocument fromEntity(IntegrationMessage message) {
        if (message == null) return null;
        IntegrationMessageDocument doc = new IntegrationMessageDocument();
        doc.setMessageId(message.getMessageId());
        doc.setSystemCode(message.getSystemCode());
        doc.setMessageType(message.getMessageType());
        doc.setTopic(message.getTopic());
        doc.setPayload(message.getPayload());
        doc.setStatus(message.getStatus());
        doc.setRetryCount(message.getRetryCount());
        doc.setMaxRetry(message.getMaxRetry());
        doc.setNextRetryTime(message.getNextRetryTime());
        doc.setSentTime(message.getSentTime());
        doc.setConsumedTime(message.getConsumedTime());
        doc.setErrorMsg(message.getErrorMsg());
        doc.setBusinessType(message.getBusinessType());
        doc.setBusinessNo(message.getBusinessNo());
        doc.setDirection(message.getDirection());
        doc.setCreatedTime(message.getCreatedTime());
        doc.setUpdatedTime(message.getUpdatedTime());
        return doc;
    }
}
