package com.xwms.core.pool.enums;

import lombok.Getter;

/** 池化分配状态 */
@Getter
public enum PoolAllocationStatus {
    ALLOCATED("ALLOCATED", "已分配"),
    PICKING("PICKING", "拣货中"),
    PICKED("PICKED", "已拣货"),
    RELEASED("RELEASED", "已释放"),
    CANCELLED("CANCELLED", "已取消");

    private final String code;
    private final String desc;

    PoolAllocationStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
