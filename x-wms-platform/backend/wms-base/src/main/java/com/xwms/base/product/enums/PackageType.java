package com.xwms.base.product.enums;

import lombok.Getter;

/** 包装类型 */
@Getter
public enum PackageType {
    BOX("BOX", "箱"),
    PALLET("PALLET", "托盘"),
    CARTON("CARTON", "纸箱"),
    BAG("BAG", "袋"),
    PIECE("PIECE", "件");

    private final String code;
    private final String desc;

    PackageType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
