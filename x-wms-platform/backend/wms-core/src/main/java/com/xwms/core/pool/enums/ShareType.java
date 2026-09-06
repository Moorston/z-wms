package com.xwms.core.pool.enums;

import lombok.Getter;

/** 共享类型 */
@Getter
public enum ShareType {
    OWNER("OWNER", "货主共享"),
    SKU("SKU", "SKU共享"),
    CATEGORY("CATEGORY", "品类共享"),
    ALL("ALL", "全共享");

    private final String code;
    private final String desc;

    ShareType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
