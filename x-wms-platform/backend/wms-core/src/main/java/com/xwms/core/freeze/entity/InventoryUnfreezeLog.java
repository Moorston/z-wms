package com.xwms.core.freeze.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 解冻记录 */
@Data
@TableName("wms_inventory_unfreeze_log")
public class InventoryUnfreezeLog {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 解冻单号 */
    private String unfreezeNo;

    /** 原冻结单号 */
    private String freezeNo;

    private String warehouseCode;
    private String skuCode;
    private String locationCode;
    private String batchNo;

    /** 解冻数量 */
    private BigDecimal unfreezeQty;

    /** 解冻类型: FULL/PART/AUTO */
    private String unfreezeType;

    /** 解冻原因 */
    private String unfreezeReason;

    private String operator;
    private String approver;
    private LocalDateTime approveTime;
    private LocalDateTime unfreezeTime;
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
