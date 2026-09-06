package com.xwms.core.equipment.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 设备使用记录 */
@Data
@TableName("wms_equipment_usage")
public class EquipmentUsage {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long equipmentId;
    private String equipmentCode;

    /** 任务类型: INBOUND/OUTBOUND/MOVE/COUNT/VAS */
    private String taskType;

    /** 关联任务号 */
    private String taskNo;

    /** 操作人 */
    private String operator;

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    /** 使用时长(分钟) */
    private Integer durationMin;

    /** 作业次数 */
    private Integer operationCount;

    private String startLocation;
    private String endLocation;

    private BigDecimal startBattery;
    private BigDecimal endBattery;

    private String remark;

    @TableField("owner_code_col")
    private String ownerCodeCol;

    @TableField("warehouse_code_col")
    private String warehouseCodeCol;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
