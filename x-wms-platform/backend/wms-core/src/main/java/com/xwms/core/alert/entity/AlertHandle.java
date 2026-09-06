package com.xwms.core.alert.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 预警处理记录 */
@Data
@TableName("wms_alert_handle")
public class AlertHandle {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String handleId;
    private String alertId;

    /** 处理类型: MANUAL/AUTO/ESCALATE */
    private String handleType;

    private String handleAction;
    private String handleNote;
    private String beforeStatus;
    private String afterStatus;
    private String handleBy;
    private String handleName;
    private String operator;
    private LocalDateTime handleTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
