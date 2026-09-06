package com.xwms.core.expiry.enums;

import lombok.Getter;

/** 预警级别 */
@Getter
public enum WarningLevel {
    NORMAL("NORMAL", "正常"),
    W1("W1", "一级预警"),
    W2("W2", "二级预警"),
    W3("W3", "三级预警"),
    EXPIRED("EXPIRED", "已过期");

    private final String code;
    private final String desc;

    WarningLevel(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
