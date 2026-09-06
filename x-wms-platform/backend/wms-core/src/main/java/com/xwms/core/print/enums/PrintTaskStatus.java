package com.xwms.core.print.enums;

import lombok.Getter;

/** 打印任务状态 */
@Getter
public enum PrintTaskStatus {
    PENDING("PENDING", "待打印"),
    PRINTING("PRINTING", "打印中"),
    SUCCESS("SUCCESS", "打印成功"),
    FAILED("FAILED", "打印失败"),
    CANCELLED("CANCELLED", "已取消");

    private final String code;
    private final String desc;

    PrintTaskStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
