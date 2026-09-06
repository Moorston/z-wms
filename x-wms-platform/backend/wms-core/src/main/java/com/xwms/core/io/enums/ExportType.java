package com.xwms.core.io.enums;

import lombok.Getter;

/** 导出类型 */
@Getter
public enum ExportType {
    INVENTORY("INVENTORY", "库存"),
    PRODUCT("PRODUCT", "产品"),
    LOCATION("LOCATION", "库位"),
    OWNER("OWNER", "货主"),
    ORDER("ORDER", "订单"),
    BATCH("BATCH", "批次"),
    REPORT("REPORT", "报表"),
    TRANSACTION("TRANSACTION", "库存流水"),
    KPI("KPI", "KPI报表");

    private final String code;
    private final String desc;

    ExportType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
