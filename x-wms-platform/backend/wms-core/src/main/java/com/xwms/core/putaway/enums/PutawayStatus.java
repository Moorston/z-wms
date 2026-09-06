package com.xwms.core.putaway.enums;

import lombok.Getter;

/** 上架状态枚举 PENDING待上架→PUTAWAYING上架中→PARTIAL部分上架→COMPLETED上架完成→CANCELLED已取消 */
@Getter
public enum PutawayStatus {
    PENDING("PENDING", "待上架"),
    ASSIGNED("ASSIGNED", "已派发"),
    CLAIMED("CLAIMED", "已领取"),
    PUTAWAYING("PUTAWAYING", "上架中"),
    PARTIAL("PARTIAL", "部分上架"),
    COMPLETED("COMPLETED", "上架完成"),
    EXCEPTION("EXCEPTION", "异常"),
    RESOLVED("RESOLVED", "已解决"),
    CANCELLED("CANCELLED", "已取消");

    private final String code;
    private final String desc;

    PutawayStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
