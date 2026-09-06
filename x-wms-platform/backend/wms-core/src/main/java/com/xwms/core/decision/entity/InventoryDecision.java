package com.xwms.core.decision.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

@Data
@TableName("wms_inventory_decision")
public class InventoryDecision {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String decisionId;
    private String warehouseCode;
    private String ownerCode;
    private String skuCode;
    private String skuName;
    private String categoryCode;
    private String decisionType;
    private String decisionName;
    private String decisionContent;
    private String decisionReason;
    private String decisionBasis;
    private String expectedImpact;
    private String actualImpact;
    private String priority;
    private BigDecimal confidence;
    private String status;
    private String approver;
    private LocalDateTime approveTime;
    private String approveComment;
    private String operator;
    private LocalDateTime operateTime;
    private String relatedBizNo;
    private LocalDateTime decisionTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
