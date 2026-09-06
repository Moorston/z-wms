package com.xwms.core.wave.enums;

import lombok.Getter;

/** 波次类型 */
@Getter
public enum WaveType {
    NORMAL("NORMAL", "普通波次"),
    URGENT("URGENT", "紧急波次"),
    BULK("BULK", "大宗波次"),
    COLD("COLD", "冷链波次");

    private final String code;
    private final String desc;

    WaveType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
