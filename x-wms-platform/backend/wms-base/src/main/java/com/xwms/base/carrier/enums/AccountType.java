package com.xwms.base.carrier.enums;

import lombok.Getter;

/** 账户类型 */
@Getter
public enum AccountType {
    MONTHLY("MONTHLY", "月结"),
    PREPAID("PREPAID", "预付"),
    CASH("CASH", "现付");

    private final String code;
    private final String desc;

    AccountType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
