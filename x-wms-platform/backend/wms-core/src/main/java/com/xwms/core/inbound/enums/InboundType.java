package com.xwms.core.inbound.enums;

import lombok.Getter;

/** 入库类型 */
@Getter
public enum InboundType {
    PURCHASE("PURCHASE", "采购入库"),
    RETURN("RETURN", "退货入库"),
    TRANSFER("TRANSFER", "调拨入库"),
    PRODUCTION("PRODUCTION", "生产入库"),
    BLIND("BLIND", "盲收入库");

    private final String code;
    private final String desc;

    InboundType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
