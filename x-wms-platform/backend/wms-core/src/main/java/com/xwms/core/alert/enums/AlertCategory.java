package com.xwms.core.alert.enums;

import lombok.Getter;

/** 预警分类 */
@Getter
public enum AlertCategory {
    INVENTORY("INVENTORY", "库存预警"),
    OPERATION("OPERATION", "作业预警"),
    SYSTEM("SYSTEM", "系统预警"),
    QUALITY("QUALITY", "质量预警"),
    SAFETY("SAFETY", "安全预警"),
    COST("COST", "成本预警");

    private final String code;
    private final String desc;

    AlertCategory(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
