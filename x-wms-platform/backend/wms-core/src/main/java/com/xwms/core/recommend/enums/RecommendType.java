package com.xwms.core.recommend.enums;

import lombok.Getter;

/** 推荐类型 */
@Getter
public enum RecommendType {
    REPLENISH("REPLENISH", "补货推荐"),
    LOCATION("LOCATION", "库位推荐"),
    WAVE("WAVE", "波次推荐"),
    PATH("PATH", "路径推荐");

    private final String code;
    private final String desc;

    RecommendType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
