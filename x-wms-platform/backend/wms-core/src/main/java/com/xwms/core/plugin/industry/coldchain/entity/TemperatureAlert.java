package com.xwms.core.plugin.industry.coldchain.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/** 温度异常报警 记录冷链温度超出阈值的报警信息 */
@Data
@TableName("wms_coldchain_temp_alert")
public class TemperatureAlert {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 报警编号 */
    private String alertNo;

    /** 关联温度记录编号 */
    private String recordNo;

    /** 仓库编码 */
    private String warehouseCode;

    /** 库区编码 */
    private String areaCode;

    /** 库位编码 */
    private String locationCode;

    /** 设备编号 */
    private String equipmentCode;

    /** 报警类型: HIGH_TEMP高温/LOW_TEMP低温/HIGH_HUMIDITY高湿/LOW_HUMIDITY低湿/EQUIPMENT_FAILURE设备故障 */
    private String alertType;

    /** 报警级别: INFO提示/WARNING警告/CRITICAL严重 */
    private String alertLevel;

    /** 当前温度 */
    private BigDecimal currentTemp;

    /** 温度上限 */
    private BigDecimal tempUpperLimit;

    /** 温度下限 */
    private BigDecimal tempLowerLimit;

    /** 当前湿度 */
    private BigDecimal currentHumidity;

    /** 持续时间(分钟) */
    private Integer durationMinutes;

    /** 报警时间 */
    private LocalDateTime alertTime;

    /** 处理状态: PENDING待处理/PROCESSING处理中/RESOLVED已解决/IGNORED已忽略 */
    private String status;

    /** 处理人 */
    private String handledBy;

    /** 处理时间 */
    private LocalDateTime handledTime;

    /** 处理结果 */
    private String handleResult;

    /** 处理备注 */
    private String handleRemark;

    /** 通知状态: NOT_NOTIFIED未通知/NOTIFIED已通知/FAILED通知失败 */
    private String notifyStatus;

    /** 通知时间 */
    private LocalDateTime notifyTime;

    /** 创建时间 */
    private LocalDateTime createdTime;

    /** 更新时间 */
    private LocalDateTime updatedTime;
}
