package com.xwms.core.metrics.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

@Data
@TableName("wms_metrics_category")
public class MetricsCategory {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String categoryId;
    private String categoryName;
    private String categoryCode;
    private String systemId;
    private String parentCategoryId;
    private Integer categoryLevel;
    private String description;
    private String categoryConfig;
    private Integer metricCount;
    private String isActive;
    private Integer sortOrder;
    private String createdBy;
    private String updatedBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
