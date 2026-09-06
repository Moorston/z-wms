package com.xwms.core.simulation.enums;

import lombok.Getter;

@Getter
public enum SimulationType {
    INVENTORY_FLOW("INVENTORY_FLOW", "库存流转仿真"),
    WAVE_PICKING("WAVE_PICKING", "波次拣货仿真"),
    REPLENISHMENT("REPLENISHMENT", "补货仿真"),
    ALLOCATION("ALLOCATION", "分配仿真"),
    LAYOUT("LAYOUT", "库位布局仿真"),
    CAPACITY("CAPACITY", "容量仿真"),
    THROUGHPUT("THROUGHPUT", "吞吐量仿真");

    private final String code;
    private final String desc;

    SimulationType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
