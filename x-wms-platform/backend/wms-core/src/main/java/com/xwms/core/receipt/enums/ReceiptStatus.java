package com.xwms.core.receipt.enums;

import lombok.Getter;

/** 收货状态枚举 PENDING待收货→RECEIVING收货中→PARTIAL部分收货→COMPLETED收货完成→CANCELLED已取消 */
@Getter
public enum ReceiptStatus {
    PENDING("PENDING", "待收货"),
    RECEIVING("RECEIVING", "收货中"),
    PARTIAL("PARTIAL", "部分收货"),
    COMPLETED("COMPLETED", "收货完成"),
    CANCELLED("CANCELLED", "已取消");

    private final String code;
    private final String desc;

    ReceiptStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
