package com.xwms.core.yard.enums;

import lombok.Getter;

/** 预约状态 */
@Getter
public enum AppointmentStatus {
    PENDING("PENDING", "待确认"),
    CONFIRMED("CONFIRMED", "已确认"),
    CANCELLED("CANCELLED", "已取消"),
    ARRIVED("ARRIVED", "已到达"),
    CHECKED_IN("CHECKED_IN", "已签到"),
    LOADING("LOADING", "装卸中"),
    COMPLETED("COMPLETED", "已完成"),
    NO_SHOW("NO_SHOW", "未到"),
    OVERDUE("OVERDUE", "逾期");

    private final String code;
    private final String desc;

    AppointmentStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static AppointmentStatus of(String code) {
        for (AppointmentStatus s : values()) {
            if (s.code.equals(code)) return s;
        }
        return PENDING;
    }
}
