package com.xwms.core.freeze.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 库存冻结明细 */
@Data
@TableName("wms_inventory_freeze_detail")
public class InventoryFreezeDetail {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 冻结单号 */
    private String freezeNo;

    private String warehouseCode;
    private String ownerCode;
    private String skuCode;
    private String skuName;
    private String locationCode;
    private String batchNo;
    private String serialNo;
    private String containerNo;

    /** 冻结数量 */
    private BigDecimal freezeQty;

    /** 已解冻数量 */
    private BigDecimal unfreezeQty;

    /** 剩余冻结数量 */
    private BigDecimal remainQty;

    /** 冻结前库存状态 */
    private String beforeStatus;

    /** 冻结后库存状态 */
    private String afterStatus;

    /** 状态: FROZEN/PARTIAL/UNFROZEN */
    private String status;

    private LocalDateTime freezeTime;
    private LocalDateTime unfreezeTime;
    private String operator;
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
