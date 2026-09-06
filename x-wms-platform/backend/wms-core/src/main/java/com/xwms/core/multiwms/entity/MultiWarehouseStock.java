package com.xwms.core.multiwms.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 多仓库存快照 */
@Data
@TableName("wms_multi_warehouse_stock")
public class MultiWarehouseStock {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String warehouseCode;
    private String sku;
    private String batchNo;

    /** 可用库存 */
    private BigDecimal availableQty;

    /** 预占库存 */
    private BigDecimal allocatedQty;

    /** 拣货中 */
    private BigDecimal pickingQty;

    /** 在途库存(调入) */
    private BigDecimal inTransitQty;

    /** 冻结库存 */
    private BigDecimal frozenQty;

    /** 总库存 */
    private BigDecimal totalQty;

    /** 安全库存 */
    private BigDecimal safetyStock;

    private LocalDateTime lastSyncTime;

    @TableField("owner_code_col")
    private String ownerCodeCol;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
