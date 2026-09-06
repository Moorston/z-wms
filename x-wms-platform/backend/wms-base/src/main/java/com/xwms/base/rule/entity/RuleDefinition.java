package com.xwms.base.rule.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 规则定义 */
@Data
@TableName("wms_rule_definition")
public class RuleDefinition {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String ruleCode;
    private String ruleName;

    /** 规则类型: 12类业务规则 */
    private String ruleType;

    /** 规则分类: SYSTEM/CUSTOM/INDUSTRY */
    private String ruleCategory;

    private String description;

    /** 规则脚本(Groovy/JavaScript/表达式) */
    private String ruleScript;

    /** 规则配置(JSON) */
    private String ruleConfig;

    /** 优先级(越小越高) */
    private Integer priority;

    private Integer enabled;
    private Integer version;

    @TableField("owner_code_col")
    private String ownerCodeCol;

    private String warehouseCode;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
