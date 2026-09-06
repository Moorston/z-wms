package com.xwms.core.qc.enums;

import lombok.Getter;

/** 质检单状态 */
@Getter
public enum QcOrderStatus {
    PENDING("PENDING", "待检"),
    INSPECTING("INSPECTING", "检验中"),
    PASSED("PASSED", "质检合格"),
    FAILED("FAILED", "质检不合格"),
    CONCESSION_PENDING("CONCESSION_PENDING", "让步接收待审批"),
    CONCESSION_APPROVED("CONCESSION_APPROVED", "让步接收审批通过"),
    CONCESSION_REJECTED("CONCESSION_REJECTED", "让步接收审批拒绝"),
    DISPOSED("DISPOSED", "已处理");

    private final String code;
    private final String desc;

    QcOrderStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static QcOrderStatus of(String code) {
        for (QcOrderStatus status : values()) {
            if (status.code.equals(code)) return status;
        }
        return PENDING;
    }
}
