package com.xwms.core.pool.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 池化分配记录 */
@Data
@TableName("wms_pool_allocation")
public class PoolAllocation {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String allocationId;
    private String poolCode;
    private String warehouseCode;
    private String orderNo;
    private Integer orderLine;
    private String skuCode;
    private String skuName;
    private String batchNo;
    private String locationCode;

    private BigDecimal allocatedQty;
    private BigDecimal pickedQty;
    private BigDecimal remainQty;

    private String sourceOwner;
    private String targetOwner;
    private String shareRuleCode;

    /** 状态: ALLOCATED/PICKING/PICKED/RELEASED/CANCELLED */
    private String status;

    private LocalDateTime allocateTime;
    private LocalDateTime releaseTime;
    private String operator;
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
