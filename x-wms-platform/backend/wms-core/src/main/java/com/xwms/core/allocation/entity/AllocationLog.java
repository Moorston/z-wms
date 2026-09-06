package com.xwms.core.allocation.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 分配日志 */
@Data
@TableName("wms_allocation_log")
public class AllocationLog {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 日志ID */
    private String logId;

    /** 分配单号 */
    private String allocationNo;

    /** 订单号 */
    private String orderNo;

    private String skuCode;

    /** 操作类型: ALLOCATE/REALLOCATE/RELEASE/PICK/CANCEL */
    private String actionType;

    /** 操作前数量 */
    private BigDecimal beforeQty;

    /** 操作后数量 */
    private BigDecimal afterQty;

    /** 变动数量 */
    private BigDecimal changeQty;

    private String locationCode;
    private String batchNo;

    /** 原因 */
    private String reason;

    private String operator;
    private LocalDateTime operateTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
