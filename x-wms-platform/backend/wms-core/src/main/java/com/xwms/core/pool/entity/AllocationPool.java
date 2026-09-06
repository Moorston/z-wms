package com.xwms.core.pool.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 分配池 */
@Data
@TableName("wms_allocation_pool")
public class AllocationPool {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String poolCode;
    private String poolName;
    private String warehouseCode;

    /** 池类型: SHARED/DEDICATED/VIRTUAL/BUFFER */
    private String poolType;

    private String ownerCode;
    private String categoryCode;
    private String skuCode;

    /** 最大容量 */
    private BigDecimal maxCapacity;

    /** 最小阈值 */
    private BigDecimal minThreshold;

    /** 分配模式: FIFO/FEFO/LIFO/PRIORITY */
    private String allocationMode;

    private Integer priority;
    private String status;
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
