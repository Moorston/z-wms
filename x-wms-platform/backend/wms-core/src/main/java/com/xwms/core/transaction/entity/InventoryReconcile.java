package com.xwms.core.transaction.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 库存对账 */
@Data
@TableName("wms_inventory_reconcile")
public class InventoryReconcile {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 对账号 */
    private String reconcileNo;

    /** 对账类型: DAILY/MONTHLY/MANUAL */
    private String reconcileType;

    private String warehouseCode;
    private String ownerCode;

    /** 对账日期 */
    private LocalDate reconcileDate;

    /** 系统期初 */
    private BigDecimal beginQty;

    /** 系统期末 */
    private BigDecimal endQty;

    /** 实际期初 */
    private BigDecimal actualBeginQty;

    /** 实际期末 */
    private BigDecimal actualEndQty;

    /** 差异数量 */
    private BigDecimal diffQty;

    /** 差异SKU数 */
    private Integer diffCount;

    /** 状态: PENDING/PROCESSING/RESOLVED/IGNORED */
    private String status;

    private String operator;
    private LocalDateTime operateTime;
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
