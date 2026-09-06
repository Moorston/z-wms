package com.xwms.core.query.enums;

import lombok.Getter;

/** 快照类型 */
@Getter
public enum SnapshotType {
    DAILY("DAILY", "日报"),
    WEEKLY("WEEKLY", "周报"),
    MONTHLY("MONTHLY", "月报");

    private final String code;
    private final String desc;

    SnapshotType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
