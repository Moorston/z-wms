package com.xwms.analytics.costing.enums;

import lombok.Getter;

/** 成本核算方法 */
@Getter
public enum CostingMethod {
    FIFO("FIFO", "先进先出"),
    LIFO("LIFO", "后进先出"),
    WEIGHTED_AVG("WEIGHTED_AVG", "加权平均"),
    MOVING_AVG("MOVING_AVG", "移动平均"),
    STANDARD("STANDARD", "标准成本"),
    SPECIFIC("SPECIFIC", "个别计价");

    private final String code;
    private final String desc;

    CostingMethod(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
