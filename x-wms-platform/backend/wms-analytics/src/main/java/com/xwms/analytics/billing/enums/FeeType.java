package com.xwms.analytics.billing.enums;

import lombok.Getter;

/** 费用类型 */
@Getter
public enum FeeType {
    STORAGE("STORAGE", "仓储费"),
    INBOUND("INBOUND", "入库费"),
    OUTBOUND("OUTBOUND", "出库费"),
    HANDLING("HANDLING", "操作费"),
    VAS("VAS", "增值服务费"),
    OTHER("OTHER", "其他费用");

    private final String code;
    private final String desc;

    FeeType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
