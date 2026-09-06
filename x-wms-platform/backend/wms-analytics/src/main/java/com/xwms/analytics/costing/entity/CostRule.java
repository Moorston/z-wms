package com.xwms.analytics.costing.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 搴撳瓨鎴愭湰瑙勫垯 */
@Data
@TableName("wms_cost_rule")
public class CostRule {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String ruleCode;
    private String ruleName;

    /** 閫傜敤SKU(绌鸿〃绀烘墍鏈? */
    private String skuCode;

    /** 閫傜敤鍝佺被 */
    private String categoryCode;

    /** 閫傜敤璐т富 */
    private String ownerCode;

    /** 鎴愭湰鏂规硶: FIFO/WEIGHTED_AVG/MOVING_AVG/STANDARD */
    private String costMethod;

    /** 鏍囧噯鎴愭湰鍗曚环 */
    private BigDecimal standardPrice;

    /** 鐘舵€? ACTIVE/INACTIVE */
    private String status;

    private Integer priority;
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
