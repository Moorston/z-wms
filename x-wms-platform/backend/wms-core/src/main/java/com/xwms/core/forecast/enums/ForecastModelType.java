package com.xwms.core.forecast.enums;

import lombok.Getter;

/** 预测模型类型 */
@Getter
public enum ForecastModelType {
    MOVING_AVG("MOVING_AVG", "移动平均"),
    EXPONENTIAL_SMOOTHING("EXPONENTIAL_SMOOTHING", "指数平滑"),
    ARIMA("ARIMA", "时间序列"),
    LSTM("LSTM", "神经网络"),
    XGBOOST("XGBOOST", "机器学习"),
    PROPHET("PROPHET", "Prophet预测");

    private final String code;
    private final String desc;

    ForecastModelType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
