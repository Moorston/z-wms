package com.xwms.base.carrier.enums;

import lombok.Getter;

/** 承运商类型 */
@Getter
public enum CarrierType {
    EXPRESS("EXPRESS", "快递公司"),
    LOGISTICS("LOGISTICS", "物流公司"),
    SELF("SELF", "自有配送");

    private final String code;
    private final String desc;

    CarrierType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
