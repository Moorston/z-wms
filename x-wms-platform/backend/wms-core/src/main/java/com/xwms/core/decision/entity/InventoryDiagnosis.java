package com.xwms.core.decision.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

@Data
@TableName("wms_inventory_diagnosis")
public class InventoryDiagnosis {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String diagnosisId;
    private String warehouseCode;
    private String ownerCode;
    private String diagnosisType;
    private String diagnosisName;
    private String diagnosisScope;
    private String scopeCode;
    private BigDecimal currentValue;
    private BigDecimal benchmarkValue;
    private BigDecimal targetValue;
    private BigDecimal deviation;
    private BigDecimal deviationRate;
    private String severity;
    private String rootCause;
    private String diagnosisResult;
    private String recommendations;
    private String actionPlan;
    private String priority;
    private String status;
    private String handler;
    private LocalDateTime handleTime;
    private String handleResult;
    private String operator;
    private LocalDateTime operateTime;
    private LocalDateTime diagnosisTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
