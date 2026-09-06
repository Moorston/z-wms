package com.xwms.core.dataquality.enums;

import lombok.Getter;

@Getter
public enum DqCheckType {
    SCHEDULED("SCHEDULED", "定时检查"),
    MANUAL("MANUAL", "手动检查"),
    EVENT_DRIVEN("EVENT_DRIVEN", "事件驱动"),
    REAL_TIME("REAL_TIME", "实时检查"),
    BATCH("BATCH", "批量检查");

    private final String code;
    private final String desc;

    DqCheckType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
