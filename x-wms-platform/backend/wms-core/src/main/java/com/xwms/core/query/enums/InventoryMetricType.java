package com.xwms.core.query.enums;

import lombok.Getter;

/** 库存指标类型 */
@Getter
public enum InventoryMetricType {
    TURNOVER("TURNOVER", "周转率"),
    ACCURACY("ACCURACY", "准确率"),
    OBSOLETE("OBSOLETE", "呆滞库存"),
    ABC("ABC", "ABC分类"),
    STOCKOUT("STOCKOUT", "缺货率"),
    FILL_RATE("FILL_RATE", "满足率"),
    CARRYING_COST("CARRYING_COST", "持有成本");

    private final String code;
    private final String desc;

    InventoryMetricType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
