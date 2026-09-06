package com.xwms.core.plugin.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 行业规则 */
@Data
@TableName("wms_industry_rule")
public class IndustryRule {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String ruleCode;
    private String ruleName;

    /** 行业: GSP/COLD_CHAIN/ECOMMERCE等 */
    private String industry;

    /** 规则类型: VALIDATION校验/PROCESS流程/NOTIFICATION通知 */
    private String ruleType;

    /** 触发事件 */
    private String triggerEvent;

    /** 规则表达式 */
    private String ruleExpression;

    /** 规则动作(JSON) */
    private String ruleAction;

    private Integer priority;
    private Integer enabled;
    private String description;

    @TableField("owner_code_col")
    private String ownerCodeCol;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
