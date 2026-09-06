package com.xwms.core.print.enums;

import lombok.Getter;

/** 打印模板类型 */
@Getter
public enum PrintTemplateType {
    PICKING("PICKING", "拣货单"),
    OUTBOUND("OUTBOUND", "出库单"),
    INBOUND("INBOUND", "入库单"),
    LABEL("LABEL", "标签"),
    RECEIPT("RECEIPT", "收货单"),
    PACKING("PACKING", "装箱单"),
    DELIVERY("DELIVERY", "配送单"),
    QC("QC", "质检单"),
    STOCKTAKE("STOCKTAKE", "盘点单");

    private final String code;
    private final String desc;

    PrintTemplateType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
