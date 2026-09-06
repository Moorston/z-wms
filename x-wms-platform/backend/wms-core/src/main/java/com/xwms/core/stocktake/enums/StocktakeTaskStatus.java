package com.xwms.core.stocktake.enums;

import lombok.Getter;

/** 盘点任务状态 */
@Getter
public enum StocktakeTaskStatus {
    DRAFT("DRAFT", "草稿"),
    PENDING("PENDING", "待执行"),
    COUNTING("COUNTING", "盘点中"),
    RECOUNTING("RECOUNTING", "复盘中"),
    ADJUSTING("ADJUSTING", "调整中"),
    COMPLETED("COMPLETED", "完成"),
    CANCELLED("CANCELLED", "取消");

    private final String code;
    private final String desc;

    StocktakeTaskStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static StocktakeTaskStatus of(String code) {
        for (StocktakeTaskStatus s : values()) {
            if (s.code.equals(code)) return s;
        }
        return DRAFT;
    }
}
