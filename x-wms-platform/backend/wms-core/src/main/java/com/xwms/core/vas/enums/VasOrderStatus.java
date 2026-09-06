package com.xwms.core.vas.enums;

import lombok.Getter;

/** VAS工单状态 */
@Getter
public enum VasOrderStatus {
    PENDING("PENDING", "待处理"),
    ASSIGNED("ASSIGNED", "已分配"),
    PROCESSING("PROCESSING", "处理中"),
    PAUSED("PAUSED", "已暂停"),
    COMPLETED("COMPLETED", "完成"),
    CANCELLED("CANCELLED", "取消"),
    EXCEPTION("EXCEPTION", "异常");

    private final String code;
    private final String desc;

    VasOrderStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static VasOrderStatus of(String code) {
        for (VasOrderStatus s : values()) {
            if (s.code.equals(code)) return s;
        }
        return PENDING;
    }
}
