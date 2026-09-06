package com.xwms.base.batch.enums;

import lombok.Getter;

/** 批次属性类型 */
@Getter
public enum BatchAttrType {
    STRING("STRING", "字符串"),
    NUMBER("NUMBER", "数字"),
    DATE("DATE", "日期"),
    ENUM("ENUM", "枚举"),
    BOOLEAN("BOOLEAN", "布尔");

    private final String code;
    private final String desc;

    BatchAttrType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
