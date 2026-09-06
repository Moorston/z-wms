package com.xwms.base.container.enums;

import lombok.Getter;

/** 容器状态 */
@Getter
public enum ContainerStatus {
    EMPTY("EMPTY", "空容器"),
    OCCUPIED("OCCUPIED", "已占用"),
    IN_TRANSIT("IN_TRANSIT", "在途"),
    REPAIR("REPAIR", "维修中"),
    DISCARD("DISCARD", "已报废");

    private final String code;
    private final String desc;

    ContainerStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
