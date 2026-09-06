package com.xwms.core.archive.enums;

import lombok.Getter;

/** 归档任务状态 */
@Getter
public enum ArchiveStatus {
    PENDING("PENDING", "待执行"),
    RUNNING("RUNNING", "执行中"),
    COMPLETED("COMPLETED", "已完成"),
    FAILED("FAILED", "失败"),
    CANCELLED("CANCELLED", "已取消");

    private final String code;
    private final String desc;

    ArchiveStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
