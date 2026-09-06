package com.xwms.analytics.kpi.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** KPI报表配置 */
@Data
@TableName("wms_kpi_report")
public class KpiReport {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String reportCode;
    private String reportName;

    /** 报表类型: DAILY/WEEKLY/MONTHLY/QUARTERLY/ANNUAL/CUSTOM */
    private String reportType;

    /** 包含的KPI编码(JSON数组) */
    private String kpiCodes;

    /** 分析维度(JSON数组): 仓库/部门/人员/时间 */
    private String dimensions;

    /** 图表类型: TABLE/LINE/BAR/PIE */
    private String chartType;

    /** 定时生成cron */
    private String scheduleCron;

    /** 最后生成时间 */
    private LocalDateTime lastGenerateTime;

    private Integer enabled;

    @TableField("owner_code_col")
    private String ownerCodeCol;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
