package com.xwms.core.archive.enums;

import lombok.Getter;

/** 归档类型 */
@Getter
public enum ArchiveType {
    DATE("DATE", "按日期归档"),
    STATUS("STATUS", "按状态归档"),
    ID("ID", "按ID归档");

    private final String code;
    private final String desc;

    ArchiveType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
