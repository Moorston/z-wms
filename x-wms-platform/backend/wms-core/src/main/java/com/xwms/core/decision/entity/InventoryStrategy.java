package com.xwms.core.decision.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

@Data
@TableName("wms_inventory_strategy")
public class InventoryStrategy {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String strategyId;
    private String strategyCode;
    private String strategyName;
    private String strategyType;
    private String warehouseCode;
    private String ownerCode;
    private String categoryCode;
    private String skuCode;
    private String strategyConfig;
    private String strategyRules;
    private Integer priority;
    private String isActive;
    private LocalDateTime effectiveStart;
    private LocalDateTime effectiveEnd;
    private Integer version;
    private String description;
    private String createdBy;
    private String updatedBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
