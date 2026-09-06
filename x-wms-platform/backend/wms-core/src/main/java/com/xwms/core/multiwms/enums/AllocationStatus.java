package com.xwms.core.multiwms.enums;

import lombok.Getter;

/** 订单分配状态 */
@Getter
public enum AllocationStatus {
    PENDING("PENDING", "待分配"),
    ALLOCATED("ALLOCATED", "已分配"),
    SHIPPED("SHIPPED", "已发货"),
    PARTIAL("PARTIAL", "部分分配"),
    FAILED("FAILED", "分配失败");

    private final String code;
    private final String desc;

    AllocationStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
