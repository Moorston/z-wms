package com.xwms.core.query.enums;

import lombok.Getter;

/** 库存查询维度 */
@Getter
public enum InventoryQueryDimension {
    SKU("SKU", "按SKU"),
    LOCATION("LOCATION", "按库位"),
    BATCH("BATCH", "按批次"),
    OWNER("OWNER", "按货主"),
    WAREHOUSE("WAREHOUSE", "按仓库"),
    CATEGORY("CATEGORY", "按品类"),
    COMBINED("COMBINED", "组合维度");

    private final String code;
    private final String desc;

    InventoryQueryDimension(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
