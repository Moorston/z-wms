package com.xwms.core.equipment.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.baomidou.mybatisplus.annotation.*;

import com.xwms.common.core.BaseEntity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/** 设备档案 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_equipment")
public class Equipment extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String equipmentCode;
    private String equipmentName;

    /**
     * 设备类型:
     * FORKLIFT/PALLET_JACK/CONVEYOR/SORTER/AGV/RFID_READER/BARCODE_SCANNER/PRINTER/WEIGHING_SCALE/PDA/OTHER
     */
    private String equipmentType;

    private String brand;
    private String model;
    private String serialNo;
    private String warehouseCode;
    private String areaCode;

    /** 当前位置 */
    private String locationCode;

    /** 状态: IDLE/IN_USE/MAINTENANCE/FAULT/RETIRED */
    private String status;

    /** 运行状态: RUNNING/STANDBY/STOPPED/ERROR */
    private String runStatus;

    private LocalDate purchaseDate;
    private LocalDate warrantyEnd;
    private LocalDate lastMaintain;
    private LocalDate nextMaintain;

    /** 维护周期(天) */
    private Integer maintainCycleDays;

    /** 累计运行小时 */
    private BigDecimal totalRunHours;

    /** 累计作业次数 */
    private Long totalOperations;

    /** 最大载重(吨) */
    private BigDecimal maxLoad;

    /** 最大速度 */
    private BigDecimal maxSpeed;

    /** 电量(%) */
    private BigDecimal batteryLevel;

    /** IP地址 */
    private String ipAddress;

    /** WCS设备ID */
    private String wcsDeviceId;

    /** 通信协议: TCP/MQTT/OPC_UA/MODBUS */
    private String protocol;

    private String remark;

    @TableField("owner_code_col")
    private String ownerCodeCol;

    @TableField("warehouse_code_col")
    private String warehouseCodeCol;
}
