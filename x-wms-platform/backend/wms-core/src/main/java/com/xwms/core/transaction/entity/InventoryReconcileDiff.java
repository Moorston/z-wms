package com.xwms.core.transaction.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 库存对账差异 */
@Data
@TableName("wms_inventory_reconcile_diff")
public class InventoryReconcileDiff {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 差异ID */
    private String diffId;

    /** 对账号 */
    private String reconcileNo;

    private String warehouseCode;
    private String skuCode;
    private String skuName;
    private String locationCode;
    private String batchNo;

    /** 系统数量 */
    private BigDecimal systemQty;

    /** 实际数量 */
    private BigDecimal actualQty;

    /** 差异数量 */
    private BigDecimal diffQty;

    /** 差异类型: SHORTAGE盘亏/OVERAGE盘盈/COST_DIFF成本差异 */
    private String diffType;

    /** 状态: PENDING/PROCESSING/RESOLVED/IGNORED */
    private String status;

    /** 处理措施 */
    private String resolveAction;

    private String resolvedBy;
    private LocalDateTime resolvedTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
