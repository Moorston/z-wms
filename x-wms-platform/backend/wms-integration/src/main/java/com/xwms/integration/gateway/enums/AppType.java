package com.xwms.integration.gateway.enums;

import lombok.Getter;

/** 应用类型 */
@Getter
public enum AppType {
    ERP("ERP", "ERP系统"),
    TMS("TMS", "运输管理系统"),
    WCS("WCS", "设备控制系统"),
    THIRD_PARTY("THIRD_PARTY", "第三方系统"),
    INTERNAL("INTERNAL", "内部系统");

    private final String code;
    private final String desc;

    AppType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
