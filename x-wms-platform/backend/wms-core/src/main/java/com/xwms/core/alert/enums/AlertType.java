package com.xwms.core.alert.enums;

import lombok.Getter;

/** 预警类型 */
@Getter
public enum AlertType {
    STOCK("STOCK", "库存预警"),
    EXPIRY("EXPIRY", "效期预警"),
    SAFETY("SAFETY", "安全库存预警"),
    ABNORMAL("ABNORMAL", "异常预警"),
    THRESHOLD("THRESHOLD", "阈值预警"),
    ACCURACY("ACCURACY", "准确率预警"),
    TURNOVER("TURNOVER", "周转率预警");

    private final String code;
    private final String desc;

    AlertType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
