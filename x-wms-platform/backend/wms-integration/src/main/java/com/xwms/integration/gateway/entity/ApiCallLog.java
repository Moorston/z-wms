package com.xwms.integration.gateway.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** API调用日志 */
@Data
@TableName("wms_api_call_log")
public class ApiCallLog {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String logNo;
    private String apiCode;
    private String apiPath;
    private String apiMethod;

    /** 调用方appKey */
    private String appKey;

    private String requestIp;
    private String requestHeaders;
    private String requestBody;
    private Integer responseStatus;
    private String responseBody;

    /** 耗时(ms) */
    private Long durationMs;

    /** 调用状态: SUCCESS/FAIL/TIMEOUT/RATE_LIMITED */
    private String callStatus;

    private String errorMsg;
    private String traceId;
    private LocalDateTime callTime;

    @TableField("owner_code_col")
    private String ownerCodeCol;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
