package com.xwms.core.forecast.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 库存分析 */
@Data
@TableName("wms_inventory_analysis")
public class InventoryAnalysis {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String analysisId;
    private String analysisName;

    /** 分析类型: TURNOVER/ABC/SAFETY_STOCK/AGING/VALUE/SPACE */
    private String analysisType;

    private String warehouseCode;
    private String ownerCode;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private String skuCode;
    private String categoryCode;

    private BigDecimal totalQuantity;
    private BigDecimal totalAmount;
    private BigDecimal avgQuantity;
    private BigDecimal avgAmount;
    private BigDecimal turnoverRate;
    private BigDecimal turnoverDays;

    /** ABC分类: A/B/C */
    private String abcClass;

    private BigDecimal safetyStock;
    private BigDecimal reorderPoint;
    private BigDecimal maxStock;
    private BigDecimal minStock;
    private Integer agingDays;

    /** 库龄区间: 0-30/31-60/61-90/91-180/180+ */
    private String agingBucket;

    private BigDecimal spaceUtilization;

    /** 分析结果(JSON) */
    private String analysisResult;

    /** 建议(JSON) */
    private String suggestions;

    /** 状态: PENDING/COMPLETED/FAILED */
    private String status;

    private String errorMessage;
    private String operator;
    private LocalDateTime analysisTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
