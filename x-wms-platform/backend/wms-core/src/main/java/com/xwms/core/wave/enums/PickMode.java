package com.xwms.core.wave.enums;

import lombok.Getter;

/** 拣货模式 */
@Getter
public enum PickMode {
    PICK_BY_ORDER("PICK_BY_ORDER", "摘果式（按单拣货）"),
    PICK_BY_SKU("PICK_BY_SKU", "播种式（按SKU拣货）"),
    PICK_BY_WAVE("PICK_BY_WAVE", "波次拣货（混合）");

    private final String code;
    private final String desc;

    PickMode(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
