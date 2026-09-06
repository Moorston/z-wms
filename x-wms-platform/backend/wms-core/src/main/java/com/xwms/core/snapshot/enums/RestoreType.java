package com.xwms.core.snapshot.enums;

import lombok.Getter;

/** 恢复类型 */
@Getter
public enum RestoreType {
    FULL("FULL", "全量恢复"),
    PARTIAL("PARTIAL", "部分恢复");

    private final String code;
    private final String desc;

    RestoreType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
