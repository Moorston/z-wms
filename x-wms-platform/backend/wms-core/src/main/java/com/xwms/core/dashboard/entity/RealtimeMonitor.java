package com.xwms.core.dashboard.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

@Data
@TableName("wms_realtime_monitor")
public class RealtimeMonitor {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String monitorId;
    private String monitorName;
    private String monitorType;
    private String warehouseCode;
    private String ownerCode;
    private String description;
    private String monitorConfig;
    private BigDecimal currentValue;
    private BigDecimal targetValue;
    private BigDecimal thresholdWarning;
    private BigDecimal thresholdCritical;
    private String unit;
    private String status;
    private String trend;
    private BigDecimal changeRate;
    private LocalDateTime lastUpdateTime;
    private Integer refreshInterval;
    private String isActive;
    private String operator;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
