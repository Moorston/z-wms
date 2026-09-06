package com.xwms.base.partner.enums;

import lombok.Getter;

/** 合作伙伴类型 */
@Getter
public enum PartnerType {
    OWNER("OWNER", "货主"),
    CUSTOMER("CUSTOMER", "客户"),
    SUPPLIER("SUPPLIER", "供应商");

    private final String code;
    private final String desc;

    PartnerType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
