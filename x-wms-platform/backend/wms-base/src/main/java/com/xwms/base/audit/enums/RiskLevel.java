package com.xwms.base.audit.enums;

import lombok.Getter;

/** 风险等级 */
@Getter
public enum RiskLevel {
    LOW("LOW", "低风险"),
    MEDIUM("MEDIUM", "中风险"),
    HIGH("HIGH", "高风险"),
    CRITICAL("CRITICAL", "严重风险");

    private final String code;
    private final String desc;

    RiskLevel(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
