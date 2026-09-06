package com.xwms.core.metrics.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

@Data
@TableName("wms_metrics_calculation")
public class MetricsCalculation {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String calcId;
    private String metricId;
    private String metricCode;
    private String metricName;
    private String systemId;
    private String warehouseCode;
    private String ownerCode;
    private String periodType;
    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;
    private String calcFormula;
    private String calcParams;
    private BigDecimal calcResult;
    private String calcUnit;
    private BigDecimal targetValue;
    private BigDecimal benchmarkValue;
    private BigDecimal deviation;
    private BigDecimal deviationRate;
    private String status;
    private String errorMessage;
    private LocalDateTime calcStartTime;
    private LocalDateTime calcEndTime;
    private Long durationMs;
    private String operator;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
