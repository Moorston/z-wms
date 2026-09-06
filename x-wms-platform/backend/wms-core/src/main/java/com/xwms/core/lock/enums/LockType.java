package com.xwms.core.lock.enums;

import lombok.Getter;

/** 锁类型 */
@Getter
public enum LockType {
    SHARED("SHARED", "共享锁(读锁)"),
    EXCLUSIVE("EXCLUSIVE", "排他锁(写锁)");

    private final String code;
    private final String desc;

    LockType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
