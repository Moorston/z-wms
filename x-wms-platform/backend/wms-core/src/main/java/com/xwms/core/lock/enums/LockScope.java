package com.xwms.core.lock.enums;

import lombok.Getter;

/** 锁范围 */
@Getter
public enum LockScope {
    SKU("SKU", "SKU级"),
    LOCATION("LOCATION", "库位级"),
    BATCH("BATCH", "批次级"),
    WAREHOUSE("WAREHOUSE", "仓库级"),
    OWNER("OWNER", "货主级");

    private final String code;
    private final String desc;

    LockScope(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
