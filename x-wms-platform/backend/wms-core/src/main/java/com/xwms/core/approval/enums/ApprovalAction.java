package com.xwms.core.approval.enums;

import lombok.Getter;

/** 审批动作 */
@Getter
public enum ApprovalAction {
    APPROVE("APPROVE", "通过"),
    REJECT("REJECT", "驳回"),
    CC("CC", "抄送"),
    TRANSFER("TRANSFER", "转办"),
    WITHDRAW("WITHDRAW", "撤回");

    private final String code;
    private final String desc;

    ApprovalAction(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
