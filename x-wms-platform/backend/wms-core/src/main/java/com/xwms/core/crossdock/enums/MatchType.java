package com.xwms.core.crossdock.enums;

import lombok.Getter;

/** 越库匹配类型 */
@Getter
public enum MatchType {
    EXACT("EXACT", "精确匹配"),
    SUBSTITUTE("SUBSTITUTE", "替代匹配"),
    PARTIAL("PARTIAL", "部分匹配");

    private final String code;
    private final String desc;

    MatchType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
