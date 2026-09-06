package com.xwms.core.security.enums;

import lombok.Getter;

/** 操作类型 */
@Getter
public enum OperationType {
    QUERY("QUERY", "查询"),
    ADD("ADD", "新增"),
    EDIT("EDIT", "修改"),
    DELETE("DELETE", "删除"),
    APPROVE("APPROVE", "审批"),
    EXPORT("EXPORT", "导出"),
    IMPORT("IMPORT", "导入"),
    LOGIN("LOGIN", "登录"),
    LOGOUT("LOGOUT", "登出"),
    PRINT("PRINT", "打印"),
    AUDIT("AUDIT", "审核");

    private final String code;
    private final String desc;

    OperationType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
