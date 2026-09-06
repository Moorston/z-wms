package com.xwms.core.datamart.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

@Data
@TableName("wms_metric_define")
public class MetricDefine {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String metricId;
    private String metricName;
    private String metricCode;
    private String metricType;
    private String metricCategory;
    private String martId;
    private String description;
    private String calculationFormula;
    private String dataSource;
    private String unit;
    private Integer precision;
    private String aggregationType;
    private String isDerived;
    private String parentMetricId;
    private BigDecimal targetValue;
    private BigDecimal benchmarkValue;
    private BigDecimal thresholdWarning;
    private BigDecimal thresholdCritical;
    private String direction;
    private String isActive;
    private Integer sortOrder;
    private String createdBy;
    private String updatedBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
