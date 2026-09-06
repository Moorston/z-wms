package com.xwms.core.plugin.industry.gsp.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/** GSP温度日志 药品经营质量管理规范(GSP)温湿度监测记录 */
@Data
@TableName("wms_gsp_temp_log")
public class GspTemperatureLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 日志编号 */
    private String logNo;

    /** 仓库编码 */
    private String warehouseCode;

    /** 库区编码 */
    private String areaCode;

    /** 库位编码 */
    private String locationCode;

    /** 设备编号 */
    private String equipmentCode;

    /** 监测点名称 */
    private String monitorPointName;

    /** 温度(℃) */
    private BigDecimal temperature;

    /** 湿度(%) */
    private BigDecimal humidity;

    /** 温度上限 */
    private BigDecimal tempUpperLimit;

    /** 温度下限 */
    private BigDecimal tempLowerLimit;

    /** 湿度上限 */
    private BigDecimal humidityUpperLimit;

    /** 湿度下限 */
    private BigDecimal humidityLowerLimit;

    /** 温度状态: NORMAL正常/HIGH偏高/LOW偏低/EXCEEDED超标 */
    private String tempStatus;

    /** 湿度状态: NORMAL正常/HIGH偏高/LOW偏低/EXCEEDED超标 */
    private String humidityStatus;

    /** 超标持续时间(分钟) */
    private Integer exceedDuration;

    /** 采集时间 */
    private LocalDateTime collectTime;

    /** 采集方式: AUTO自动/MANUAL人工 */
    private String collectType;

    /** 采集人 */
    private String collector;

    /** 是否报警: Y是/N否 */
    private String isAlert;

    /** 报警编号 */
    private String alertNo;

    /** 报警级别: INFO提示/WARNING警告/CRITICAL严重 */
    private String alertLevel;

    /** 处理状态: PENDING待处理/PROCESSING处理中/RESOLVED已解决 */
    private String handleStatus;

    /** 处理人 */
    private String handledBy;

    /** 处理时间 */
    private LocalDateTime handledTime;

    /** 处理措施 */
    private String handleMeasure;

    /** 处理结果 */
    private String handleResult;

    /** 储存条件: COOL阴凉(不超过20℃)/COLD冷藏(2-8℃)/FROZEN冷冻(不超过-10℃)/NORMAL常温(10-30℃) */
    private String storageCondition;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createdTime;
}
