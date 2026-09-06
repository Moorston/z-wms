package com.xwms.core.returnorder.enums;

import lombok.Getter;

/** 退货单状态枚举 */
@Getter
public enum ReturnStatus {
    CREATED("CREATED", "已创建"),
    RECEIVING("RECEIVING", "收货中"),
    RECEIVED("RECEIVED", "已收货"),
    QC("QC", "质检中"),
    QC_DONE("QC_DONE", "质检完成"),
    PUTAWAYING("PUTAWAYING", "上架中"),
    COMPLETED("COMPLETED", "已完成"),
    CANCELLED("CANCELLED", "已取消");

    private final String code;
    private final String desc;

    ReturnStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
