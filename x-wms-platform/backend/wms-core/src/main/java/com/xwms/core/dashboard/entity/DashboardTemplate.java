package com.xwms.core.dashboard.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 大屏模板 */
@Data
@TableName("wms_dashboard_template")
public class DashboardTemplate {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String templateCode;
    private String templateName;

    /** 模板类型: WAREHOUSE/INVENTORY/OPERATION/KPI/REAL_TIME */
    private String templateType;

    /** 行业: GENERAL/PHARMA/FOOD/ECOMMERCE/COLD_CHAIN */
    private String industry;

    private String description;
    private String thumbnailUrl;

    /** 完整配置JSON */
    private String configJson;

    /** 组件数量 */
    private Integer componentCount;

    /** 是否内置: Y/N */
    private String isBuiltin;

    private String status;
    private String createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
