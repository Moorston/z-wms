package com.xwms.integration.external.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 集成消息 */
@Data
@TableName("wms_integration_message")
public class IntegrationMessage {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String messageId;
    private String systemCode;

    /** 消息类型: ORDER_CREATE/ORDER_UPDATE/INVENTORY_SYNC/SHIPMENT_NOTIFY等 */
    private String messageType;

    private String topic;
    private String payload;

    /** 状态: PENDING/SENDING/SENT/FAILED/CONSUMED */
    private String status;

    private Integer retryCount;
    private Integer maxRetry;
    private LocalDateTime nextRetryTime;
    private LocalDateTime sentTime;
    private LocalDateTime consumedTime;
    private String errorMsg;
    private String businessType;
    private String businessNo;

    /** 方向: SEND发送/RECEIVE接收 */
    private String direction;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
