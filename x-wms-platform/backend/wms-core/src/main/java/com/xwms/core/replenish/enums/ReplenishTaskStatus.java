package com.xwms.core.replenish.enums;

import lombok.Getter;

/** 补货任务状态 */
@Getter
public enum ReplenishTaskStatus {
    PENDING("PENDING", "待处理"),
    ASSIGNED("ASSIGNED", "已分配"),
    PICKING("PICKING", "拣货中"),
    PICKED("PICKED", "已拣货"),
    PUTAWAYING("PUTAWAYING", "上架中"),
    COMPLETED("COMPLETED", "完成"),
    EXCEPTION("EXCEPTION", "异常"),
    CANCELLED("CANCELLED", "取消");

    private final String code;
    private final String desc;

    ReplenishTaskStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static ReplenishTaskStatus of(String code) {
        for (ReplenishTaskStatus s : values()) {
            if (s.code.equals(code)) return s;
        }
        return PENDING;
    }
}
