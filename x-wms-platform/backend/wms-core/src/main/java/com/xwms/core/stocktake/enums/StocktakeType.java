package com.xwms.core.stocktake.enums;

import lombok.Getter;

/** 盘点类型 */
@Getter
public enum StocktakeType {
    FULL("FULL", "全盘"),
    AREA("AREA", "库区盘点"),
    SKU("SKU", "指定SKU盘点"),
    CYCLE("CYCLE", "循环盘点"),
    RANDOM("RANDOM", "抽盘");

    private final String code;
    private final String desc;

    StocktakeType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
