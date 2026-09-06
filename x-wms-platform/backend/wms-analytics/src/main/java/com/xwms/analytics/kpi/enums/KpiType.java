package com.xwms.analytics.kpi.enums;

import lombok.Getter;

/** KPI指标类型 */
@Getter
public enum KpiType {
    RATIO("RATIO", "比率"),
    COUNT("COUNT", "数量"),
    TIME("TIME", "时间"),
    AMOUNT("AMOUNT", "金额");

    private final String code;
    private final String desc;

    KpiType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
