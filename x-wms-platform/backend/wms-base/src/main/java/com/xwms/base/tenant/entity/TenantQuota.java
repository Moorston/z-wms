package com.xwms.base.tenant.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 租户资源配额 */
@Data
@TableName("wms_tenant_quota")
public class TenantQuota {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String tenantCode;

    /** 资源类型: USER用户/WAREHOUSE仓库/LOCATION库位/SKU商品/ORDER订单/STORAGE存储 */
    private String resourceType;

    /** 配额上限, 0表示不限 */
    private Long quotaLimit;

    /** 已使用 */
    private Long quotaUsed;

    /** 单位: 个/GB/次/天 */
    private String quotaUnit;

    /** 告警阈值(%) */
    private Integer warningThreshold;

    /** 状态: NORMAL/WARNING/OVER_LIMIT */
    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
