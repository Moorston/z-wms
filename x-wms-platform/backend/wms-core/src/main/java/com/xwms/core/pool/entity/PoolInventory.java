package com.xwms.core.pool.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 池化库存 */
@Data
@TableName("wms_pool_inventory")
public class PoolInventory {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String poolCode;
    private String warehouseCode;
    private String ownerCode;
    private String skuCode;
    private String skuName;
    private String batchNo;
    private String locationCode;

    private BigDecimal totalQty;
    private BigDecimal availableQty;
    private BigDecimal allocatedQty;
    private BigDecimal reservedQty;
    private BigDecimal frozenQty;

    private BigDecimal unitCost;
    private BigDecimal totalCost;

    private LocalDateTime lastInboundTime;
    private LocalDateTime lastOutboundTime;

    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
