package com.xwms.core.decision.enums;

import lombok.Getter;

@Getter
public enum DecisionType {
    REPLENISH("REPLENISH", "补货决策"),
    ALLOCATION("ALLOCATION", "分配决策"),
    ADJUSTMENT("ADJUSTMENT", "调整决策"),
    FREEZE("FREEZE", "冻结决策"),
    ABC_CLASSIFICATION("ABC_CLASSIFICATION", "ABC分类决策"),
    SAFETY_STOCK("SAFETY_STOCK", "安全库存决策");

    private final String code;
    private final String desc;

    DecisionType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
