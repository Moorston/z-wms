package com.xwms.core.dashboard.enums;

import lombok.Getter;

@Getter
public enum DashboardType {
    OVERVIEW("OVERVIEW", "总览大屏"),
    INVENTORY("INVENTORY", "库存大屏"),
    OPERATION("OPERATION", "作业大屏"),
    WAREHOUSE("WAREHOUSE", "仓库大屏"),
    KPI("KPI", "KPI大屏"),
    REAL_TIME("REAL_TIME", "实时监控大屏"),
    CUSTOM("CUSTOM", "自定义大屏");

    private final String code;
    private final String desc;

    DashboardType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
