package com.xwms.base.container.enums;

import lombok.Getter;

/** 容器类别 */
@Getter
public enum ContainerCategory {
    PALLET("PALLET", "托盘"),
    BOX("BOX", "纸箱"),
    BAG("BAG", "包装袋"),
    TOTE("TOTE", "周转箱"),
    CAGE("CAGE", "笼车");

    private final String code;
    private final String desc;

    ContainerCategory(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
