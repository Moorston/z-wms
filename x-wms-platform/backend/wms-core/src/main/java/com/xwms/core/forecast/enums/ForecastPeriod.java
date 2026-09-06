package com.xwms.core.forecast.enums;

import lombok.Getter;

/** 预测周期 */
@Getter
public enum ForecastPeriod {
    DAILY("DAILY", "日"),
    WEEKLY("WEEKLY", "周"),
    MONTHLY("MONTHLY", "月"),
    QUARTERLY("QUARTERLY", "季");

    private final String code;
    private final String desc;

    ForecastPeriod(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
