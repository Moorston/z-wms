package com.xwms.core.allocation.enums;

import lombok.Getter;

/** 分配策略类型 */
@Getter
public enum AllocationStrategyType {
    FIFO("FIFO", "先进先出"),
    FEFO("FEFO", "先到期先出"),
    LIFO("LIFO", "后进先出"),
    LEFO("LEFO", "后到期先出"),
    MANUAL("MANUAL", "手动指定");

    private final String code;
    private final String desc;

    AllocationStrategyType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
