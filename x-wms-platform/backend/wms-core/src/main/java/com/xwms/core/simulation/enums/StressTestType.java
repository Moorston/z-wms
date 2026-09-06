package com.xwms.core.simulation.enums;

import lombok.Getter;

@Getter
public enum StressTestType {
    INVENTORY_QUERY("INVENTORY_QUERY", "库存查询压力测试"),
    ORDER_ALLOCATION("ORDER_ALLOCATION", "订单分配压力测试"),
    WAVE_PICKING("WAVE_PICKING", "波次拣货压力测试"),
    BATCH_IMPORT("BATCH_IMPORT", "批量导入压力测试"),
    BATCH_EXPORT("BATCH_EXPORT", "批量导出压力测试"),
    CONCURRENT_DEDUCT("CONCURRENT_DEDUCT", "并发扣减压力测试"),
    API_THROUGHPUT("API_THROUGHPUT", "API吞吐量压力测试");

    private final String code;
    private final String desc;

    StressTestType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
