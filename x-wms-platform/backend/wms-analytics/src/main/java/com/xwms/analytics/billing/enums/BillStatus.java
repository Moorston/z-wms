package com.xwms.analytics.billing.enums;

import lombok.Getter;

/** 账单状态 */
@Getter
public enum BillStatus {
    DRAFT("DRAFT", "草稿"),
    PENDING("PENDING", "待确认"),
    CONFIRMED("CONFIRMED", "已确认"),
    INVOICED("INVOICED", "已开票"),
    PARTIAL_PAID("PARTIAL_PAID", "部分付款"),
    PAID("PAID", "已付款"),
    OVERDUE("OVERDUE", "逾期"),
    CANCELLED("CANCELLED", "取消");

    private final String code;
    private final String desc;

    BillStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static BillStatus of(String code) {
        for (BillStatus s : values()) {
            if (s.code.equals(code)) return s;
        }
        return DRAFT;
    }
}
