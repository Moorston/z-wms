package com.xwms.core.plugin.enums;

import lombok.Getter;

/** 插件类型 */
@Getter
public enum PluginType {
    INDUSTRY("INDUSTRY", "行业插件"),
    BUSINESS("BUSINESS", "业务插件"),
    TECH("TECH", "技术插件");

    private final String code;
    private final String desc;

    PluginType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
