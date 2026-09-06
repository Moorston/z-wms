package com.xwms.core.metrics.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

@Data
@TableName("wms_metrics_system")
public class MetricsSystem {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String systemId;
    private String systemName;
    private String systemCode;
    private String systemType;
    private String warehouseCode;
    private String ownerCode;
    private String description;
    private String systemConfig;
    private Integer metricCount;
    private Integer categoryCount;
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
