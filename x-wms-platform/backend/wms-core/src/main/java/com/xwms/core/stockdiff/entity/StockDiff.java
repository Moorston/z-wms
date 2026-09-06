package com.xwms.core.stockdiff.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 盘点差异记录 */
@Data
@TableName("wms_stock_diff")
public class StockDiff {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String diffId;
    private String stocktakeNo;
    private Integer stocktakeLine;
    private String warehouseCode;
    private String ownerCode;
    private String locationCode;
    private String skuCode;
    private String skuName;
    private String batchNo;
    private String serialNo;

    /** 系统库存 */
    private BigDecimal systemQty;

    /** 实盘数量 */
    private BigDecimal countedQty;

    /** 差异数量 */
    private BigDecimal diffQty;

    /** 差异率(%) */
    private BigDecimal diffRate;

    /** 差异类型: SHORTAGE/OVERAGE/DAMAGE/EXPIRED/OTHER */
    private String diffType;

    /** 差异级别: MINOR/NORMAL/MAJOR/CRITICAL */
    private String diffLevel;

    private String reasonCode;
    private String reasonDesc;

    /** 状态: PENDING/PROCESSING/APPROVING/RESOLVED/CANCELLED */
    private String status;

    /** 是否调整库存: Y/N */
    private String adjustFlag;

    private String adjustNo;
    private String handler;
    private LocalDateTime handleTime;
    private String approver;
    private LocalDateTime approveTime;
    private String approveNote;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
