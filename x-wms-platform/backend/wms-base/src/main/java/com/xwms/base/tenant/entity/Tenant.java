package com.xwms.base.tenant.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 租户 */
@Data
@TableName("wms_tenant")
public class Tenant {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String tenantCode;
    private String tenantName;

    /** 租户类型: OWNER货主/WAREHOUSE仓库/PLATFORM平台 */
    private String tenantType;

    private String contactPerson;
    private String contactPhone;
    private String contactEmail;
    private String address;

    /** 行业: 医药/食品/电商/制造业等 */
    private String industry;

    /** 状态: ACTIVE/INACTIVE/FROZEN/EXPIRED */
    private String status;

    /** 过期日期 */
    private LocalDate expireDate;

    private String logoUrl;
    private String description;
    private String createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
