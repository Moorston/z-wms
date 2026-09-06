package com.xwms.core.security.enums;

import lombok.Getter;

/** 数据权限类型 */
@Getter
public enum DataPermissionType {
    WAREHOUSE("WAREHOUSE", "仓库"),
    OWNER("OWNER", "货主"),
    AREA("AREA", "库区"),
    LOCATION("LOCATION", "库位"),
    PRODUCT("PRODUCT", "商品"),
    DEPARTMENT("DEPARTMENT", "部门");

    private final String code;
    private final String desc;

    DataPermissionType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
