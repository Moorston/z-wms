package com.xwms.core.inbound.enums;

import lombok.Getter;

/** 入库单状态 */
@Getter
public enum InboundStatus {
    CREATED("CREATED", "已创建"),
    RECEIVING("RECEIVING", "收货中"),
    RECEIVED("RECEIVED", "已收货"),
    QCING("QCING", "质检中"),
    PUTAWAYING("PUTAWAYING", "上架中"),
    DONE("DONE", "已完成"),
    CANCELLED("CANCELLED", "已取消");

    private final String code;
    private final String desc;

    InboundStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
