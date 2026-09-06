package com.xwms.core.transaction.enums;

import lombok.Getter;

/** 库存流水类型 */
@Getter
public enum TransactionType {
    INBOUND("INBOUND", "入库"),
    OUTBOUND("OUTBOUND", "出库"),
    TRANSFER("TRANSFER", "调拨"),
    ADJUST("ADJUST", "调整"),
    MOVE("MOVE", "移库"),
    RESERVE("RESERVE", "预占"),
    RELEASE("RELEASE", "释放预占"),
    FROZEN("FROZEN", "冻结"),
    UNFROZEN("UNFROZEN", "解冻"),
    RETURN("RETURN", "退货"),
    CROSSDOCK("CROSSDOCK", "越库"),
    VAS("VAS", "增值服务");

    private final String code;
    private final String desc;

    TransactionType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
