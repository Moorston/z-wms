package com.xwms.core.reserve.enums;

import lombok.Getter;

/** 预占动作类型 */
@Getter
public enum ReserveActionType {
    RESERVE("RESERVE", "预占"),
    RELEASE("RELEASE", "释放"),
    CONFIRM("CONFIRM", "确认"),
    EXPIRE("EXPIRE", "过期");

    private final String code;
    private final String desc;

    ReserveActionType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
