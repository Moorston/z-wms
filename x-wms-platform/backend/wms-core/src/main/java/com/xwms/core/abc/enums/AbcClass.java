package com.xwms.core.abc.enums;

import lombok.Getter;

/** ABC分类等级 */
@Getter
public enum AbcClass {
    A("A", "A类(重点管理)"),
    B("B", "B类(常规管理)"),
    C("C", "C类(简化管理)");

    private final String code;
    private final String desc;

    AbcClass(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
