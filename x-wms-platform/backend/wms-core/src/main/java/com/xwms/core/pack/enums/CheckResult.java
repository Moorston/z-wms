package com.xwms.core.pack.enums;

import lombok.Getter;

/** 复核结果 */
@Getter
public enum CheckResult {
    PASS("PASS", "复核通过"),
    FAIL("FAIL", "复核失败"),
    DIFFERENCE("DIFFERENCE", "数量差异");

    private final String code;
    private final String desc;

    CheckResult(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
