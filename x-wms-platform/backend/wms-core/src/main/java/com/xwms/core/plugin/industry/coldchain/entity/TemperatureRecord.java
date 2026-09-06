package com.xwms.core.plugin.industry.coldchain.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/** 温度监控记录 记录冷链仓库、库位、设备的温度数据 */
@Data
@TableName("wms_coldchain_temp_record")
public class TemperatureRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 记录编号 */
    private String recordNo;

    /** 仓库编码 */
    private String warehouseCode;

    /** 库区编码 */
    private String areaCode;

    /** 库位编码 */
    private String locationCode;

    /** 设备编号 */
    private String equipmentCode;

    /** 温度(摄氏度) */
    private BigDecimal temperature;

    /** 湿度(%) */
    private BigDecimal humidity;

    /** 温度类型: REFRIGERATED冷藏/FROZEN冷冻/AMBIENT常温/CONTROLLED恒温 */
    private String temperatureType;

    /** 采集时间 */
    private LocalDateTime collectTime;

    /** 采集方式: SENSOR自动传感器/MANUAL人工录入 */
    private String collectType;

    /** 采集人 */
    private String collector;

    /** 状态: NORMAL正常/ABNORMAL异常 */
    private String status;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createdTime;
}
