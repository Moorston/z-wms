package com.xwms.core.safety.enums;

import lombok.Getter;

/** 库存状态 */
@Getter
public enum StockStatus {
    NORMAL("NORMAL", "正常"),
    BELOW_SAFETY("BELOW_SAFETY", "低于安全库存"),
    BELOW_REORDER("BELOW_REORDER", "低于补货点"),
    OUT_OF_STOCK("OUT_OF_STOCK", "缺货"),
    OVER_STOCK("OVER_STOCK", "超储");

    private final String code;
    private final String desc;

    StockStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
