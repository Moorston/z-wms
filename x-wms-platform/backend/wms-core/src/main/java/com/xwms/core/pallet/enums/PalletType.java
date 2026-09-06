package com.xwms.core.pallet.enums;

import lombok.Getter;

/** 托盘类型枚举 */
@Getter
public enum PalletType {
    STANDARD("STANDARD", "标准托盘"),
    CHEP("CHEP", "欧标托盘"),
    SMALL("SMALL", "小托盘"),
    BIG("BIG", "大托盘"),
    CARTON("CARTON", "纸箱"),
    BAG("BAG", "袋子");

    private final String code;
    private final String desc;

    PalletType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
