package com.xwms.core.abc.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** ABC分类规则 */
@Data
@TableName("wms_abc_rule")
public class AbcRule {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String ruleCode;
    private String ruleName;
    private String warehouseCode;
    private String ownerCode;
    private String categoryCode;

    /** 分类依据: SALES_AMOUNT/SALES_QTY/PROFIT/TURNOVER */
    private String classifyBy;

    /** 统计周期: DAY/WEEK/MONTH/QUARTER/YEAR */
    private String periodType;

    /** A类占比(%) */
    private BigDecimal aRatio;

    /** B类占比(%) */
    private BigDecimal bRatio;

    /** C类占比(%) */
    private BigDecimal cRatio;

    /** A类品种占比(%) */
    private BigDecimal aCountRatio;

    /** B类品种占比(%) */
    private BigDecimal bCountRatio;

    /** C类品种占比(%) */
    private BigDecimal cCountRatio;

    /** 状态: ACTIVE/INACTIVE */
    private String status;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
