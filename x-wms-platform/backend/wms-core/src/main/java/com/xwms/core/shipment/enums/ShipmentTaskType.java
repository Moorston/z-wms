package com.xwms.core.shipment.enums;

import lombok.Getter;

/** 发运作业类型 */
@Getter
public enum ShipmentTaskType {
    GET_TRACKING("GET_TRACKING", "获取运单号"),
    PRINT("PRINT", "打印快递单"),
    PICKUP("PICKUP", "快递揽收"),
    DELIVER("DELIVER", "配送");

    private final String code;
    private final String desc;

    ShipmentTaskType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
