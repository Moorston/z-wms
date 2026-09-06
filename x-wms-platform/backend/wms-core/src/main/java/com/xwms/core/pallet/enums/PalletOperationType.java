package com.xwms.core.pallet.enums;

import lombok.Getter;

/** 托盘操作类型枚举 */
@Getter
public enum PalletOperationType {
    PALLETIZE("PALLETIZE", "码盘"),
    DEPALLETIZE("DEPALLETIZE", "拆盘"),
    MERGE("MERGE", "合并"),
    SPLIT("SPLIT", "拆分"),
    MOVE("MOVE", "移动"),
    SEAL("SEAL", "封存"),
    UNSEAL("UNSEAL", "解封"),
    DAMAGE("DAMAGE", "损坏"),
    REPAIR("REPAIR", "修复"),
    SCRAP("SCRAP", "报废");

    private final String code;
    private final String desc;

    PalletOperationType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
