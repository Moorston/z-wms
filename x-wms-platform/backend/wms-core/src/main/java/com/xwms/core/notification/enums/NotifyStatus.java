package com.xwms.core.notification.enums;

import lombok.Getter;

/** 通知状态 */
@Getter
public enum NotifyStatus {
    PENDING("PENDING", "待发送"),
    SENDING("SENDING", "发送中"),
    SENT("SENT", "已发送"),
    FAILED("FAILED", "发送失败"),
    READ("READ", "已读");

    private final String code;
    private final String desc;

    NotifyStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
