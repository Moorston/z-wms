package com.xwms.core.qc.enums;

import lombok.Getter;

/** 质检类型 */
@Getter
public enum QcType {
    FULL("FULL", "全检"),
    SAMPLE("SAMPLE", "抽检"),
    NONE("NONE", "免检");

    private final String code;
    private final String desc;

    QcType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static QcType of(String code) {
        for (QcType type : values()) {
            if (type.code.equals(code)) return type;
        }
        return NONE;
    }
}
