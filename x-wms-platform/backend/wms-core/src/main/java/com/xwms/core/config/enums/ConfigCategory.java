package com.xwms.core.config.enums;

import lombok.Getter;

/** 系统参数分类枚举 */
@Getter
public enum ConfigCategory {
    INBOUND("INBOUND", "入库参数"),
    OUTBOUND("OUTBOUND", "出库参数"),
    INVENTORY("INVENTORY", "库存参数"),
    QC("QC", "质检参数"),
    SYSTEM("SYSTEM", "系统参数"),
    INTERFACE("INTERFACE", "接口参数"),
    PRINT("PRINT", "打印参数"),
    WAVE("WAVE", "波次参数"),
    LOCATION("LOCATION", "库位参数"),
    BATCH("BATCH", "批次参数");

    private final String code;
    private final String desc;

    ConfigCategory(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
