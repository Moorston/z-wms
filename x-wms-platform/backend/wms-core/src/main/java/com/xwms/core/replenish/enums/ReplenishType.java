package com.xwms.core.replenish.enums;

import lombok.Getter;

/** 补货类型 */
@Getter
public enum ReplenishType {
    NORMAL("NORMAL", "普通补货"),
    URGENT("URGENT", "紧急补货"),
    CROSSDOCK("CROSSDOCK", "越库补货"),
    PRESALE("PRESALE", "大促预补货"),
    PERIODIC("PERIODIC", "周期补货");

    private final String code;
    private final String desc;

    ReplenishType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
