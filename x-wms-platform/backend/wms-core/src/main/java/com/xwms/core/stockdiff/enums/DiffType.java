package com.xwms.core.stockdiff.enums;

import lombok.Getter;

/** 差异类型 */
@Getter
public enum DiffType {
    SHORTAGE("SHORTAGE", "盘亏"),
    OVERAGE("OVERAGE", "盘盈"),
    DAMAGE("DAMAGE", "损坏"),
    EXPIRED("EXPIRED", "过期"),
    OTHER("OTHER", "其他");

    private final String code;
    private final String desc;

    DiffType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
