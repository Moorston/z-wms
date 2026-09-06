package com.xwms.core.po.enums;

import lombok.Getter;

/** 采购订单状态枚举 CREATED创建→RELEASED已释放→PARTIAL_RECEIVED部分收货→FULLY_RECEIVED完全收货→COMPLETED订单完成 */
@Getter
public enum PoStatus {
    CREATED("CREATED", "已创建"),
    RELEASED("RELEASED", "已释放"),
    PARTIAL_RECEIVED("PARTIAL_RECEIVED", "部分收货"),
    FULLY_RECEIVED("FULLY_RECEIVED", "完全收货"),
    COMPLETED("COMPLETED", "订单完成"),
    CANCELLED("CANCELLED", "已取消");

    private final String code;
    private final String desc;

    PoStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static PoStatus getByCode(String code) {
        for (PoStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }
}
