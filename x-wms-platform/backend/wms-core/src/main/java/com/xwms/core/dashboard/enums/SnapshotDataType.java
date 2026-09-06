package com.xwms.core.dashboard.enums;

import lombok.Getter;

/** 快照数据类型 */
@Getter
public enum SnapshotDataType {
    INVENTORY_TOTAL("INVENTORY_TOTAL", "库存总量"),
    INVENTORY_VALUE("INVENTORY_VALUE", "库存价值"),
    INVENTORY_TURNOVER("INVENTORY_TURNOVER", "库存周转率"),
    OUTBOUND_TODAY("OUTBOUND_TODAY", "今日出库"),
    INBOUND_TODAY("INBOUND_TODAY", "今日入库"),
    OPERATION_RATE("OPERATION_RATE", "作业效率"),
    OPERATION_QUEUE("OPERATION_QUEUE", "作业队列"),
    STAFF_ONLINE("STAFF_ONLINE", "在线人员"),
    STAFF_EFFICIENCY("STAFF_EFFICIENCY", "人员效率"),
    EQUIPMENT_STATUS("EQUIPMENT_STATUS", "设备状态"),
    EQUIPMENT_UTILIZATION("EQUIPMENT_UTILIZATION", "设备利用率"),
    WAVE_PROGRESS("WAVE_PROGRESS", "波次进度"),
    PICKING_PROGRESS("PICKING_PROGRESS", "拣货进度"),
    ALERT_COUNT("ALERT_COUNT", "预警数量"),
    TEMPERATURE("TEMPERATURE", "温度"),
    HUMIDITY("HUMIDITY", "湿度");

    private final String code;
    private final String desc;

    SnapshotDataType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
