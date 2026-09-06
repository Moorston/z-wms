package com.xwms.core.transfer.enums;

import lombok.Getter;

/** 调拨作业类型 */
@Getter
public enum TransferTaskType {
    SHIP("SHIP", "调拨发运"),
    RECEIVE("RECEIVE", "调拨收货");

    private final String code;
    private final String desc;

    TransferTaskType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
