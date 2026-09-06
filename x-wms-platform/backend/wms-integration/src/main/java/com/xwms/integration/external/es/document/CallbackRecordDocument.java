package com.xwms.integration.external.es.document;

import java.time.LocalDateTime;

import com.xwms.integration.external.entity.CallbackRecord;

import lombok.Data;

/** ES 文档：回调记录 */
@Data
public class CallbackRecordDocument {

    private String callbackNo;
    private String systemCode;
    private String callbackUrl;
    private String callbackType;
    private String requestBody;
    private Integer responseStatus;
    private String responseBody;
    private String status;
    private Integer retryCount;
    private Integer maxRetry;
    private LocalDateTime nextRetryTime;
    private Long costTime;
    private String errorMsg;
    private String businessType;
    private String businessNo;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;

    /** 从数据库实体转换为 ES 文档 */
    public static CallbackRecordDocument fromEntity(CallbackRecord callback) {
        if (callback == null) return null;
        CallbackRecordDocument doc = new CallbackRecordDocument();
        doc.setCallbackNo(callback.getCallbackNo());
        doc.setSystemCode(callback.getSystemCode());
        doc.setCallbackUrl(callback.getCallbackUrl());
        doc.setCallbackType(callback.getCallbackType());
        doc.setRequestBody(callback.getRequestBody());
        doc.setResponseStatus(callback.getResponseStatus());
        doc.setResponseBody(callback.getResponseBody());
        doc.setStatus(callback.getStatus());
        doc.setRetryCount(callback.getRetryCount());
        doc.setMaxRetry(callback.getMaxRetry());
        doc.setNextRetryTime(callback.getNextRetryTime());
        doc.setCostTime(callback.getCostTime());
        doc.setErrorMsg(callback.getErrorMsg());
        doc.setBusinessType(callback.getBusinessType());
        doc.setBusinessNo(callback.getBusinessNo());
        doc.setCreatedTime(callback.getCreatedTime());
        doc.setUpdatedTime(callback.getUpdatedTime());
        return doc;
    }
}
