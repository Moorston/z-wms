package com.xwms.core.replenish.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 补货在途库存 */
@Data
@TableName("wms_replenish_in_transit")
public class ReplenishInTransit {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 关联补货任务ID */
    private Long taskId;

    private String sku;
    private String batchNo;

    /** 源库位 */
    private String fromLocation;

    /** 目标库位 */
    private String toLocation;

    /** 在途数量 */
    private BigDecimal qty;

    /** 状态: IN_TRANSIT/ARRIVED/CANCELLED */
    private String status;

    private String ownerCode;
    private String warehouseCode;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    private LocalDateTime arrivedTime;
}
