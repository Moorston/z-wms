package com.xwms.core.transaction.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 库存流水 */
@Data
@TableName("wms_inventory_transaction")
public class InventoryTransaction {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 流水号 */
    private String txnNo;

    /** 流水类型 */
    private String txnType;

    /** 方向: IN/OUT/NONE */
    private String txnDirection;

    private String warehouseCode;
    private String ownerCode;
    private String skuCode;
    private String skuName;
    private String locationCode;
    private String batchNo;
    private String serialNo;
    private String containerNo;

    /** 变动数量(正数) */
    private BigDecimal quantity;

    /** 变动前数量 */
    private BigDecimal beforeQty;

    /** 变动后数量 */
    private BigDecimal afterQty;

    /** 单位成本 */
    private BigDecimal unitCost;

    /** 总成本 */
    private BigDecimal totalCost;

    /** 业务类型 */
    private String businessType;

    /** 业务单号 */
    private String businessNo;

    /** 业务行号 */
    private Integer businessLine;

    /** 关联流水号 */
    private String refTxnNo;

    private String operator;
    private LocalDateTime operateTime;
    private String remark;

    /** 链路追踪ID */
    private String traceId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
