package com.xwms.analytics.costing.enums;

import lombok.Getter;

/** 成本核算类型 */
@Getter
public enum CostCalculateType {
    MONTH_END("MONTH_END", "月末核算"),
    QUARTER_END("QUARTER_END", "季末核算"),
    YEAR_END("YEAR_END", "年末核算"),
    REAL_TIME("REAL_TIME", "实时核算");

    private final String code;
    private final String desc;

    CostCalculateType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
