package com.xwms.core.notification.enums;

import lombok.Getter;

/** 通知渠道 */
@Getter
public enum NotifyChannel {
    IN_APP("IN_APP", "站内信"),
    SMS("SMS", "短信"),
    EMAIL("EMAIL", "邮件"),
    DINGTALK("DINGTALK", "钉钉"),
    WECHAT("WECHAT", "企业微信"),
    FEISHU("FEISHU", "飞书");

    private final String code;
    private final String desc;

    NotifyChannel(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
