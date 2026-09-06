package com.xwms.base.batch.enums;

import lombok.Getter;

/** 批次属性分类 */
@Getter
public enum BatchAttrCategory {
    PRODUCTION("PRODUCTION", "生产信息"),
    QUALITY("QUALITY", "质量信息"),
    LOGISTICS("LOGISTICS", "物流信息"),
    REGULATORY("REGULATORY", "合规信息");

    private final String code;
    private final String desc;

    BatchAttrCategory(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
