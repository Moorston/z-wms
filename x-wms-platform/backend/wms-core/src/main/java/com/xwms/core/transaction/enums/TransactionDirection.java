package com.xwms.core.transaction.enums;

import lombok.Getter;

/** 流水方向 */
@Getter
public enum TransactionDirection {
    IN("IN", "增加"),
    OUT("OUT", "减少"),
    NONE("NONE", "无变化");

    private final String code;
    private final String desc;

    TransactionDirection(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
