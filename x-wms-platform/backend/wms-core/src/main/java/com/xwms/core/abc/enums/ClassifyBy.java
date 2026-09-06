package com.xwms.core.abc.enums;

import lombok.Getter;

/** ABC分类依据 */
@Getter
public enum ClassifyBy {
    SALES_AMOUNT("SALES_AMOUNT", "销售额"),
    SALES_QTY("SALES_QTY", "销售数量"),
    PROFIT("PROFIT", "利润"),
    TURNOVER("TURNOVER", "周转率");

    private final String code;
    private final String desc;

    ClassifyBy(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
