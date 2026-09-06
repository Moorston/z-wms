package com.xwms.integration.external.enums;

import lombok.Getter;

/** 外部系统类型 */
@Getter
public enum ExternalSystemType {
    WCS("WCS", "设备控制系统"),
    TMS("TMS", "运输管理系统"),
    ERP("ERP", "企业资源计划"),
    OMS("OMS", "订单管理系统"),
    CRM("CRM", "客户关系管理"),
    BI("BI", "商业智能"),
    OTHER("OTHER", "其他系统");

    private final String code;
    private final String desc;

    ExternalSystemType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
