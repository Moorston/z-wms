package com.xwms.core.forecast.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 预测模型 */
@Data
@TableName("wms_forecast_model")
public class ForecastModel {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String modelCode;
    private String modelName;

    /** 模型类型: MOVING_AVG/EXPONENTIAL_SMOOTHING/ARIMA/LSTM/XGBOOST/PROPHET */
    private String modelType;

    private String algorithm;
    private String version;
    private String description;

    /** 模型参数(JSON) */
    private String parameters;

    /** 超参数(JSON) */
    private String hyperparameters;

    /** 特征列表(JSON) */
    private String featureList;

    private LocalDate trainingDataStart;
    private LocalDate trainingDataEnd;
    private Integer trainingSamples;

    /** 准确率(%) */
    private BigDecimal accuracy;

    /** 平均绝对误差 */
    private BigDecimal mae;

    /** 均方根误差 */
    private BigDecimal rmse;

    /** 平均绝对百分比误差 */
    private BigDecimal mape;

    private LocalDateTime trainingTime;
    private Long trainingDuration;
    private String modelPath;
    private Long modelSize;

    /** 框架: TensorFlow/PyTorch/Scikit-learn/Statsmodels */
    private String framework;

    /** 状态: DRAFT/TRAINING/ACTIVE/INACTIVE/RETIRED */
    private String status;

    /** 是否默认: Y/N */
    private String isDefault;

    private String createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
