package com.xwms.core.performance.enums;

import lombok.Getter;

/** 作业类型 */
@Getter
public enum WorkType {
    RECEIVING("RECEIVING", "收货"),
    PUTAWAY("PUTAWAY", "上架"),
    PICKING("PICKING", "拣货"),
    CHECKING("CHECKING", "复核"),
    PACKING("PACKING", "打包"),
    COUNTING("COUNTING", "盘点"),
    MOVING("MOVING", "移库"),
    VAS("VAS", "增值服务"),
    REPLENISH("REPLENISH", "补货");

    private final String code;
    private final String desc;

    WorkType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
