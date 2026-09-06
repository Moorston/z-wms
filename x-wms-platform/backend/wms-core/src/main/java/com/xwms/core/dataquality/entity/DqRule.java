package com.xwms.core.dataquality.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

@Data
@TableName("wms_dq_rule")
public class DqRule {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String ruleId;
    private String ruleName;
    private String ruleCode;
    private String ruleType;
    private String ruleCategory;
    private String tableName;
    private String fieldName;
    private String description;
    private String ruleExpression;
    private String ruleConfig;
    private String severity;
    private BigDecimal threshold;
    private String isActive;
    private Integer sortOrder;
    private String createdBy;
    private String updatedBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
