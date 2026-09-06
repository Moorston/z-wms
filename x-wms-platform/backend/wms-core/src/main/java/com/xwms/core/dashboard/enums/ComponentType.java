package com.xwms.core.dashboard.enums;

import lombok.Getter;

@Getter
public enum ComponentType {
    BAR_CHART("BAR_CHART", "柱状图"),
    LINE_CHART("LINE_CHART", "折线图"),
    PIE_CHART("PIE_CHART", "饼图"),
    GAUGE("GAUGE", "仪表盘"),
    TABLE("TABLE", "数据表格"),
    CARD("CARD", "指标卡片"),
    MAP("MAP", "地图"),
    HEATMAP("HEATMAP", "热力图"),
    PROGRESS("PROGRESS", "进度条"),
    TREND("TREND", "趋势图"),
    RANKING("RANKING", "排行榜"),
    CUSTOM("CUSTOM", "自定义组件");

    private final String code;
    private final String desc;

    ComponentType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
