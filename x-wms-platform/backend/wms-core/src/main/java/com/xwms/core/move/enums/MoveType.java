package com.xwms.core.move.enums;

import lombok.Getter;

/** 移库类型 */
@Getter
public enum MoveType {
    NORMAL("NORMAL", "普通移库"),
    BATCH("BATCH", "批量移库"),
    AUTO("AUTO", "自动移库"),
    REPLENISH("REPLENISH", "补货移库"),
    ADJUST("ADJUST", "调整移库");

    private final String code;
    private final String desc;

    MoveType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
