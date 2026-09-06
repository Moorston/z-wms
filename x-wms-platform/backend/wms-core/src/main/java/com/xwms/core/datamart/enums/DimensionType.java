package com.xwms.core.datamart.enums;

import lombok.Getter;

@Getter
public enum DimensionType {
    TIME("TIME", "时间维度"),
    WAREHOUSE("WAREHOUSE", "仓库维度"),
    OWNER("OWNER", "货主维度"),
    SKU("SKU", "商品维度"),
    CATEGORY("CATEGORY", "品类维度"),
    LOCATION("LOCATION", "库位维度"),
    ORDER("ORDER", "订单维度"),
    CUSTOMER("CUSTOMER", "客户维度"),
    SUPPLIER("SUPPLIER", "供应商维度"),
    STAFF("STAFF", "人员维度"),
    DEVICE("DEVICE", "设备维度"),
    CUSTOM("CUSTOM", "自定义维度");

    private final String code;
    private final String desc;

    DimensionType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
