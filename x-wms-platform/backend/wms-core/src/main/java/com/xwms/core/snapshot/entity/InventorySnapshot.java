package com.xwms.core.snapshot.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 库存快照 */
@Data
@TableName("wms_inventory_snapshot")
public class InventorySnapshot {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 快照号 */
    private String snapshotNo;

    /** 快照类型: DAILY/MONTHLY/MANUAL/REALTIME */
    private String snapshotType;

    private String warehouseCode;
    private String ownerCode;

    /** 快照日期 */
    private LocalDate snapshotDate;

    /** 快照时间 */
    private LocalDateTime snapshotTime;

    /** SKU总数 */
    private Integer totalSkuCount;

    /** 总数量 */
    private BigDecimal totalQty;

    /** 总成本 */
    private BigDecimal totalCost;

    /** 总价值 */
    private BigDecimal totalValue;

    /** 状态: PROCESSING/COMPLETED/FAILED */
    private String status;

    private String failReason;
    private String operator;
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
