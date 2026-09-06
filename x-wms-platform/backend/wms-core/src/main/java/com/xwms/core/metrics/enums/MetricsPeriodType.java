package com.xwms.core.metrics.enums;

import lombok.Getter;

@Getter
public enum MetricsPeriodType {
    REAL_TIME("REAL_TIME", "实时"),
    HOURLY("HOURLY", "小时"),
    DAILY("DAILY", "日"),
    WEEKLY("WEEKLY", "周"),
    MONTHLY("MONTHLY", "月"),
    QUARTERLY("QUARTERLY", "季"),
    YEARLY("YEARLY", "年");

    private final String code;
    private final String desc;

    MetricsPeriodType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
