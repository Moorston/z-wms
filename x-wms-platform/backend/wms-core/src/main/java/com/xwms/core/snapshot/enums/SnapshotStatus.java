package com.xwms.core.snapshot.enums;

import lombok.Getter;

/** 快照状态 */
@Getter
public enum SnapshotStatus {
    PROCESSING("PROCESSING", "处理中"),
    COMPLETED("COMPLETED", "已完成"),
    FAILED("FAILED", "失败");

    private final String code;
    private final String desc;

    SnapshotStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
