package com.xwms.core.equipment.enums;

import lombok.Getter;

/** 设备状态 */
@Getter
public enum EquipmentStatus {
    IDLE("IDLE", "空闲"),
    IN_USE("IN_USE", "使用中"),
    MAINTENANCE("MAINTENANCE", "维护中"),
    FAULT("FAULT", "故障"),
    RETIRED("RETIRED", "报废");

    private final String code;
    private final String desc;

    EquipmentStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
