package com.xwms.core.performance.enums;

import lombok.Getter;

/** 排名类型 */
@Getter
public enum RankingType {
    WAREHOUSE("WAREHOUSE", "仓库"),
    OWNER("OWNER", "货主"),
    SKU("SKU", "商品"),
    STAFF("STAFF", "人员"),
    TEAM("TEAM", "团队");

    private final String code;
    private final String desc;

    RankingType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
