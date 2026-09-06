package com.xwms.core.returnorder.enums;

import lombok.Getter;

/** 退货类型枚举 */
@Getter
public enum ReturnType {
    CUSTOMER("CUSTOMER", "客户退货"),
    SUPPLIER("SUPPLIER", "供应商退货"),
    TRANSFER("TRANSFER", "调拨退货"),
    INTERNAL("INTERNAL", "内部退货");

    private final String code;
    private final String desc;

    ReturnType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
