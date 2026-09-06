package com.xwms.base.serial.enums;

import lombok.Getter;

/** 序列号状态 */
@Getter
public enum SerialStatus {
    CREATED("CREATED", "已生成"),
    INBOUND("INBOUND", "已入库"),
    IN_STOCK("IN_STOCK", "在库"),
    ALLOCATED("ALLOCATED", "已分配"),
    PICKED("PICKED", "已拣货"),
    PACKED("PACKED", "已打包"),
    SHIPPED("SHIPPED", "已发运"),
    RETURNED("RETURNED", "已退货"),
    SCRAPPED("SCRAPPED", "已报废");

    private final String code;
    private final String desc;

    SerialStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
