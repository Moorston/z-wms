package com.xwms.core.multiwms.enums;

import lombok.Getter;

/** 调拨单状态 */
@Getter
public enum TransferStatus {
    DRAFT("DRAFT", "草稿"),
    CONFIRMED("CONFIRMED", "已确认"),
    IN_TRANSIT("IN_TRANSIT", "运输中"),
    RECEIVED("RECEIVED", "已收货"),
    COMPLETED("COMPLETED", "已完成"),
    CANCELLED("CANCELLED", "已取消");

    private final String code;
    private final String desc;

    TransferStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
