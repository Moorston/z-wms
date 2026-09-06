package com.xwms.analytics.costing.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 成本调整 */
@Data
@TableName("wms_cost_adjust")
public class CostAdjust {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String adjustId;

    /** 调整类型: PRICE_ADJUST/QUANTITY_ADJUST/DIFFERENCE_ADJUST/WRITE_DOWN/WRITE_OFF */
    private String adjustType;

    private String warehouseCode;
    private String ownerCode;
    private String skuCode;
    private String batchNo;
    private String locationCode;

    private BigDecimal beforeQuantity;
    private BigDecimal afterQuantity;
    private BigDecimal quantityDiff;

    private BigDecimal beforeCost;
    private BigDecimal afterCost;
    private BigDecimal costDiff;

    private BigDecimal beforeAmount;
    private BigDecimal afterAmount;
    private BigDecimal amountDiff;

    private String adjustReason;
    private String adjustBasis;
    private String relatedBizType;
    private String relatedBizNo;

    /** 状态: PENDING/APPROVED/REJECTED/CANCELLED */
    private String status;

    private String approver;
    private LocalDateTime approveTime;
    private String approveOpinion;
    private String operator;
    private LocalDateTime adjustTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
