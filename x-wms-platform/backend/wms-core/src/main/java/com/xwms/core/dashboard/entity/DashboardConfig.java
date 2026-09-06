package com.xwms.core.dashboard.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

@Data
@TableName("wms_dashboard_config")
public class DashboardConfig {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String configId;
    private String configName;
    private String configCode;
    private String dashboardType;
    private String warehouseCode;
    private String ownerCode;
    private String description;
    private String layoutConfig;
    private String themeConfig;
    private Integer refreshInterval;
    private String isDefault;
    private String isActive;
    private Integer sortOrder;
    private String createdBy;
    private String updatedBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
