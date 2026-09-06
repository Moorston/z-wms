package com.xwms.integration.external.es.document;

import java.time.LocalDateTime;

import com.xwms.integration.external.entity.ApiCallLog;

import lombok.Data;

/** ES 文档：接口调用日志 */
@Data
public class ApiCallLogDocument {

    private String logNo;
    private String systemCode;
    private String systemName;
    private String apiName;
    private String apiUrl;
    private String httpMethod;
    private String requestBody;
    private String responseBody;
    private String requestHeaders;
    private Integer responseStatus;
    private Long costTime;
    private String status;
    private String errorMsg;
    private Integer retryCount;
    private String traceId;
    private String businessType;
    private String businessNo;
    private String direction;
    private LocalDateTime createdTime;

    /** 从数据库实体转换为 ES 文档 */
    public static ApiCallLogDocument fromEntity(ApiCallLog log) {
        if (log == null) return null;
        ApiCallLogDocument doc = new ApiCallLogDocument();
        doc.setLogNo(log.getLogNo());
        doc.setSystemCode(log.getSystemCode());
        doc.setSystemName(log.getSystemName());
        doc.setApiName(log.getApiName());
        doc.setApiUrl(log.getApiUrl());
        doc.setHttpMethod(log.getHttpMethod());
        doc.setRequestBody(log.getRequestBody());
        doc.setResponseBody(log.getResponseBody());
        doc.setRequestHeaders(log.getRequestHeaders());
        doc.setResponseStatus(log.getResponseStatus());
        doc.setCostTime(log.getCostTime());
        doc.setStatus(log.getStatus());
        doc.setErrorMsg(log.getErrorMsg());
        doc.setRetryCount(log.getRetryCount());
        doc.setTraceId(log.getTraceId());
        doc.setBusinessType(log.getBusinessType());
        doc.setBusinessNo(log.getBusinessNo());
        doc.setDirection(log.getDirection());
        doc.setCreatedTime(log.getCreatedTime());
        return doc;
    }
}
