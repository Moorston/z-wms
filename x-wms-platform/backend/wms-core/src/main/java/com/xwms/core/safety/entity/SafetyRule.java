package com.xwms.core.safety.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 安全库存规则 */
@Data
@TableName("wms_safety_rule")
public class SafetyRule {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String ruleCode;
    private String ruleName;
    private String warehouseCode;
    private String ownerCode;
    private String skuCode;
    private String categoryCode;
    private String abcClass;

    /** 计算方法: FIXED/STATISTICAL/LEAD_TIME/SERVICE_LEVEL */
    private String calcMethod;

    /** 固定安全库存 */
    private BigDecimal fixedSafety;

    /** 固定补货点 */
    private BigDecimal fixedReorder;

    /** 固定最高库存 */
    private BigDecimal fixedMax;

    /** 服务水平(%) */
    private BigDecimal serviceLevel;

    /** 提前期(天) */
    private Integer leadTimeDays;

    /** 盘点周期(天) */
    private Integer reviewPeriod;

    /** Z值(服务水平对应) */
    private BigDecimal zScore;

    /** 状态: ACTIVE/INACTIVE */
    private String status;

    private Integer priority;
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
