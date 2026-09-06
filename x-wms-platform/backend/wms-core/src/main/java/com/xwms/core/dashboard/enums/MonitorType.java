package com.xwms.core.dashboard.enums;

import lombok.Getter;

@Getter
public enum MonitorType {
    INVENTORY_QTY("INVENTORY_QTY", "库存数量监控"),
    INVENTORY_VALUE("INVENTORY_VALUE", "库存金额监控"),
    ORDER_VOLUME("ORDER_VOLUME", "订单量监控"),
    THROUGHPUT("THROUGHPUT", "吞吐量监控"),
    PICKING_EFFICIENCY("PICKING_EFFICIENCY", "拣货效率监控"),
    DEVICE_STATUS("DEVICE_STATUS", "设备状态监控"),
    STAFF_STATUS("STAFF_STATUS", "人员状态监控"),
    WAREHOUSE_UTILIZATION("WAREHOUSE_UTILIZATION", "仓库利用率监控");

    private final String code;
    private final String desc;

    MonitorType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
