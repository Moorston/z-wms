package com.xwms.core.datamart.enums;

import lombok.Getter;

@Getter
public enum MetricType {
    QUANTITY("QUANTITY", "数量型指标"),
    AMOUNT("AMOUNT", "金额型指标"),
    RATE("RATE", "比率型指标"),
    TIME("TIME", "时间型指标"),
    EFFICIENCY("EFFICIENCY", "效率型指标"),
    QUALITY("QUALITY", "质量型指标"),
    COST("COST", "成本型指标"),
    CUSTOM("CUSTOM", "自定义指标");

    private final String code;
    private final String desc;

    MetricType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
