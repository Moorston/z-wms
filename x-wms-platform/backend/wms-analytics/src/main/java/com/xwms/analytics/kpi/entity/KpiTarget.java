package com.xwms.analytics.kpi.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** KPI目标 */
@Data
@TableName("wms_kpi_target")
public class KpiTarget {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String targetNo;
    private String kpiCode;
    private String kpiName;

    /** 目标周期: DAILY/MONTHLY/QUARTERLY/ANNUAL */
    private String targetPeriod;

    /** 期间值: 202608/2026Q3/2026 */
    private String periodValue;

    private String warehouseCode;
    private String department;

    /** 目标值 */
    private BigDecimal targetValue;

    /** 挑战值 */
    private BigDecimal challengeValue;

    /** 基准值 */
    private BigDecimal baselineValue;

    /** 状态: ACTIVE/EXPIRED */
    private String status;

    private String remark;

    @TableField("owner_code_col")
    private String ownerCodeCol;

    private String createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
