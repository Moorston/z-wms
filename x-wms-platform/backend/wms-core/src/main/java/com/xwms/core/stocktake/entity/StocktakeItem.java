package com.xwms.core.stocktake.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 盘点明细 */
@Data
@TableName("wms_stocktake_item")
public class StocktakeItem {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long taskId;
    private String locationCode;
    private String sku;
    private String barcode;
    private String productName;
    private String batchNo;
    private String ownerCode;

    /** 系统库存 */
    private BigDecimal systemQty;

    /** 初盘数量 */
    private BigDecimal firstCountQty;

    /** 复盘数量 */
    private BigDecimal secondCountQty;

    /** 最终确认数量 */
    private BigDecimal finalCountQty;

    /** 差异数量(最终-系统) */
    private BigDecimal diffQty;

    /** 差异金额 */
    private BigDecimal diffAmount;

    /** 盘点状态: PENDING/COUNTED/RECOUNT/CONFIRMED/ADJUSTED */
    private String countStatus;

    private String firstCounter;
    private String secondCounter;
    private LocalDateTime firstCountTime;
    private LocalDateTime secondCountTime;
    private LocalDateTime confirmTime;

    private String remark;

    @TableField("owner_code_col")
    private String ownerCodeCol;

    @TableField("warehouse_code_col")
    private String warehouseCodeCol;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
