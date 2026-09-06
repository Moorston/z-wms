package com.xwms.base.tenant.enums;

import lombok.Getter;

/** 租户状态 */
@Getter
public enum TenantStatus {
    ACTIVE("ACTIVE", "正常"),
    INACTIVE("INACTIVE", "未激活"),
    FROZEN("FROZEN", "已冻结"),
    EXPIRED("EXPIRED", "已过期");

    private final String code;
    private final String desc;

    TenantStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
