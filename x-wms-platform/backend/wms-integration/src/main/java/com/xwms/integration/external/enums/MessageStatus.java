package com.xwms.integration.external.enums;

import lombok.Getter;

/** 集成消息状态 */
@Getter
public enum MessageStatus {
    PENDING("PENDING", "待发送"),
    SENDING("SENDING", "发送中"),
    SENT("SENT", "已发送"),
    FAILED("FAILED", "失败"),
    CONSUMED("CONSUMED", "已消费");

    private final String code;
    private final String desc;

    MessageStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
