package com.xwms.integration.gateway.enums;

import lombok.Getter;

/** API分类 */
@Getter
public enum ApiCategory {
    INBOUND("INBOUND", "入库API"),
    OUTBOUND("OUTBOUND", "出库API"),
    INVENTORY("INVENTORY", "库存API"),
    BASE("BASE", "基础数据API"),
    REPORT("REPORT", "报表API"),
    SYSTEM("SYSTEM", "系统API");

    private final String code;
    private final String desc;

    ApiCategory(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
