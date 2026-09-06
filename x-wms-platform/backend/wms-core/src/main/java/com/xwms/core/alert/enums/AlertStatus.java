package com.xwms.core.alert.enums;

import lombok.Getter;

/** 预警状态 */
@Getter
public enum AlertStatus {
    PENDING("PENDING", "待处理"),
    PROCESSING("PROCESSING", "处理中"),
    RESOLVED("RESOLVED", "已解决"),
    IGNORED("IGNORED", "已忽略");

    private final String code;
    private final String desc;

    AlertStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
