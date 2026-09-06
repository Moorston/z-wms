package com.xwms.core.shipment.enums;

import lombok.Getter;

/** 快递服务类型 */
@Getter
public enum ServiceType {
    STANDARD("STANDARD", "标准快递"),
    EXPRESS("EXPRESS", "快速快递"),
    NEXT_DAY("NEXT_DAY", "次日达"),
    SAME_DAY("SAME_DAY", "当日达"),
    ECONOMY("ECONOMY", "经济快递");

    private final String code;
    private final String desc;

    ServiceType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
