package com.xwms.core.forecast.enums;

import lombok.Getter;

/** 库存分析类型 */
@Getter
public enum InventoryAnalysisType {
    TURNOVER("TURNOVER", "周转率分析"),
    ABC("ABC", "ABC分类分析"),
    SAFETY_STOCK("SAFETY_STOCK", "安全库存分析"),
    AGING("AGING", "库龄分析"),
    VALUE("VALUE", "价值分析"),
    SPACE("SPACE", "空间分析");

    private final String code;
    private final String desc;

    InventoryAnalysisType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
