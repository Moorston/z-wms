package com.xwms.core.notification.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 通知规则 */
@Data
@TableName("wms_notify_rule")
public class NotifyRule {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String ruleCode;
    private String ruleName;

    /** 触发事件: ORDER_CREATED/STOCK_LOW/QC_FAILED/APPOINTMENT_OVERDUE等 */
    private String eventType;

    private String templateCode;
    private String channel;

    /** 接收者类型 */
    private String receiverType;

    /** 接收者ID */
    private String receiverId;

    /** 触发条件表达式(Spring EL) */
    private String conditionExpr;

    private Integer enabled;
    private Integer priority;

    private String remark;

    @TableField("owner_code_col")
    private String ownerCodeCol;

    @TableField("warehouse_code_col")
    private String warehouseCodeCol;

    private String createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
