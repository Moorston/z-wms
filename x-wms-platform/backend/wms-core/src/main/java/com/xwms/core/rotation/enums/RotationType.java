package com.xwms.core.rotation.enums;

import lombok.Getter;

/** 周转类型 */
@Getter
public enum RotationType {
    FIFO("FIFO", "先进先出"),
    FEFO("FEFO", "先效期先出"),
    LIFO("LIFO", "后进先出"),
    FIFO_FEFO("FIFO_FEFO", "FIFO+FEFO混合");

    private final String code;
    private final String desc;

    RotationType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
