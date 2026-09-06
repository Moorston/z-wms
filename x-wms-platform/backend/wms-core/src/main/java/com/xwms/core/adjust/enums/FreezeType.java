package com.xwms.core.adjust.enums;

import lombok.Getter;

/** 库存冻结类型 */
@Getter
public enum FreezeType {
    QC("QC", "质检冻结"),
    RETURN("RETURN", "退货冻结"),
    DAMAGE("DAMAGE", "损坏冻结"),
    MANUAL("MANUAL", "手工冻结");

    private final String code;
    private final String desc;

    FreezeType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
