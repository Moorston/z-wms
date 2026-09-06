package com.xwms.analytics.costing.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 成本明细 */
@Data
@TableName("wms_cost_detail")
public class CostDetail {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String detailId;
    private String calculateId;
    private String adjustId;
    private String allocationId;

    private String warehouseCode;
    private String ownerCode;
    private String skuCode;
    private String batchNo;
    private String locationCode;

    /** 业务类型: INBOUND/OUTBOUND/TRANSFER/ADJUST/ALLOCATION */
    private String bizType;

    private String bizNo;

    private BigDecimal quantity;
    private BigDecimal unitCost;
    private BigDecimal totalCost;

    private BigDecimal beforeQuantity;
    private BigDecimal afterQuantity;
    private BigDecimal beforeUnitCost;
    private BigDecimal afterUnitCost;
    private BigDecimal beforeTotalCost;
    private BigDecimal afterTotalCost;
    private BigDecimal costDiff;

    private String costingMethod;
    private LocalDate periodDate;
    private String operator;
    private LocalDateTime operationTime;
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
