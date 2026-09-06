package com.xwms.core.expiry.enums;

import lombok.Getter;

/** 效期状态 */
@Getter
public enum ExpiryStatus {
    NORMAL("NORMAL", "正常"),
    NEAR_EXPIRY("NEAR_EXPIRY", "临期"),
    EXPIRED("EXPIRED", "已过期");

    private final String code;
    private final String desc;

    ExpiryStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
