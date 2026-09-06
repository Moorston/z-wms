package com.xwms.core.io.enums;

import lombok.Getter;

/** 任务状态 */
@Getter
public enum TaskStatus {
    PENDING("PENDING", "待处理"),
    PROCESSING("PROCESSING", "处理中"),
    COMPLETED("COMPLETED", "已完成"),
    FAILED("FAILED", "失败"),
    CANCELLED("CANCELLED", "已取消");

    private final String code;
    private final String desc;

    TaskStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
