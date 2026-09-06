package com.xwms.core.performance.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 绩效考核 */
@Data
@TableName("wms_performance_assess")
public class PerformanceAssess {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String assessNo;
    private Long employeeId;
    private String employeeNo;
    private String employeeName;
    private String warehouseCode;

    /** 考核期间: 202608(月)/2026Q3(季)/2026(年) */
    private String assessPeriod;

    /** 考核类型: MONTHLY月度/QUARTERLY季度/ANNUAL年度 */
    private String assessType;

    private Integer workDays;
    private BigDecimal totalHours;
    private BigDecimal totalQty;
    private BigDecimal avgEfficiency;
    private BigDecimal errorRate;
    private BigDecimal qualityScore;
    private BigDecimal attendanceScore;
    private BigDecimal performanceScore;
    private String performanceLevel;

    /** 部门排名 */
    private Integer rankInDept;

    /** 部门总人数 */
    private Integer totalEmployees;

    private BigDecimal salaryAmount;
    private BigDecimal bonusAmount;

    private String assessBy;
    private LocalDateTime assessTime;

    /** 状态: DRAFT草稿/CONFIRMED已确认/PUBLISHED已发布 */
    private String status;

    private String remark;

    @TableField("owner_code_col")
    private String ownerCodeCol;

    @TableField("warehouse_code_col")
    private String warehouseCodeCol;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
