package com.xwms.core.dataquality.enums;

import lombok.Getter;

@Getter
public enum DqIssueStatus {
    OPEN("OPEN", "待处理"),
    IN_PROGRESS("IN_PROGRESS", "处理中"),
    FIXED("FIXED", "已修复"),
    VERIFIED("VERIFIED", "已验证"),
    CLOSED("CLOSED", "已关闭"),
    REOPENED("REOPENED", "已重开"),
    IGNORED("IGNORED", "已忽略");

    private final String code;
    private final String desc;

    DqIssueStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
