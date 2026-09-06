package com.xwms.core.replenish.entity;

import java.math.BigDecimal;

import com.baomidou.mybatisplus.annotation.*;

import com.xwms.common.core.BaseEntity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/** 补货规则 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_replenish_rule")
public class ReplenishRule extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String ruleCode;
    private String ruleName;
    private String sku;
    private String ownerCode;
    private String warehouseCode;

    /** 拣货区编码 */
    private String pickAreaCode;

    /** 存储区编码 */
    private String storageAreaCode;

    /** 安全库存 */
    private BigDecimal safetyStock;

    /** 补货点(触发阈值) */
    private BigDecimal reorderPoint;

    /** 补货量(固定批量) */
    private BigDecimal replenishQty;

    /** 补货上限 */
    private BigDecimal maxStock;

    /** 补货类型: FIXED固定批量/TO_LEVEL按需补到上限/EOQ经济订货量 */
    private String replenishType;

    /** 优先级 1-10 */
    private Integer priority;

    /** ABC分类 */
    private String abcClass;

    /** 补货提前期(小时) */
    private Integer leadTimeHours;

    /** 状态: ENABLED/DISABLED */
    private String status;

    private String remark;

    @TableField("owner_code_col")
    private String ownerCodeCol;

    @TableField("warehouse_code_col")
    private String warehouseCodeCol;
}
