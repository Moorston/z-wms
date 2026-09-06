package com.xwms.core.returnorder.enums;

import lombok.Getter;

/** 退货单状态 */
@Getter
public enum ReturnOrderStatus {
    CREATED("CREATED", "已创建"),
    RECEIVING("RECEIVING", "收货中"),
    RECEIVED("RECEIVED", "已收货"),
    QC("QC", "质检中"),
    QC_PASSED("QC_PASSED", "质检合格"),
    QC_FAILED("QC_FAILED", "质检不合格"),
    PUTAWAYING("PUTAWAYING", "上架中"),
    COMPLETED("COMPLETED", "完成"),
    REJECTED("REJECTED", "拒收"),
    CANCELLED("CANCELLED", "取消");

    private final String code;
    private final String desc;

    ReturnOrderStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static ReturnOrderStatus of(String code) {
        for (ReturnOrderStatus s : values()) {
            if (s.code.equals(code)) return s;
        }
        return CREATED;
    }
}
