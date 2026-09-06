package com.xwms.core.reserve.enums;

import lombok.Getter;

/** 预占类型 */
@Getter
public enum ReserveType {
    OUTBOUND("OUTBOUND", "出库预占"),
    TRANSFER("TRANSFER", "调拨预占"),
    VAS("VAS", "增值服务预占"),
    REPLENISH("REPLENISH", "补货预占");

    private final String code;
    private final String desc;

    ReserveType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
