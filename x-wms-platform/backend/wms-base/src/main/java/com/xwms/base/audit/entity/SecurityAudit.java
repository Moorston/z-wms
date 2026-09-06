package com.xwms.base.audit.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 安全审计 */
@Data
@TableName("sys_security_audit")
public class SecurityAudit {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String auditNo;

    /** 审计类型: LOGIN_ABNORMAL/PERMISSION_VIOLATION/DATA_EXPORT/CONFIG_CHANGE/SENSITIVE_ACCESS等 */
    private String auditType;

    private String userId;
    private String userName;

    /** 风险等级: LOW/MEDIUM/HIGH/CRITICAL */
    private String riskLevel;

    private String description;

    /** 资源类型: USER/ROLE/DATA/API/CONFIG */
    private String resourceType;

    private String resourceId;

    /** 操作: ACCESS/MODIFY/DELETE/EXPORT */
    private String action;

    private String beforeValue;
    private String afterValue;

    private String ipAddress;

    /** 状态: PENDING/PROCESSING/RESOLVED/IGNORED */
    private String status;

    private String handledBy;
    private LocalDateTime handledTime;
    private String handleRemark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
