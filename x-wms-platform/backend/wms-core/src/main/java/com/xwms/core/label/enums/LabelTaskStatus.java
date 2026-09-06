package com.xwms.core.label.enums;

import lombok.Getter;

/** 标签任务状态 */
@Getter
public enum LabelTaskStatus {
    PENDING("PENDING", "待打印"),
    PRINTING("PRINTING", "打印中"),
    COMPLETED("COMPLETED", "已完成"),
    FAILED("FAILED", "失败"),
    CANCELLED("CANCELLED", "已取消");

    private final String code;
    private final String desc;

    LabelTaskStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
