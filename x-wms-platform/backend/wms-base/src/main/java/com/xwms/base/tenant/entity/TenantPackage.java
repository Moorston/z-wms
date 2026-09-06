package com.xwms.base.tenant.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 租户套餐 */
@Data
@TableName("wms_tenant_package")
public class TenantPackage {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String packageCode;
    private String packageName;

    /** 套餐级别: BASIC基础/STANDARD标准/PRO专业/ENTERPRISE企业 */
    private String packageLevel;

    /** 价格 */
    private BigDecimal price;

    /** 价格单位: MONTH月/YEAR年 */
    private String priceUnit;

    private String description;

    /** 功能特性(JSON) */
    private String features;

    /** 资源配额(JSON) */
    private String quotas;

    private Integer enabled;
    private Integer sortOrder;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
