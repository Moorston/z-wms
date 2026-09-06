package com.xwms.core.adjust.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 库存调整明细 */
@Data
@TableName("wms_inventory_adjust_detail")
public class InventoryAdjustDetail {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String adjustNo;
    private Integer lineNo;
    private String skuCode;
    private String skuName;
    private String batchNo;
    private String locationCode;
    private String containerNo;

    /** 调整前数量 */
    private BigDecimal beforeQty;

    /** 调整数量(正数增加/负数减少) */
    private BigDecimal adjustQty;

    /** 调整后数量 */
    private BigDecimal afterQty;

    private BigDecimal unitPrice;
    private BigDecimal adjustAmount;

    private String reason;
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
