package com.xwms.core.combo.enums;

import lombok.Getter;

/** 组合类型 */
@Getter
public enum ComboType {
    KIT("KIT", "套装"),
    BUNDLE("BUNDLE", "捆绑"),
    GIFT("GIFT", "赠品"),
    PROMO("PROMO", "促销");

    private final String code;
    private final String desc;

    ComboType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
