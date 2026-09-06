package com.xwms.core.alert.enums;

import lombok.Getter;

/** 预警级别 */
@Getter
public enum AlertLevel {
    INFO("INFO", "信息"),
    WARNING("WARNING", "警告"),
    CRITICAL("CRITICAL", "严重");

    private final String code;
    private final String desc;

    AlertLevel(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
