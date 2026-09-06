package com.xwms.core.safety.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 补货建议 */
@Data
@TableName("wms_reorder_suggestion")
public class ReorderSuggestion {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String suggestionNo;
    private String warehouseCode;
    private String ownerCode;
    private String skuCode;
    private String skuName;

    /** 当前库存 */
    private BigDecimal currentStock;

    /** 补货点 */
    private BigDecimal reorderPoint;

    /** 安全库存 */
    private BigDecimal safetyStock;

    /** 最高库存 */
    private BigDecimal maxStock;

    /** 建议补货数量 */
    private BigDecimal suggestQty;

    /** 建议类型: URGENT紧急/NORMAL正常/OPTIMAL优化 */
    private String suggestType;

    /** 状态: PENDING/CONVERTED/IGNORED */
    private String status;

    /** 来源: AUTO/MANUAL */
    private String source;

    /** 关联补货单号 */
    private String refNo;

    private String createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
