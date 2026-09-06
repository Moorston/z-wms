package com.xwms.core.abc.enums;

import lombok.Getter;

/** XYZ分类等级（需求波动性） */
@Getter
public enum XyzClass {
    X("X", "X类(需求稳定)"),
    Y("Y", "Y类(需求波动中等)"),
    Z("Z", "Z类(需求波动大)");

    private final String code;
    private final String desc;

    XyzClass(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
