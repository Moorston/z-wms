package com.xwms.base.rule.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 规则参数 */
@Data
@TableName("wms_rule_param")
public class RuleParam {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String ruleCode;
    private String paramCode;
    private String paramName;

    /** 参数类型: STRING/NUMBER/BOOLEAN/ENUM/JSON */
    private String paramType;

    private String paramValue;
    private String defaultValue;
    private Integer isRequired;
    private String description;
    private Integer sortOrder;

    @TableField("owner_code_col")
    private String ownerCodeCol;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
