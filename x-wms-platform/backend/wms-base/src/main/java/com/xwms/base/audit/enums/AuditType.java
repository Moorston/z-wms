package com.xwms.base.audit.enums;

import lombok.Getter;

/** 安全审计类型 */
@Getter
public enum AuditType {
    LOGIN_ABNORMAL("LOGIN_ABNORMAL", "异常登录"),
    PERMISSION_VIOLATION("PERMISSION_VIOLATION", "越权访问"),
    DATA_EXPORT("DATA_EXPORT", "数据导出"),
    CONFIG_CHANGE("CONFIG_CHANGE", "配置变更"),
    SENSITIVE_ACCESS("SENSITIVE_ACCESS", "敏感数据访问"),
    PASSWORD_CHANGE("PASSWORD_CHANGE", "密码修改"),
    ROLE_CHANGE("ROLE_CHANGE", "角色变更");

    private final String code;
    private final String desc;

    AuditType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
