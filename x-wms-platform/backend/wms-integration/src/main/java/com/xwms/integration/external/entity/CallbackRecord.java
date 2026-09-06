package com.xwms.integration.external.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 回调记录 */
@Data
@TableName("wms_callback_record")
public class CallbackRecord {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String callbackNo;
    private String systemCode;
    private String callbackUrl;

    /** 回调类型 */
    private String callbackType;

    private String requestBody;
    private Integer responseStatus;
    private String responseBody;

    /** 状态: PENDING/CALLBACKING/SUCCESS/FAILED */
    private String status;

    private Integer retryCount;
    private Integer maxRetry;
    private LocalDateTime nextRetryTime;

    /** 耗时(ms) */
    private Long costTime;

    private String errorMsg;
    private String businessType;
    private String businessNo;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
