package com.xwms.core.freeze.enums;

import lombok.Getter;

/** 冻结类型 */
@Getter
public enum FreezeType {
    QC("QC", "质检冻结"),
    STOCKTAKE("STOCKTAKE", "盘点冻结"),
    EXCEPTION("EXCEPTION", "异常冻结"),
    RECALL("RECALL", "召回冻结"),
    EXPIRE("EXPIRE", "过期冻结"),
    CUSTOMER("CUSTOMER", "客户冻结"),
    OTHER("OTHER", "其他冻结");

    private final String code;
    private final String desc;

    FreezeType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
