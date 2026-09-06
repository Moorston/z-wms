package com.xwms.core.dataquality.enums;

import lombok.Getter;

@Getter
public enum DqRuleType {
    COMPLETENESS("COMPLETENESS", "完整性规则"),
    ACCURACY("ACCURACY", "准确性规则"),
    CONSISTENCY("CONSISTENCY", "一致性规则"),
    UNIQUENESS("UNIQUENESS", "唯一性规则"),
    VALIDITY("VALIDITY", "有效性规则"),
    TIMELINESS("TIMELINESS", "及时性规则"),
    CUSTOM("CUSTOM", "自定义规则");

    private final String code;
    private final String desc;

    DqRuleType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
