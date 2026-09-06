package com.xwms.core.plugin.industry.coldchain.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/** 冷链设备管理 管理冷库、冷藏车、温度传感器等冷链设备 */
@Data
@TableName("wms_coldchain_equipment")
public class ColdChainEquipment {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 设备编号 */
    private String equipmentCode;

    /** 设备名称 */
    private String equipmentName;

    /**
     * 设备类型:
     * REFRIGERATOR冷库/FREEZER冻库/COLD_TRUCK冷藏车/TEMP_SENSOR温度传感器/HUMIDITY_SENSOR湿度传感器/CONTROLLER温控器
     */
    private String equipmentType;

    /** 设备品牌 */
    private String brand;

    /** 设备型号 */
    private String model;

    /** 设备序列号 */
    private String serialNumber;

    /** 所属仓库 */
    private String warehouseCode;

    /** 所属库区 */
    private String areaCode;

    /** 所属库位 */
    private String locationCode;

    /** 温度类型: REFRIGERATED冷藏/FROZEN冷冻/AMBIENT常温/CONTROLLED恒温 */
    private String temperatureType;

    /** 温度上限 */
    private BigDecimal tempUpperLimit;

    /** 温度下限 */
    private BigDecimal tempLowerLimit;

    /** 湿度上限 */
    private BigDecimal humidityUpperLimit;

    /** 湿度下限 */
    private BigDecimal humidityLowerLimit;

    /** 当前温度 */
    private BigDecimal currentTemp;

    /** 当前湿度 */
    private BigDecimal currentHumidity;

    /** 运行状态: RUNNING运行/STANDBY待机/MAINTENANCE维护/FAULT故障/STOPPED停用 */
    private String runStatus;

    /** 启用状态: ACTIVE启用/INACTIVE停用 */
    private String status;

    /** 安装日期 */
    private LocalDateTime installDate;

    /** 上次维护日期 */
    private LocalDateTime lastMaintainDate;

    /** 下次维护日期 */
    private LocalDateTime nextMaintainDate;

    /** 负责人 */
    private String responsiblePerson;

    /** 联系电话 */
    private String contactPhone;

    /** 备注 */
    private String remark;

    /** 创建人 */
    private String createdBy;

    /** 创建时间 */
    private LocalDateTime createdTime;

    /** 更新人 */
    private String updatedBy;

    /** 更新时间 */
    private LocalDateTime updatedTime;
}
