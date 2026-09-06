package com.xwms.core.move.enums;

import lombok.Getter;

/** 移库任务类型 */
@Getter
public enum MoveTaskType {
    MANUAL("MANUAL", "人工移库"),
    WCS("WCS", "设备自动移库");

    private final String code;
    private final String desc;

    MoveTaskType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
