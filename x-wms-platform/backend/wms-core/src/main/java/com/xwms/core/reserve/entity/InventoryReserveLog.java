package com.xwms.core.reserve.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 库存预占流水 */
@Data
@TableName("wms_inventory_reserve_log")
public class InventoryReserveLog {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String logNo;
    private String reserveNo;
    private String refType;
    private String refNo;
    private String warehouseCode;
    private String ownerCode;
    private String skuCode;
    private String batchNo;
    private String locationCode;

    /** 操作类型: RESERVE/RELEASE/CONFIRM/EXPIRE */
    private String actionType;

    private BigDecimal beforeQty;
    private BigDecimal changeQty;
    private BigDecimal afterQty;

    private String operator;
    private LocalDateTime actionTime;
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
