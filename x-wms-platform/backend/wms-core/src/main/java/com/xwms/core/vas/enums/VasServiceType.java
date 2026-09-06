package com.xwms.core.vas.enums;

import lombok.Getter;

/** VAS服务类型 */
@Getter
public enum VasServiceType {
    LABELING("LABELING", "贴标"),
    REPACK("REPACK", "重新包装"),
    KITTING("KITTING", "组合套装"),
    SPLIT("SPLIT", "拆零"),
    ASSEMBLY("ASSEMBLY", "组装"),
    INSPECTION("INSPECTION", "质检加工"),
    CUSTOM("CUSTOM", "定制服务"),
    OTHER("OTHER", "其他");

    private final String code;
    private final String desc;

    VasServiceType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
