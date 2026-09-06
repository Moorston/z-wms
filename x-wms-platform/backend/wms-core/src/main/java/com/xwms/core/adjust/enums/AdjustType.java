package com.xwms.core.adjust.enums;

import lombok.Getter;

/** 库存调整类型 */
@Getter
public enum AdjustType {
    PROFIT("PROFIT", "盘盈"),
    LOSS("LOSS", "盘亏"),
    DAMAGE("DAMAGE", "损坏"),
    EXPIRE("EXPIRE", "过期"),
    TRANSFER("TRANSFER", "调拨差异"),
    MANUAL("MANUAL", "手工调整");

    private final String code;
    private final String desc;

    AdjustType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
