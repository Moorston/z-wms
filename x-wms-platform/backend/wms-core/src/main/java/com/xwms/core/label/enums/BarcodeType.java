package com.xwms.core.label.enums;

import lombok.Getter;

/** 条码类型 */
@Getter
public enum BarcodeType {
    SKU("SKU", "商品条码"),
    LOCATION("LOCATION", "库位条码"),
    BATCH("BATCH", "批次条码"),
    CONTAINER("CONTAINER", "容器条码"),
    ORDER("ORDER", "订单条码"),
    PALLET("PALLET", "托盘条码"),
    SERIAL("SERIAL", "序列号条码");

    private final String code;
    private final String desc;

    BarcodeType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
