package com.xwms.core.putaway.enums;

import lombok.Getter;

/** 上架方式枚举 */
@Getter
public enum PutawayType {
    STANDARD("STANDARD", "标准上架"),
    QUICK("QUICK", "快捷上架"),
    MERGE("MERGE", "合并上架（多SKU同托）"),
    BATCH("BATCH", "批量上架（多托同库位）"),
    LPN("LPN", "按箱码/LPN上架"),
    DIRECT("DIRECT", "直接收货到存储库位（免上架）"),
    RESERVATION("RESERVATION", "码盘预约库位");

    private final String code;
    private final String desc;

    PutawayType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
