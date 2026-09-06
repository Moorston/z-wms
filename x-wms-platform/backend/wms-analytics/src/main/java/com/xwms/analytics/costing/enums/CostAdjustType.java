package com.xwms.analytics.costing.enums;

import lombok.Getter;

/** 成本调整类型 */
@Getter
public enum CostAdjustType {
    PRICE_ADJUST("PRICE_ADJUST", "价格调整"),
    QUANTITY_ADJUST("QUANTITY_ADJUST", "数量调整"),
    DIFFERENCE_ADJUST("DIFFERENCE_ADJUST", "差异调整"),
    WRITE_DOWN("WRITE_DOWN", "减值"),
    WRITE_OFF("WRITE_OFF", "核销");

    private final String code;
    private final String desc;

    CostAdjustType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
