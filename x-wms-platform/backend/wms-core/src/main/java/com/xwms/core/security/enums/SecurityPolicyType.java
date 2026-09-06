package com.xwms.core.security.enums;

import lombok.Getter;

/** 安全策略类型 */
@Getter
public enum SecurityPolicyType {
    PASSWORD("PASSWORD", "密码策略"),
    LOGIN("LOGIN", "登录策略"),
    SESSION("SESSION", "会话策略"),
    ACCESS("ACCESS", "访问策略"),
    DATA("DATA", "数据策略"),
    API("API", "接口策略");

    private final String code;
    private final String desc;

    SecurityPolicyType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
