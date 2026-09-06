package com.xwms.analytics.costing.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 鎴愭湰娴佹按 */
@Data
@TableName("wms_cost_log")
public class CostLog {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String logNo;

    /** 鍏宠仈绫诲瀷: INBOUND/OUTBOUND/TRANSFER/ADJUST/COSTING */
    private String refType;

    private String refNo;
    private String warehouseCode;
    private String ownerCode;
    private String skuCode;
    private String batchNo;
    private String locationCode;
    private BigDecimal quantity;
    private BigDecimal beforeUnitCost;
    private BigDecimal afterUnitCost;
    private BigDecimal beforeTotalCost;
    private BigDecimal afterTotalCost;
    private BigDecimal changeAmount;
    private String operator;
    private LocalDateTime actionTime;
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
