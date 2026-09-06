package com.xwms.core.adjust.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 库存调整单 */
@Data
@TableName("wms_inventory_adjust")
public class InventoryAdjust {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String adjustNo;

    /** 调整类型: PROFIT/LOSS/DAMAGE/EXPIRE/TRANSFER/MANUAL */
    private String adjustType;

    private String warehouseCode;
    private String ownerCode;

    /** 关联类型: STOCKTAKE/RETURN/MANUAL */
    private String refType;

    private String refNo;

    /** 状态: DRAFT/SUBMITTED/APPROVED/EXECUTED/CANCELLED */
    private String status;

    private Integer totalSku;
    private BigDecimal totalQty;
    private BigDecimal totalAmount;

    private String reason;
    private String remark;
    private String createdBy;
    private String approvedBy;
    private String executedBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    private LocalDateTime approvedTime;
    private LocalDateTime executedTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;

    /** 调整明细（非数据库列，用于请求体承载明细列表） */
    @TableField(exist = false)
    private List<InventoryAdjustDetail> details;
}
