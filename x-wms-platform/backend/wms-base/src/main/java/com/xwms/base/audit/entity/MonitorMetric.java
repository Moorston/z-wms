package com.xwms.base.audit.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 系统监控指标 */
@Data
@TableName("sys_monitor_metric")
public class MonitorMetric {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 指标名: CPU/MEMORY/DISK/DB_CONN/API_QPS/API_LATENCY等 */
    private String metricName;

    /** 指标分类: SYSTEM/APPLICATION/DATABASE/BUSINESS */
    private String metricCategory;

    /** 指标值 */
    private BigDecimal metricValue;

    /** 单位: %/MB/GB/ms/count等 */
    private String metricUnit;

    /** 告警阈值 */
    private BigDecimal thresholdWarn;

    /** 严重阈值 */
    private BigDecimal thresholdCritical;

    /** 状态: NORMAL/WARN/CRITICAL */
    private String status;

    /** 实例ID */
    private String instanceId;

    /** 采集时间 */
    private LocalDateTime collectedTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
