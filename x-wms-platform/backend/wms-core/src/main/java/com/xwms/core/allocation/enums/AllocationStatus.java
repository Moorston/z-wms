package com.xwms.core.allocation.enums;

import lombok.Getter;

/** 分配状态 */
@Getter
public enum AllocationStatus {
    ALLOCATED("ALLOCATED", "已分配"),
    PICKING("PICKING", "拣货中"),
    PICKED("PICKED", "已拣货"),
    RELEASED("RELEASED", "已释放"),
    CANCELLED("CANCELLED", "已取消");

    private final String code;
    private final String desc;

    AllocationStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
