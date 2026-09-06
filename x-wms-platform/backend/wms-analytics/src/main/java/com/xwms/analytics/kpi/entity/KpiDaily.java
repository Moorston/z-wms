package com.xwms.analytics.kpi.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** KPI日数据 */
@Data
@TableName("wms_kpi_daily")
public class KpiDaily {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String kpiCode;
    private String kpiName;
    private String kpiCategory;

    /** 统计日期 */
    private LocalDate statDate;

    private String warehouseCode;
    private String department;
    private String employeeId;

    /** 实际值 */
    private BigDecimal actualValue;

    /** 目标值 */
    private BigDecimal targetValue;

    /** 达成率 */
    private BigDecimal targetRate;

    /** 同比值 */
    private BigDecimal compareValue;

    /** 同比增长率 */
    private BigDecimal compareRate;

    /** 环比值 */
    private BigDecimal ringValue;

    /** 环比增长率 */
    private BigDecimal ringRate;

    /** 明细数据(JSON) */
    private String dataDetail;

    @TableField("owner_code_col")
    private String ownerCodeCol;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
