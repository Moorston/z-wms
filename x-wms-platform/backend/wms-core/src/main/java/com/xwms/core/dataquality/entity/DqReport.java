package com.xwms.core.dataquality.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

@Data
@TableName("wms_dq_report")
public class DqReport {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String reportId;
    private String reportName;
    private String reportType;
    private String warehouseCode;
    private String ownerCode;
    private String periodType;
    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;
    private Integer totalRules;
    private Integer executedRules;
    private Integer passRules;
    private Integer failRules;
    private BigDecimal overallScore;
    private String overallGrade;
    private String reportContent;
    private String reportSummary;
    private String status;
    private LocalDateTime generatedTime;
    private String operator;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
