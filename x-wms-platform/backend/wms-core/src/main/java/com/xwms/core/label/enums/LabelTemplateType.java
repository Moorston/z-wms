package com.xwms.core.label.enums;

import lombok.Getter;

/** 标签模板类型 */
@Getter
public enum LabelTemplateType {
    SKU("SKU", "商品标签"),
    LOCATION("LOCATION", "库位标签"),
    BATCH("BATCH", "批次标签"),
    CONTAINER("CONTAINER", "容器标签"),
    ORDER("ORDER", "订单标签"),
    PALLET("PALLET", "托盘标签"),
    SERIAL("SERIAL", "序列号标签");

    private final String code;
    private final String desc;

    LabelTemplateType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
