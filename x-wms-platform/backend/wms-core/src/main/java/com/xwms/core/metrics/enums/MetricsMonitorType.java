package com.xwms.core.metrics.enums;

import lombok.Getter;

@Getter
public enum MetricsMonitorType {
    THRESHOLD("THRESHOLD", "阈值监控"),
    TREND("TREND", "趋势监控"),
    ANOMALY("ANOMALY", "异常监控"),
    COMPARISON("COMPARISON", "对比监控"),
    TARGET("TARGET", "目标监控"),
    SLA("SLA", "SLA监控");

    private final String code;
    private final String desc;

    MetricsMonitorType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
