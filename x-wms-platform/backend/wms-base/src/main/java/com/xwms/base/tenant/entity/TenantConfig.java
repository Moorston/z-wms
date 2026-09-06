package com.xwms.base.tenant.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 租户配置 */
@Data
@TableName("wms_tenant_config")
public class TenantConfig {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String tenantCode;

    /** 配置键 */
    private String configKey;

    /** 配置值 */
    private String configValue;

    /** 配置类型: STRING/NUMBER/BOOLEAN/JSON */
    private String configType;

    private String description;

    /** 是否系统配置(不可删除) */
    private Integer isSystem;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
