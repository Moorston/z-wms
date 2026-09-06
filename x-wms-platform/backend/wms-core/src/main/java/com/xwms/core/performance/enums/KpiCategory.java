package com.xwms.core.performance.enums;

import lombok.Getter;

/** KPI分类 */
@Getter
public enum KpiCategory {
    INVENTORY("INVENTORY", "库存"),
    OPERATION("OPERATION", "作业"),
    COST("COST", "成本"),
    SERVICE("SERVICE", "服务"),
    QUALITY("QUALITY", "质量"),
    PEOPLE("PEOPLE", "人员");

    private final String code;
    private final String desc;

    KpiCategory(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
