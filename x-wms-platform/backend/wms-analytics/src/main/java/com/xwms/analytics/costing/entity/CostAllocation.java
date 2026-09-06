package com.xwms.analytics.costing.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 成本分摊 */
@Data
@TableName("wms_cost_allocation")
public class CostAllocation {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String allocationId;
    private String allocationName;

    /** 分摊类型: FREIGHT/STORAGE/HANDLING/INSURANCE/OTHER */
    private String allocationType;

    private String warehouseCode;
    private String ownerCode;

    private LocalDate periodStart;
    private LocalDate periodEnd;

    private BigDecimal totalAmount;

    /** 分摊方法: BY_QUANTITY/BY_AMOUNT/BY_WEIGHT/BY_VOLUME/BY_SKU */
    private String allocationMethod;

    private Integer allocatedCount;
    private BigDecimal allocatedAmount;

    /** 状态: PENDING/ALLOCATING/COMPLETED/FAILED/CANCELLED */
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
