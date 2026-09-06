package com.xwms.core.decision.enums;

import lombok.Getter;

@Getter
public enum OptimizationType {
    LAYOUT("LAYOUT", "库位布局优化"),
    ROUTING("ROUTING", "路径优化"),
    ALLOCATION("ALLOCATION", "分配优化"),
    REPLENISH("REPLENISH", "补货优化"),
    ABC_CLASSIFICATION("ABC_CLASSIFICATION", "ABC分类优化"),
    SAFETY_STOCK("SAFETY_STOCK", "安全库存优化"),
    TURNOVER("TURNOVER", "周转优化");

    private final String code;
    private final String desc;

    OptimizationType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
