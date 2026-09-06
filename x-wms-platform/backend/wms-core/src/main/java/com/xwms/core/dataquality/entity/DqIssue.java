package com.xwms.core.dataquality.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

@Data
@TableName("wms_dq_issue")
public class DqIssue {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String issueId;
    private String checkId;
    private String ruleId;
    private String ruleCode;
    private String ruleName;
    private String warehouseCode;
    private String ownerCode;
    private String tableName;
    private String fieldName;
    private String issueType;
    private String issueDescription;
    private String issueData;
    private String severity;
    private String status;
    private String priority;
    private String assignee;
    private String fixPlan;
    private String fixResult;
    private LocalDateTime fixTime;
    private LocalDateTime verifiedTime;
    private String verifier;
    private Integer reopenCount;
    private LocalDateTime dueTime;
    private String operator;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
