package com.xwms.analytics.kpi.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** KPI指标定义 */
@Data
@TableName("wms_kpi_define")
public class KpiDefine {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String kpiCode;
    private String kpiName;

    /** 分类: EFFICIENCY/QUALITY/COST/INVENTORY/SAFETY/SERVICE */
    private String kpiCategory;

    /** 类型: RATIO/COUNT/TIME/AMOUNT */
    private String kpiType;

    /** 单位: %/件/小时/元/次 */
    private String unit;

    private String description;

    /** 计算公式 */
    private String calcFormula;

    /** 数据来源 */
    private String dataSource;

    /** 目标方向: UP越高越好/DOWN越低越好 */
    private String targetDirection;

    private Integer sortOrder;
    private Integer enabled;

    @TableField("owner_code_col")
    private String ownerCodeCol;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
