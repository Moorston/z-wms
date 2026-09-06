package com.xwms.core.snapshot.enums;

import lombok.Getter;

/** 快照类型 */
@Getter
public enum SnapshotType {
    DAILY("DAILY", "日结快照"),
    MONTHLY("MONTHLY", "月结快照"),
    MANUAL("MANUAL", "手动快照"),
    REALTIME("REALTIME", "实时快照");

    private final String code;
    private final String desc;

    SnapshotType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
