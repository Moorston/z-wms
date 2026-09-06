package com.xwms.core.io.enums;

import lombok.Getter;

/** 导入类型 */
@Getter
public enum ImportType {
    INVENTORY("INVENTORY", "库存"),
    PRODUCT("PRODUCT", "产品"),
    LOCATION("LOCATION", "库位"),
    OWNER("OWNER", "货主"),
    ORDER("ORDER", "订单"),
    BATCH("BATCH", "批次"),
    ADJUST("ADJUST", "库存调整"),
    SERIAL("SERIAL", "序列号");

    private final String code;
    private final String desc;

    ImportType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
