package com.xwms.core.alert.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 预警记录 */
@Data
@TableName("wms_alert_record")
public class AlertRecord {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String alertId;
    private String ruleCode;
    private String ruleName;
    private String alertCategory;
    private String alertType;
    private String alertLevel;
    private String warehouseCode;
    private String ownerCode;
    private String skuCode;
    private String skuName;
    private String locationCode;
    private String batchNo;
    private String businessType;
    private String businessNo;

    private BigDecimal currentValue;
    private BigDecimal thresholdValue;
    private BigDecimal deviationValue;
    private BigDecimal deviationRate;

    private String alertTitle;
    private String alertContent;

    /** 状态: PENDING/PROCESSING/RESOLVED/IGNORED */
    private String status;

    private LocalDateTime triggerTime;
    private LocalDateTime resolveTime;
    private String resolvedBy;
    private String resolveNote;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
