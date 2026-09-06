package com.xwms.core.metrics.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

@Data
@TableName("wms_metrics_monitor")
public class MetricsMonitor {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String monitorId;
    private String metricId;
    private String metricCode;
    private String metricName;
    private String systemId;
    private String warehouseCode;
    private String ownerCode;
    private String monitorType;
    private String monitorConfig;
    private BigDecimal currentValue;
    private BigDecimal targetValue;
    private BigDecimal thresholdWarning;
    private BigDecimal thresholdCritical;
    private String unit;
    private String status;
    private String trend;
    private BigDecimal changeRate;
    private Integer alertCount;
    private LocalDateTime lastAlertTime;
    private LocalDateTime lastCheckTime;
    private Integer checkInterval;
    private String isActive;
    private String operator;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
