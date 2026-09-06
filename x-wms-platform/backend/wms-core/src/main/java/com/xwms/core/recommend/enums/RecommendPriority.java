package com.xwms.core.recommend.enums;

import lombok.Getter;

/** 推荐优先级 */
@Getter
public enum RecommendPriority {
    URGENT("URGENT", "紧急"),
    HIGH("HIGH", "高"),
    NORMAL("NORMAL", "正常"),
    LOW("LOW", "低");

    private final String code;
    private final String desc;

    RecommendPriority(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
