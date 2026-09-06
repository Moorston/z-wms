package com.xwms.core.notification.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 通知记录 */
@Data
@TableName("wms_notify_record")
public class NotifyRecord {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String recordNo;
    private String templateCode;
    private String notifyType;

    /** 渠道 */
    private String channel;

    private String title;
    private String content;

    /** 发送人 */
    private String sender;

    /** 接收者类型: USER/ROLE/DEPT/ALL */
    private String receiverType;

    /** 接收者ID */
    private String receiverId;

    private String receiverName;

    /** 关联业务类型 */
    private String businessType;

    /** 关联业务单号 */
    private String businessNo;

    /** 优先级: LOW/NORMAL/HIGH/URGENT */
    private String priority;

    /** 状态: PENDING/SENDING/SENT/FAILED/READ */
    private String status;

    private Integer retryCount;
    private Integer maxRetry;
    private String errorMsg;

    private LocalDateTime sendTime;
    private LocalDateTime readTime;

    @TableField("owner_code_col")
    private String ownerCodeCol;

    @TableField("warehouse_code_col")
    private String warehouseCodeCol;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
