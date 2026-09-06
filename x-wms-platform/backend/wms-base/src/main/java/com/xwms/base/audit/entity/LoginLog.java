package com.xwms.base.audit.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 登录日志 */
@Data
@TableName("sys_login_log")
public class LoginLog {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String logNo;
    private String userId;
    private String userName;

    /** 登录类型: LOGIN/LOGOUT */
    private String loginType;

    /** 登录状态: SUCCESS/FAILED */
    private String loginStatus;

    /** 失败原因 */
    private String failReason;

    private String ipAddress;
    private String userAgent;

    /** 设备类型: PC/PDA/MOBILE */
    private String deviceType;

    private String browser;
    private String os;

    /** 登录地点 */
    private String location;

    private String sessionId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
