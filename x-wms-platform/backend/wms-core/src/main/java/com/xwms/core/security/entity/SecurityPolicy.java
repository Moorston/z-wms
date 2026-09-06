package com.xwms.core.security.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 安全策略 */
@Data
@TableName("wms_security_policy")
public class SecurityPolicy {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String policyCode;
    private String policyName;

    /** 策略类型: PASSWORD/LOGIN/SESSION/ACCESS/DATA/API */
    private String policyType;

    /** 策略范围: GLOBAL/WAREHOUSE/ROLE/USER */
    private String policyScope;

    private String scopeValue;

    /** 策略配置(JSON) */
    private String policyConfig;

    /** 优先级 */
    private Integer priority;

    /** 是否启用: Y/N */
    private String isEnabled;

    private LocalDate effectiveDate;
    private LocalDate expireDate;
    private String description;
    private String createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
