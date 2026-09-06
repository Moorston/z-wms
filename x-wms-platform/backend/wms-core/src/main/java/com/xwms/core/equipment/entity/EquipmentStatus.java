package com.xwms.core.equipment.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 设备状态实时 */
@Data
@TableName("wms_equipment_status")
public class EquipmentStatus {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long equipmentId;
    private String equipmentCode;

    /** 运行状态: RUNNING/STANDBY/STOPPED/ERROR */
    private String runStatus;

    /** 工作状态: IDLE/WORKING/CHARGING/MAINTENANCE */
    private String workStatus;

    /** 当前任务号 */
    private String currentTaskNo;

    /** 当前位置 */
    private String currentLocation;

    /** 目标位置 */
    private String targetLocation;

    /** 电量 */
    private BigDecimal batteryLevel;

    /** 当前速度 */
    private BigDecimal speed;

    /** 当前载重 */
    private BigDecimal loadWeight;

    /** 故障代码 */
    private String errorCode;

    /** 故障信息 */
    private String errorMsg;

    /** 信号强度 */
    private BigDecimal signalStrength;

    /** 最后心跳时间 */
    private LocalDateTime lastHeartbeat;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
