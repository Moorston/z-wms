package com.xwms.core.combo.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 组合规则 */
@Data
@TableName("wms_combo_rule")
public class ComboRule {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 组合品编码 */
    private String comboCode;

    /** 组合品名称 */
    private String comboName;

    private String warehouseCode;
    private String ownerCode;

    /** 组合类型: KIT/BUNDLE/GIFT/PROMO */
    private String comboType;

    /** 状态: ACTIVE/INACTIVE */
    private String status;

    /** 是否自动组装: Y/N */
    private String autoAssemble;

    /** 是否自动拆解: Y/N */
    private String autoDisassemble;

    /** 成本计算: SUM/FIXED/WEIGHTED */
    private String costCalcMethod;

    /** 固定成本 */
    private BigDecimal fixedCost;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
