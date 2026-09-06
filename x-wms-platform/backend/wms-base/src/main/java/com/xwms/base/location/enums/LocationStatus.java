package com.xwms.base.location.enums;

import lombok.Getter;

/** 库位状态 */
@Getter
public enum LocationStatus {
    EMPTY("EMPTY", "空库位"),
    NORMAL("NORMAL", "正常"),
    FULL("FULL", "已满"),
    FROZEN("FROZEN", "冻结"),
    DISABLED("DISABLED", "停用");

    private final String code;
    private final String desc;

    LocationStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
