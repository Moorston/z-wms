package com.xwms.core.pool.enums;

import lombok.Getter;

/** 分配池类型 */
@Getter
public enum PoolType {
    SHARED("SHARED", "共享池"),
    DEDICATED("DEDICATED", "专用池"),
    VIRTUAL("VIRTUAL", "虚拟池"),
    BUFFER("BUFFER", "缓冲池");

    private final String code;
    private final String desc;

    PoolType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
