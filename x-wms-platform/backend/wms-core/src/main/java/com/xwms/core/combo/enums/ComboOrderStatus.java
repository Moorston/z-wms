package com.xwms.core.combo.enums;

import lombok.Getter;

/** 组合单状态 */
@Getter
public enum ComboOrderStatus {
    PENDING("PENDING", "待处理"),
    PROCESSING("PROCESSING", "处理中"),
    COMPLETED("COMPLETED", "已完成"),
    CANCELLED("CANCELLED", "已取消");

    private final String code;
    private final String desc;

    ComboOrderStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
