package com.xwms.core.recommend.enums;

import lombok.Getter;

/** 推荐状态 */
@Getter
public enum RecommendStatus {
    PENDING("PENDING", "待处理"),
    ACCEPTED("ACCEPTED", "已接受"),
    REJECTED("REJECTED", "已拒绝"),
    EXECUTED("EXECUTED", "已执行");

    private final String code;
    private final String desc;

    RecommendStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
