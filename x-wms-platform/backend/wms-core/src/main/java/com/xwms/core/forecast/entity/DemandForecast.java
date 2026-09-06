package com.xwms.core.forecast.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 需求预测 */
@Data
@TableName("wms_demand_forecast")
public class DemandForecast {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String forecastId;
    private String forecastName;
    private String modelCode;
    private String warehouseCode;
    private String ownerCode;
    private String skuCode;
    private String categoryCode;

    /** 预测周期: DAILY/WEEKLY/MONTHLY/QUARTERLY */
    private String forecastPeriod;

    /** 预测期数 */
    private Integer forecastHorizon;

    private LocalDate historyStart;
    private LocalDate historyEnd;
    private LocalDate forecastStart;
    private LocalDate forecastEnd;

    private BigDecimal actualQuantity;
    private BigDecimal forecastQuantity;
    private BigDecimal lowerBound;
    private BigDecimal upperBound;

    /** 置信度(%) */
    private BigDecimal confidence;

    /** 平均绝对误差 */
    private BigDecimal mae;

    /** 均方根误差 */
    private BigDecimal rmse;

    /** 平均绝对百分比误差 */
    private BigDecimal mape;

    /** 预测数据(JSON数组) */
    private String forecastData;

    /** 实际数据(JSON数组) */
    private String actualData;

    /** 状态: PENDING/COMPLETED/FAILED */
    private String status;

    private String errorMessage;
    private String operator;
    private LocalDateTime forecastTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
