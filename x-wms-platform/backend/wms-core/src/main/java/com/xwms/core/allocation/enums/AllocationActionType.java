package com.xwms.core.allocation.enums;

import lombok.Getter;

/** 分配操作类型 */
@Getter
public enum AllocationActionType {
    ALLOCATE("ALLOCATE", "分配"),
    REALLOCATE("REALLOCATE", "重新分配"),
    RELEASE("RELEASE", "释放"),
    PICK("PICK", "拣货"),
    CANCEL("CANCEL", "取消");

    private final String code;
    private final String desc;

    AllocationActionType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
