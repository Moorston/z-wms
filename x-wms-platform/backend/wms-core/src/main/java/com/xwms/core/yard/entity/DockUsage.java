package com.xwms.core.yard.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 月台使用记录 */
@Data
@TableName("wms_dock_usage")
public class DockUsage {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long dockId;
    private String dockCode;
    private Long appointmentId;
    private String appointNo;
    private String warehouseCode;
    private String plateNo;
    private String carrierName;

    /** 使用类型: INBOUND/OUTBOUND/RETURN */
    private String usageType;

    /** 占用开始 */
    private LocalDateTime occupyStart;

    /** 占用结束 */
    private LocalDateTime occupyEnd;

    /** 占用时长(分钟) */
    private Integer durationMin;

    /** 状态: ACTIVE/COMPLETED */
    private String status;

    private String remark;

    @TableField("owner_code_col")
    private String ownerCodeCol;

    @TableField("warehouse_code_col")
    private String warehouseCodeCol;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
