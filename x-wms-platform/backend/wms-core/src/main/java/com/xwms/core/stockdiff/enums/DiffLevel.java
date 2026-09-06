package com.xwms.core.stockdiff.enums;

import lombok.Getter;

/** 差异级别 */
@Getter
public enum DiffLevel {
    MINOR("MINOR", "轻微"),
    NORMAL("NORMAL", "正常"),
    MAJOR("MAJOR", "重大"),
    CRITICAL("CRITICAL", "严重");

    private final String code;
    private final String desc;

    DiffLevel(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
