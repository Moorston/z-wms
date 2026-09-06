package com.xwms.core.lock.enums;

import lombok.Getter;

/** 锁状态 */
@Getter
public enum LockStatus {
    HELD("HELD", "持有中"),
    WAITING("WAITING", "等待中"),
    RELEASED("RELEASED", "已释放"),
    TIMEOUT("TIMEOUT", "已超时"),
    DEADLOCK("DEADLOCK", "死锁");

    private final String code;
    private final String desc;

    LockStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
