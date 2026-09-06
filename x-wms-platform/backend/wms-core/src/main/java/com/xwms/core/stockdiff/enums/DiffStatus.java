package com.xwms.core.stockdiff.enums;

import lombok.Getter;

/** 差异状态 */
@Getter
public enum DiffStatus {
    PENDING("PENDING", "待处理"),
    PROCESSING("PROCESSING", "处理中"),
    APPROVING("APPROVING", "审批中"),
    RESOLVED("RESOLVED", "已解决"),
    CANCELLED("CANCELLED", "已取消");

    private final String code;
    private final String desc;

    DiffStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
