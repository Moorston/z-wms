package com.xwms.core.qc.enums;

import lombok.Getter;

/** 质检结果 */
@Getter
public enum QcResult {
    PASSED("PASSED", "合格"),
    FAILED("FAILED", "不合格"),
    CONCESSION("CONCESSION", "让步接收");

    private final String code;
    private final String desc;

    QcResult(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
