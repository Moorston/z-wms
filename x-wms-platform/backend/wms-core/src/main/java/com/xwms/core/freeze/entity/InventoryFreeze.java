package com.xwms.core.freeze.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 库存冻结单 */
@Data
@TableName("wms_inventory_freeze")
public class InventoryFreeze {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 冻结单号 */
    private String freezeNo;

    /** 冻结类型: QC/STOCKTAKE/EXCEPTION/RECALL/EXPIRE/CUSTOMER/OTHER */
    private String freezeType;

    /** 冻结原因 */
    private String freezeReason;

    private String warehouseCode;
    private String ownerCode;

    /** 状态: FROZEN/PARTIAL/UNFROZEN/CANCELLED */
    private String status;

    /** 冻结SKU数 */
    private Integer totalSkuCount;

    /** 冻结总数量 */
    private BigDecimal totalQty;

    private LocalDateTime freezeTime;
    private LocalDateTime unfreezeTime;
    private String operator;
    private String approver;
    private LocalDateTime approveTime;
    private String remark;

    /** 关联单号(质检单/盘点单等) */
    private String refNo;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
