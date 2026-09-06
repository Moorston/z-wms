package com.xwms.analytics.costing.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 成本核算 */
@Data
@TableName("wms_cost_calculate")
public class CostCalculate {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String calculateId;
    private String calculateName;

    /** 核算类型: MONTH_END/QUARTER_END/YEAR_END/REAL_TIME */
    private String calculateType;

    private String warehouseCode;
    private String ownerCode;

    private LocalDate periodStart;
    private LocalDate periodEnd;

    /** 成本方法: FIFO/LIFO/WEIGHTED_AVG/MOVING_AVG/STANDARD/SPECIFIC */
    private String costingMethod;

    private Integer totalSkuCount;
    private Integer calculatedCount;
    private Integer failedCount;

    private BigDecimal totalQuantity;
    private BigDecimal totalAmount;
    private BigDecimal averageCost;

    /** 状态: PENDING/CALCULATING/COMPLETED/FAILED/CANCELLED */
    private String status;

    private String errorMessage;
    private String operator;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long durationMs;
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
