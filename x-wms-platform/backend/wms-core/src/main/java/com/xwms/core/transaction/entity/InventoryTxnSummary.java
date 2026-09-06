package com.xwms.core.transaction.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 库存流水汇总 */
@Data
@TableName("wms_inventory_txn_summary")
public class InventoryTxnSummary {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 汇总日期 */
    private LocalDate summaryDate;

    private String warehouseCode;
    private String ownerCode;
    private String skuCode;
    private String locationCode;
    private String batchNo;

    /** 期初数量 */
    private BigDecimal beginQty;

    /** 入库数量 */
    private BigDecimal inboundQty;

    /** 出库数量 */
    private BigDecimal outboundQty;

    /** 调整增加 */
    private BigDecimal adjustInQty;

    /** 调整减少 */
    private BigDecimal adjustOutQty;

    /** 调入数量 */
    private BigDecimal transferInQty;

    /** 调出数量 */
    private BigDecimal transferOutQty;

    /** 期末数量 */
    private BigDecimal endQty;

    /** 期初成本 */
    private BigDecimal beginCost;

    /** 入库成本 */
    private BigDecimal inboundCost;

    /** 出库成本 */
    private BigDecimal outboundCost;

    /** 期末成本 */
    private BigDecimal endCost;

    /** 流水笔数 */
    private Integer txnCount;

    private LocalDateTime summaryTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
