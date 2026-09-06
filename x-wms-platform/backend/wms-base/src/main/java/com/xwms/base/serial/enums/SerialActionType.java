package com.xwms.base.serial.enums;

import lombok.Getter;

/** 序列号动作类型 */
@Getter
public enum SerialActionType {
    GENERATE("GENERATE", "生成"),
    INBOUND("INBOUND", "入库"),
    PUTAWAY("PUTAWAY", "上架"),
    PICK("PICK", "拣货"),
    PACK("PACK", "打包"),
    SHIP("SHIP", "发运"),
    RETURN("RETURN", "退货"),
    SCRAP("SCRAP", "报废"),
    MOVE("MOVE", "移库");

    private final String code;
    private final String desc;

    SerialActionType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
