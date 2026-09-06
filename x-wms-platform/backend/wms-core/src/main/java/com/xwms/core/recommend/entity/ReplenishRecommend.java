package com.xwms.core.recommend.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 补货推荐 */
@Data
@TableName("wms_replenish_recommend")
public class ReplenishRecommend {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String recommendId;
    private String warehouseCode;
    private String ownerCode;
    private String skuCode;
    private String skuName;
    private String categoryCode;
    private String fromLocation;
    private String toLocation;

    private BigDecimal currentQuantity;
    private BigDecimal safetyStock;
    private BigDecimal reorderPoint;
    private BigDecimal maxStock;
    private BigDecimal recommendQuantity;
    private BigDecimal forecastDemand;

    /** 预测周期: DAILY/WEEKLY/MONTHLY */
    private String forecastPeriod;

    /** 提前期(天) */
    private Integer leadTime;

    /** 优先级: URGENT/HIGH/NORMAL/LOW */
    private String priority;

    /** 置信度(%) */
    private BigDecimal confidence;

    private String reason;

    /** 状态: PENDING/ACCEPTED/REJECTED/EXECUTED */
    private String status;

    private String operator;
    private LocalDateTime operateTime;
    private String relatedReplenishId;
    private LocalDateTime recommendTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
