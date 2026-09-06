package com.xwms.core.serial.enums;

import lombok.Getter;

/** 序列号状态枚举 */
@Getter
public enum SerialStatus {
    IN_STOCK("IN_STOCK", "在库"),
    OUT_STOCK("OUT_STOCK", "已出库"),
    RETURNED("RETURNED", "退货"),
    SCRAPPED("SCRAPPED", "报废"),
    FROZEN("FROZEN", "冻结");

    private final String code;
    private final String desc;

    SerialStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
