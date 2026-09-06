package com.xwms.core.datamart.enums;

import lombok.Getter;

@Getter
public enum MartType {
    INVENTORY("INVENTORY", "库存数据集市"),
    OPERATION("OPERATION", "作业数据集市"),
    ORDER("ORDER", "订单数据集市"),
    COST("COST", "成本数据集市"),
    KPI("KPI", "KPI数据集市"),
    ANALYSIS("ANALYSIS", "分析数据集市"),
    CUSTOM("CUSTOM", "自定义数据集市");

    private final String code;
    private final String desc;

    MartType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
