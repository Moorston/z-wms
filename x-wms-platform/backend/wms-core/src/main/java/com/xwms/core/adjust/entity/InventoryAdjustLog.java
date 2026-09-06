package com.xwms.core.adjust.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 库存调整流水 */
@Data
@TableName("wms_inventory_adjust_log")
public class InventoryAdjustLog {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String logNo;
    private String adjustNo;
    private String freezeNo;
    private String warehouseCode;
    private String ownerCode;
    private String skuCode;
    private String batchNo;
    private String locationCode;

    /** 操作类型: ADJUST/FREEZE/RELEASE */
    private String actionType;

    private BigDecimal beforeQty;
    private BigDecimal changeQty;
    private BigDecimal afterQty;
    private BigDecimal beforeFrozen;
    private BigDecimal afterFrozen;

    private String operator;
    private LocalDateTime actionTime;
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
