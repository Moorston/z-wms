package com.xwms.integration.external.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 接口调用日志 */
@Data
@TableName("wms_api_call_log")
public class ApiCallLog {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String logNo;
    private String systemCode;
    private String systemName;
    private String apiName;
    private String apiUrl;
    private String httpMethod;
    private String requestHeaders;
    private String requestBody;
    private Integer responseStatus;
    private String responseBody;

    /** 耗时(ms) */
    private Long costTime;

    /** 状态: SUCCESS/FAILED/TIMEOUT */
    private String status;

    private String errorMsg;
    private Integer retryCount;
    private String traceId;
    private String businessType;
    private String businessNo;

    /** 方向: INBOUND入站/OUTBOUND出站 */
    private String direction;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
