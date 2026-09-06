package com.xwms.core.rf.enums;

import lombok.Getter;

/** RF任务状态 */
@Getter
public enum RfTaskStatus {
    PENDING("PENDING", "待领取"),
    ASSIGNED("ASSIGNED", "已分配"),
    IN_PROGRESS("IN_PROGRESS", "进行中"),
    PAUSED("PAUSED", "已暂停"),
    COMPLETED("COMPLETED", "已完成"),
    CANCELLED("CANCELLED", "已取消");

    private final String code;
    private final String desc;

    RfTaskStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
