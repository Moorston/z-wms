package com.xwms.core.pallet.enums;

import lombok.Getter;

/** 托盘状态枚举 EMPTY空托盘→IN_USE使用中→FULL满托盘→IN_TRANSIT在途→STORED已存储→SHIPPED已出库→DAMAGED损坏 */
@Getter
public enum PalletStatus {
    EMPTY("EMPTY", "空托盘"),
    IN_USE("IN_USE", "使用中"),
    FULL("FULL", "满托盘"),
    IN_TRANSIT("IN_TRANSIT", "在途"),
    STORED("STORED", "已存储"),
    SHIPPED("SHIPPED", "已出库"),
    DAMAGED("DAMAGED", "损坏");

    private final String code;
    private final String desc;

    PalletStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
