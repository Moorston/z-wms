package com.xwms.core.performance.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 绩效日报 */
@Data
@TableName("wms_performance_daily")
public class PerformanceDaily {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long employeeId;
    private String employeeNo;
    private String employeeName;
    private String warehouseCode;

    /** 工作日期 */
    private LocalDate workDate;

    /** 工作时长(小时) */
    private BigDecimal workHours;

    private BigDecimal receivingQty;
    private BigDecimal putawayQty;
    private BigDecimal pickingQty;
    private BigDecimal checkingQty;
    private BigDecimal packingQty;
    private BigDecimal countingQty;
    private BigDecimal movingQty;
    private BigDecimal vasQty;
    private BigDecimal replenishQty;

    /** 总作业量 */
    private BigDecimal totalQty;

    /** 总耗时(分钟) */
    private Integer totalDuration;

    /** 效率(件/小时) */
    private BigDecimal efficiency;

    /** 总差错 */
    private Integer errorCount;

    /** 差错率 */
    private BigDecimal errorRate;

    /** 平均质量分 */
    private BigDecimal qualityScore;

    /** 综合绩效分(0-100) */
    private BigDecimal performanceScore;

    /** 绩效等级: S/A/B/C/D */
    private String performanceLevel;

    /** 当日工资 */
    private BigDecimal salaryAmount;

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
