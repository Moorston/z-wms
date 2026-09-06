package com.xwms.base.product.enums;

import lombok.Getter;

/** 条码类型 */
@Getter
public enum BarcodeType {
    EAN13("EAN13", "EAN-13"),
    UPC("UPC", "UPC"),
    CODE128("CODE128", "Code 128"),
    QR("QR", "二维码"),
    INTERNAL("INTERNAL", "内部码");

    private final String code;
    private final String desc;

    BarcodeType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
