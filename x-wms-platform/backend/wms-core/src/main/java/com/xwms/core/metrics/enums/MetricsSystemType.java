package com.xwms.core.metrics.enums;

import lombok.Getter;

@Getter
public enum MetricsSystemType {
    INVENTORY("INVENTORY", "库存指标体系"),
    OPERATION("OPERATION", "作业指标体系"),
    SERVICE("SERVICE", "服务指标体系"),
    COST("COST", "成本指标体系"),
    QUALITY("QUALITY", "质量指标体系"),
    EFFICIENCY("EFFICIENCY", "效率指标体系"),
    COMPREHENSIVE("COMPREHENSIVE", "综合指标体系");

    private final String code;
    private final String desc;

    MetricsSystemType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
