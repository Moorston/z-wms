package com.xwms.analytics.kpi.enums;

import lombok.Getter;

/** KPI指标分类 */
@Getter
public enum KpiCategory {
    EFFICIENCY("EFFICIENCY", "效率"),
    QUALITY("QUALITY", "质量"),
    COST("COST", "成本"),
    INVENTORY("INVENTORY", "库存"),
    SAFETY("SAFETY", "安全"),
    SERVICE("SERVICE", "服务");

    private final String code;
    private final String desc;

    KpiCategory(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
