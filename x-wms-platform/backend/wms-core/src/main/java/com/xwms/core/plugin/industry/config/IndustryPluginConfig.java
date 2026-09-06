package com.xwms.core.plugin.industry.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

/**
 * 行业插件配置 通过Nacos动态配置，支持运行时启用/禁用行业插件 配置示例（Nacos）： wms: plugin: industry: enabled: true plugins: gsp:
 * enabled: true priority: 200 coldchain: enabled: true priority: 180 ecommerce: enabled: false #
 * 非大促期间禁用 priority: 150
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "wms.plugin.industry")
public class IndustryPluginConfig {

    /** 行业插件总开关 */
    private boolean enabled = true;

    /** 各行业插件配置 */
    private Map<String, PluginConfig> plugins = new HashMap<>();

    @Data
    public static class PluginConfig {
        /** 是否启用 */
        private boolean enabled = true;

        /** 优先级（越大越先执行） */
        private int priority = 100;

        /** 插件参数 */
        private Map<String, String> params = new HashMap<>();
    }

    /** 检查某行业插件是否启用 */
    public boolean isPluginEnabled(String pluginId) {
        if (!enabled) return false;
        PluginConfig config = plugins.get(pluginId);
        return config == null || config.isEnabled();
    }

    /** 获取插件优先级 */
    public int getPluginPriority(String pluginId, int defaultPriority) {
        PluginConfig config = plugins.get(pluginId);
        return config != null ? config.getPriority() : defaultPriority;
    }
}
