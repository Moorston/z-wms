package com.xwms.core.plugin.industry.coldchain.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/** 温区管理 定义仓库中的不同温度区域及其参数 */
@Data
@TableName("wms_coldchain_temp_zone")
public class TemperatureZone {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 温区编码 */
    private String zoneCode;

    /** 温区名称 */
    private String zoneName;

    /** 仓库编码 */
    private String warehouseCode;

    /** 库区编码列表(逗号分隔) */
    private String areaCodes;

    /**
     * 温度类型:
     * FROZEN冷冻(-18℃以下)/REFRIGERATED冷藏(0-8℃)/COOL凉藏(8-15℃)/AMBIENT常温(15-25℃)/CONTROLLED恒温(指定温度)
     */
    private String temperatureType;

    /** 目标温度 */
    private BigDecimal targetTemp;

    /** 温度上限 */
    private BigDecimal tempUpperLimit;

    /** 温度下限 */
    private BigDecimal tempLowerLimit;

    /** 目标湿度 */
    private BigDecimal targetHumidity;

    /** 湿度上限 */
    private BigDecimal humidityUpperLimit;

    /** 湿度下限 */
    private BigDecimal humidityLowerLimit;

    /** 允许温度波动范围(±℃) */
    private BigDecimal tempTolerance;

    /** 报警延迟时间(分钟)，超过此时间才报警 */
    private Integer alertDelayMinutes;

    /** 关联设备编号列表(逗号分隔) */
    private String equipmentCodes;

    /** 状态: ACTIVE启用/INACTIVE停用 */
    private String status;

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
