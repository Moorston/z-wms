package com.xwms.base.rule.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 规则版本 */
@Data
@TableName("wms_rule_version")
public class RuleVersion {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String ruleCode;
    private Integer version;
    private String ruleScript;
    private String ruleConfig;
    private String changeDesc;
    private Integer isCurrent;
    private String operator;

    @TableField("owner_code_col")
    private String ownerCodeCol;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
