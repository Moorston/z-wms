package com.xwms.core.allocation.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 分配明细 */
@Data
@TableName("wms_allocation_detail")
public class AllocationDetail {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 分配单号 */
    private String allocationNo;

    /** 订单号 */
    private String orderNo;

    /** 订单行号 */
    private Integer orderLine;

    private String warehouseCode;
    private String ownerCode;
    private String skuCode;
    private String skuName;
    private String locationCode;
    private String batchNo;
    private String serialNo;
    private String containerNo;

    /** 已分配数量 */
    private BigDecimal allocatedQty;

    /** 已拣货数量 */
    private BigDecimal pickedQty;

    /** 剩余分配数量 */
    private BigDecimal remainQty;

    /** 单位成本 */
    private BigDecimal unitCost;

    /** 总成本 */
    private BigDecimal totalCost;

    /** 状态: ALLOCATED/PICKING/PICKED/RELEASED/CANCELLED */
    private String status;

    private LocalDateTime allocateTime;
    private LocalDateTime releaseTime;
    private String operator;
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
