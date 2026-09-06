package com.xwms.core.simulation.enums;

import lombok.Getter;

@Getter
public enum ScenarioType {
    PEAK_SEASON("PEAK_SEASON", "旺季大促场景"),
    NORMAL("NORMAL", "日常作业场景"),
    LOW_SEASON("LOW_SEASON", "淡季场景"),
    EMERGENCY("EMERGENCY", "紧急订单场景"),
    BULK_ORDER("BULK_ORDER", "大宗订单场景"),
    RETURN_PEAK("RETURN_PEAK", "退货高峰场景"),
    INVENTORY_SHORTAGE("INVENTORY_SHORTAGE", "库存短缺场景");

    private final String code;
    private final String desc;

    ScenarioType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
