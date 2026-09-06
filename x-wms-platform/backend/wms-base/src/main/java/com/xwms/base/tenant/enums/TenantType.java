package com.xwms.base.tenant.enums;

import lombok.Getter;

/** 租户类型 */
@Getter
public enum TenantType {
    OWNER("OWNER", "货主"),
    WAREHOUSE("WAREHOUSE", "仓库"),
    PLATFORM("PLATFORM", "平台");

    private final String code;
    private final String desc;

    TenantType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
