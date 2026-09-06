package com.xwms.core.expiry.enums;

import lombok.Getter;

/** 过期处理类型 */
@Getter
public enum ExpiryHandleType {
    FREEZE("FREEZE", "冻结"),
    RETURN("RETURN", "退货"),
    DESTROY("DESTROY", "销毁"),
    SELL("SELL", "折价销售"),
    ADJUST("ADJUST", "库存调整");

    private final String code;
    private final String desc;

    ExpiryHandleType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
