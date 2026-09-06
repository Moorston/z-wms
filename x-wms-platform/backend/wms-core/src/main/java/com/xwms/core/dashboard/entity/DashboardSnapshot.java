package com.xwms.core.dashboard.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 实时数据快照 */
@Data
@TableName("wms_dashboard_snapshot")
public class DashboardSnapshot {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String snapshotId;
    private String dashboardCode;
    private String componentCode;
    private String warehouseCode;
    private String ownerCode;

    /** 数据类型: INVENTORY_TOTAL/INVENTORY_VALUE/OUTBOUND_TODAY等 */
    private String dataType;

    private String dataKey;

    /** 数据值 */
    private BigDecimal dataValue;

    private String dataUnit;
    private String dataText;

    /** JSON数据 */
    private String dataJson;

    private LocalDateTime snapshotTime;

    /** 周期类型: REAL_TIME/HOUR/DAY/WEEK/MONTH */
    private String periodType;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
