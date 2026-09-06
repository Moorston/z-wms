package com.xwms.core.receipt.enums;

import lombok.Getter;

/** 收货方式枚举 */
@Getter
public enum ReceiptType {
    ASN("ASN", "按ASN整单收货"),
    PARTIAL("PARTIAL", "部分收货/多次收货"),
    PALLET("PALLET", "码盘收货"),
    SCAN("SCAN", "扫描收货"),
    BOX("BOX", "按箱收货"),
    QUICK("QUICK", "快捷收货"),
    VISUAL("VISUAL", "可视化收货"),
    MIX("MIX", "混ASN/混PO扫描收货"),
    COMPONENT("COMPONENT", "组件扫描收货"),
    SORT("SORT", "整理收货"),
    PRE("PRE", "预收货"),
    BLIND("BLIND", "盲收");

    private final String code;
    private final String desc;

    ReceiptType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
