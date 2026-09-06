package com.xwms.core.forecast.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 库存优化 */
@Data
@TableName("wms_inventory_optimization")
public class InventoryOptimization {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String optimizationId;
    private String optimizationName;

    /** 优化类型: SAFETY_STOCK/REORDER_POINT/ABC/SPACE/COST */
    private String optimizationType;

    private String warehouseCode;
    private String ownerCode;
    private String skuCode;
    private String categoryCode;

    private BigDecimal currentValue;
    private BigDecimal optimizedValue;

    /** 改善率(%) */
    private BigDecimal improvementRate;

    private BigDecimal costSaving;
    private BigDecimal spaceSaving;

    /** 优化结果(JSON) */
    private String optimizationResult;

    /** 建议(JSON) */
    private String suggestions;

    /** 状态: PENDING/COMPLETED/FAILED/IMPLEMENTED */
    private String status;

    private String errorMessage;
    private String operator;
    private String implementer;
    private LocalDateTime implementTime;
    private LocalDateTime optimizationTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
