package com.xwms.core.label.enums;

import lombok.Getter;

/** 打印任务状态 */
@Getter
public enum PrintStatus {
    PENDING("PENDING", "待打印"),
    PRINTING("PRINTING", "打印中"),
    PRINTED("PRINTED", "已打印"),
    FAILED("FAILED", "打印失败"),
    CANCELLED("CANCELLED", "已取消");

    private final String code;
    private final String desc;

    PrintStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
