package com.xwms.common.plugin;

/** 插件基类接口 所有行业插件、规则插件、集成适配器实现此接口 */
public interface WmsPlugin {
    /** 插件ID */
    String getPluginId();

    /** 插件名称 */
    String getPluginName();

    /** 优先级（越大越先执行） */
    default int getPriority() {
        return 100;
    }

    /** 是否启用 */
    default boolean isEnabled() {
        return true;
    }

    /** 插件描述 */
    default String getDescription() {
        return "";
    }
}
