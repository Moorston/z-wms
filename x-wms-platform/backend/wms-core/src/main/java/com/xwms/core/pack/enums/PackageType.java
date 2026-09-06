package com.xwms.core.pack.enums;

import lombok.Getter;

/** 包裹类型 */
@Getter
public enum PackageType {
    BOX("BOX", "纸箱"),
    BAG("BAG", "包装袋"),
    PALLET("PALLET", "托盘"),
    TUBE("TUBE", "管状包装"),
    CUSTOM("CUSTOM", "自定义包装");

    private final String code;
    private final String desc;

    PackageType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
