package com.xwms.core.freeze.enums;

import lombok.Getter;

/** 冻结状态 */
@Getter
public enum FreezeStatus {
    FROZEN("FROZEN", "已冻结"),
    PARTIAL("PARTIAL", "部分解冻"),
    UNFROZEN("UNFROZEN", "已解冻"),
    CANCELLED("CANCELLED", "已取消");

    private final String code;
    private final String desc;

    FreezeStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
