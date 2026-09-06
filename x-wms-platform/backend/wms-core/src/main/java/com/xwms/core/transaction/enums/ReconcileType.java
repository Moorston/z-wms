package com.xwms.core.transaction.enums;

import lombok.Getter;

/** 对账类型 */
@Getter
public enum ReconcileType {
    DAILY("DAILY", "日结"),
    MONTHLY("MONTHLY", "月结"),
    MANUAL("MANUAL", "手动");

    private final String code;
    private final String desc;

    ReconcileType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
