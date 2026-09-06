package com.xwms.base.rule.enums;

import lombok.Getter;

/** 规则分类 */
@Getter
public enum RuleCategory {
    SYSTEM("SYSTEM", "系统规则"),
    CUSTOM("CUSTOM", "自定义规则"),
    INDUSTRY("INDUSTRY", "行业规则");

    private final String code;
    private final String desc;

    RuleCategory(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
