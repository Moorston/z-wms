package com.xwms.core.outbound.enums;

import lombok.Getter;

/** 出库类型 */
@Getter
public enum OutboundType {
    SALE("SALE", "销售出库"),
    TRANSFER("TRANSFER", "调拨出库"),
    RETURN("RETURN", "退货出库"),
    VAS("VAS", "增值服务出库"),
    SAMPLE("SAMPLE", "样品出库");

    private final String code;
    private final String desc;

    OutboundType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
