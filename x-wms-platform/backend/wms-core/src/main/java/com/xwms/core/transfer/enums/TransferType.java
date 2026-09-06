package com.xwms.core.transfer.enums;

import lombok.Getter;

/** 调拨类型 */
@Getter
public enum TransferType {
    NORMAL("NORMAL", "正常调拨"),
    URGENT("URGENT", "紧急调拨"),
    RETURN("RETURN", "退货调拨");

    private final String code;
    private final String desc;

    TransferType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
