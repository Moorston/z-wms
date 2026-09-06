package com.xwms.core.freeze.enums;

import lombok.Getter;

/** 解冻类型 */
@Getter
public enum UnfreezeType {
    FULL("FULL", "全部解冻"),
    PART("PART", "部分解冻"),
    AUTO("AUTO", "自动解冻");

    private final String code;
    private final String desc;

    UnfreezeType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
