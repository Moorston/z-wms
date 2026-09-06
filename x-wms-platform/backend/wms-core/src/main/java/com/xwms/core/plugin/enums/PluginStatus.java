package com.xwms.core.plugin.enums;

import lombok.Getter;

/** 插件状态 */
@Getter
public enum PluginStatus {
    INSTALLED("INSTALLED", "已安装"),
    ENABLED("ENABLED", "已启用"),
    DISABLED("DISABLED", "已禁用"),
    ERROR("ERROR", "异常");

    private final String code;
    private final String desc;

    PluginStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
