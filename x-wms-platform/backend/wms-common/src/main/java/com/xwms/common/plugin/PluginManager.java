package com.xwms.common.plugin;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/** 插件管理器 负责插件的注册、查找、执行链 */
public class PluginManager {
    private final Map<String, List<WmsPlugin>> plugins = new ConcurrentHashMap<>();

    /** 注册插件 */
    public void register(WmsPlugin plugin) {
        plugins.computeIfAbsent(
                        plugin.getClass().getInterfaces()[0].getSimpleName(),
                        k -> new java.util.ArrayList<>())
                .add(plugin);
    }

    /** 获取某类型的所有插件（按优先级排序） */
    @SuppressWarnings("unchecked")
    public <T extends WmsPlugin> List<T> getPlugins(Class<T> type) {
        return (List<T>)
                plugins.getOrDefault(type.getSimpleName(), List.of()).stream()
                        .filter(WmsPlugin::isEnabled)
                        .sorted((a, b) -> Integer.compare(b.getPriority(), a.getPriority()))
                        .collect(Collectors.toList());
    }

    /** 执行链：按优先级依次执行，命中即返回 */
    public <T extends WmsPlugin, R> R executeChain(
            Class<T> type, java.util.function.Function<T, R> handler) {
        for (T plugin : getPlugins(type)) {
            R result = handler.apply(plugin);
            if (result != null) return result;
        }
        return null;
    }
}
