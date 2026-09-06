package com.xwms.core.dashboard.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 看板组件 */
@Data
@TableName("wms_dashboard_component")
public class DashboardComponent {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String componentCode;
    private String componentName;

    /** 组件类型: LINE/BAR/PIE/GAUGE/TABLE/TEXT/NUMBER/MAP/SCATTER/RADAR */
    private String componentType;

    /** 数据源: INVENTORY/OUTBOUND/INBOUND/OPERATION/KPI/EQUIPMENT/STAFF */
    private String dataSource;

    /** 数据查询SQL/API */
    private String dataQuery;

    /** 刷新间隔(秒) */
    private Integer refreshInterval;

    private Integer positionX;
    private Integer positionY;
    private Integer width;
    private Integer height;

    private String title;
    private String titleColor;
    private String backgroundColor;
    private String borderColor;
    private Integer borderRadius;

    /** 组件配置(JSON) */
    private String componentConfig;

    private String status;
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
