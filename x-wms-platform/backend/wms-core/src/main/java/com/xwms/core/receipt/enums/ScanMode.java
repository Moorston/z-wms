package com.xwms.core.receipt.enums;

import lombok.Getter;

/** 扫描模式枚举 */
@Getter
public enum ScanMode {
    BATCH("BATCH", "批量扫描"),
    PIECE("PIECE", "逐件扫描"),
    BOX("BOX", "逐箱扫描"),
    SERIAL("SERIAL", "序列号扫描");

    private final String code;
    private final String desc;

    ScanMode(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
