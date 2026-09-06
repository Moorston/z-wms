package com.xwms.core.combo.enums;

import lombok.Getter;

/** 成本计算方法 */
@Getter
public enum CostCalcMethod {
    SUM("SUM", "子品成本汇总"),
    FIXED("FIXED", "固定成本"),
    WEIGHTED("WEIGHTED", "加权平均");

    private final String code;
    private final String desc;

    CostCalcMethod(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
