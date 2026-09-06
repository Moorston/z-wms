package com.xwms.core.snapshot.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 库存快照明细 */
@Data
@TableName("wms_inventory_snapshot_detail")
public class InventorySnapshotDetail {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 快照号 */
    private String snapshotNo;

    private String warehouseCode;
    private String ownerCode;
    private String skuCode;
    private String skuName;
    private String categoryCode;
    private String locationCode;
    private String batchNo;
    private String serialNo;
    private String containerNo;

    /** 快照数量 */
    private BigDecimal quantity;

    /** 可用数量 */
    private BigDecimal availableQty;

    /** 预占数量 */
    private BigDecimal allocatedQty;

    /** 拣货中数量 */
    private BigDecimal pickingQty;

    /** 冻结数量 */
    private BigDecimal frozenQty;

    /** 单位成本 */
    private BigDecimal unitCost;

    /** 总成本 */
    private BigDecimal totalCost;

    /** ABC分类 */
    private String abcClass;

    /** 库存状态 */
    private String inventoryStatus;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
