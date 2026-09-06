package com.xwms.core.rf.enums;

import lombok.Getter;

/** RF任务类型 */
@Getter
public enum RfTaskType {
    RECEIVING("RECEIVING", "收货"),
    PUTAWAY("PUTAWAY", "上架"),
    PICKING("PICKING", "拣货"),
    CHECKING("CHECKING", "复核"),
    PACKING("PACKING", "打包"),
    MOVING("MOVING", "移库"),
    COUNTING("COUNTING", "盘点"),
    REPLENISH("REPLENISH", "补货"),
    VAS("VAS", "增值服务"),
    RETURN("RETURN", "退货"),
    SORTING("SORTING", "分拣");

    private final String code;
    private final String desc;

    RfTaskType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
