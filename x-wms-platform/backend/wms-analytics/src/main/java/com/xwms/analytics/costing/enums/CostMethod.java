package com.xwms.analytics.costing.enums;

import lombok.Getter;

/** 鎴愭湰鏍哥畻鏂规硶 */
@Getter
public enum CostMethod {
    FIFO("FIFO", "鍏堣繘鍏堝嚭"),
    WEIGHTED_AVG("WEIGHTED_AVG", "鍔犳潈骞冲潎"),
    MOVING_AVG("MOVING_AVG", "绉诲姩骞冲潎"),
    STANDARD("STANDARD", "鏍囧噯鎴愭湰");

    private final String code;
    private final String desc;

    CostMethod(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
