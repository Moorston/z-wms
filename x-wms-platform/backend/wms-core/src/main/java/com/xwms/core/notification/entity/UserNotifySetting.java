package com.xwms.core.notification.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 用户通知设置 */
@Data
@TableName("wms_user_notify_setting")
public class UserNotifySetting {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;

    /** 通知类型 */
    private String notifyType;

    /** 渠道 */
    private String channel;

    private Integer enabled;

    /** 免打扰开始时间 HH:mm */
    private String quietStart;

    /** 免打扰结束时间 HH:mm */
    private String quietEnd;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
