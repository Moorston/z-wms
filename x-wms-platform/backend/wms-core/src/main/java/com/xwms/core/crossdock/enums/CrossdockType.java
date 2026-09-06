package com.xwms.core.crossdock.enums;

import lombok.Getter;

/** 越库类型 */
@Getter
public enum CrossdockType {
    FLOW_THROUGH("FLOW_THROUGH", "直通越库（整进整出）"),
    COMBINE("COMBINE", "合并越库（多单合一）"),
    BREAK_BULK("BREAK_BULK", "拆零越库（整进零出）");

    private final String code;
    private final String desc;

    CrossdockType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
