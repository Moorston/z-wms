package com.xwms.core.combo.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 组合明细 */
@Data
@TableName("wms_combo_item")
public class ComboItem {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 组合品编码 */
    private String comboCode;

    /** 子品SKU */
    private String itemSkuCode;

    /** 子品名称 */
    private String itemSkuName;

    /** 子品数量 */
    private BigDecimal quantity;

    /** 子品单位成本 */
    private BigDecimal unitCost;

    /** 是否可选: Y/N */
    private String isOptional;

    /** 排序号 */
    private Integer sortNo;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
