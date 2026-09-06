package com.xwms.base.location.enums;

import lombok.Getter;

/** 库位类型 */
@Getter
public enum LocationType {
    STORAGE("STORAGE", "存储位"),
    PICK("PICK", "拣货位"),
    RECEIVE("RECEIVE", "收货位"),
    SHIP("SHIP", "发货位"),
    QC("QC", "质检位"),
    RETURN("RETURN", "退货位"),
    VIRTUAL("VIRTUAL", "虚拟位");

    private final String code;
    private final String desc;

    LocationType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
