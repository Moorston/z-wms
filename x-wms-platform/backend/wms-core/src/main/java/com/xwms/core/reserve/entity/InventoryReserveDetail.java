package com.xwms.core.reserve.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 库存预占明细 */
@Data
@TableName("wms_inventory_reserve_detail")
public class InventoryReserveDetail {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String reserveNo;
    private Integer lineNo;
    private String skuCode;
    private String skuName;
    private String batchNo;
    private String locationCode;
    private String containerNo;

    /** 计划预占数量 */
    private BigDecimal planQty;

    /** 已预占数量 */
    private BigDecimal reservedQty;

    /** 已释放数量 */
    private BigDecimal releasedQty;

    /** 已确认数量 */
    private BigDecimal confirmedQty;

    /** 状态: RESERVED/PARTIAL/RELEASED/CONFIRMED */
    private String status;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
