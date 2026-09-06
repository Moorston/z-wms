package com.xwms.core.simulation.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

@Data
@TableName("wms_scheme_evaluation")
public class SchemeEvaluation {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String evaluationId;
    private String evaluationName;
    private String warehouseCode;
    private String ownerCode;
    private String schemeType;
    private String description;
    private String schemeAConfig;
    private String schemeBConfig;
    private String schemeAResult;
    private String schemeBResult;
    private String comparisonResult;
    private String evaluationMetrics;
    private String recommendation;
    private String recommendationReason;
    private String status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long durationMs;
    private String operator;
    private String approver;
    private LocalDateTime approveTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
