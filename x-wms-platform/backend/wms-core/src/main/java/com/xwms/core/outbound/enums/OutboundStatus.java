package com.xwms.core.outbound.enums;

import lombok.Getter;

/** 出库单状态 */
@Getter
public enum OutboundStatus {
    CREATED("CREATED", "已创建"),
    ALLOCATING("ALLOCATING", "分配中"),
    ALLOCATED("ALLOCATED", "已分配"),
    PICKING("PICKING", "拣货中"),
    PICKED("PICKED", "已拣货"),
    PACKING("PACKING", "打包中"),
    PACKED("PACKED", "已打包"),
    SHIPPING("SHIPPING", "发运中"),
    SHIPPED("SHIPPED", "已发运"),
    CANCELLED("CANCELLED", "已取消");

    private final String code;
    private final String desc;

    OutboundStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
