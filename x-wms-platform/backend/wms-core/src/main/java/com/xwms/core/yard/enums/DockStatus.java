package com.xwms.core.yard.enums;

import lombok.Getter;

/** 月台状态 */
@Getter
public enum DockStatus {
    IDLE("IDLE", "空闲"),
    OCCUPIED("OCCUPIED", "占用"),
    RESERVED("RESERVED", "已预约"),
    MAINTENANCE("MAINTENANCE", "维护中"),
    DISABLED("DISABLED", "禁用");

    private final String code;
    private final String desc;

    DockStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
