package com.xwms.core.decision.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

@Data
@TableName("wms_inventory_optimization")
public class InventoryOptimization {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String optimizationId;
    private String warehouseCode;
    private String ownerCode;
    private String optimizationType;
    private String optimizationName;
    private String currentState;
    private String targetState;
    private String optimizationPlan;
    private String expectedBenefit;
    private String actualBenefit;
    private String implementationPlan;
    private String implementationStatus;
    private LocalDateTime implementationStart;
    private LocalDateTime implementationEnd;
    private String priority;
    private String status;
    private String approver;
    private LocalDateTime approveTime;
    private String operator;
    private LocalDateTime operateTime;
    private LocalDateTime optimizationTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
