package com.xwms.core.approval.enums;

import lombok.Getter;

/** 审批实例状态 */
@Getter
public enum ApprovalInstanceStatus {
    PENDING("PENDING", "待审批"),
    APPROVING("APPROVING", "审批中"),
    APPROVED("APPROVED", "已通过"),
    REJECTED("REJECTED", "已驳回"),
    CANCELLED("CANCELLED", "已取消"),
    TIMEOUT("TIMEOUT", "已超时");

    private final String code;
    private final String desc;

    ApprovalInstanceStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
