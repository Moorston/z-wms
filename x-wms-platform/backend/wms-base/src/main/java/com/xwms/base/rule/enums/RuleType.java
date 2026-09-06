package com.xwms.base.rule.enums;

import lombok.Getter;

/** 规则类型（12类业务规则） */
@Getter
public enum RuleType {
    PUTAWAY("PUTAWAY", "上架规则"),
    ALLOCATION("ALLOCATION", "分配规则"),
    ROTATION("ROTATION", "周转规则"),
    WAVE("WAVE", "波次规则"),
    REPLENISH("REPLENISH", "补货规则"),
    QC("QC", "质检规则"),
    CROSSDOCK("CROSSDOCK", "越库规则"),
    DELIVERY("DELIVERY", "配送规则"),
    PATH("PATH", "路径规则"),
    AUTOSHIP("AUTOSHIP", "自动发运规则"),
    PREALLOC("PREALLOC", "预配规则"),
    WAVESCHED("WAVESCHED", "波次调度规则");

    private final String code;
    private final String desc;

    RuleType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
