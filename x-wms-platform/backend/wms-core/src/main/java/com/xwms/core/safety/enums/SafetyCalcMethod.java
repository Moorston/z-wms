package com.xwms.core.safety.enums;

import lombok.Getter;

/** 安全库存计算方法 */
@Getter
public enum SafetyCalcMethod {
    FIXED("FIXED", "固定值"),
    STATISTICAL("STATISTICAL", "统计计算"),
    LEAD_TIME("LEAD_TIME", "提前期法"),
    SERVICE_LEVEL("SERVICE_LEVEL", "服务水平法");

    private final String code;
    private final String desc;

    SafetyCalcMethod(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
