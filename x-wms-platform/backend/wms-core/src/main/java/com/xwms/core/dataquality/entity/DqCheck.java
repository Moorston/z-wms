package com.xwms.core.dataquality.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

@Data
@TableName("wms_dq_check")
public class DqCheck {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String checkId;
    private String checkName;
    private String ruleId;
    private String ruleCode;
    private String ruleName;
    private String warehouseCode;
    private String ownerCode;
    private String checkType;
    private String checkConfig;
    private Long totalCount;
    private Long passCount;
    private Long failCount;
    private BigDecimal passRate;
    private BigDecimal failRate;
    private String checkResult;
    private String failDetails;
    private String status;
    private String errorMessage;
    private LocalDateTime checkStartTime;
    private LocalDateTime checkEndTime;
    private Long durationMs;
    private String operator;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
