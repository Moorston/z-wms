package com.xwms.integration.api.registry;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.annotation.PostConstruct;

import org.springframework.stereotype.Service;

import com.xwms.integration.core.model.ApiDefinition;

import lombok.extern.slf4j.Slf4j;

/** API注册中心 管理所有外部系统API定义，支持运行时动态注册/注销 启动时从数据库/Nacos加载，运行时可热更新 */
@Slf4j
@Service
public class ApiRegistry {

    /** path+method -> ApiDefinition */
    private final Map<String, ApiDefinition> registry = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        // TODO: 从数据库/Nacos加载所有API定义
        log.info("API注册中心初始化完成");
    }

    /** 注册API */
    public void register(ApiDefinition definition) {
        String key = buildKey(definition.getPath(), definition.getMethod());
        registry.put(key, definition);
        log.info(
                "API注册: {} {} -> adapter={}",
                definition.getMethod(),
                definition.getPath(),
                definition.getAdapterId());
    }

    /** 注销API */
    public void unregister(String path, String method) {
        registry.remove(buildKey(path, method));
        log.info("API注销: {} {}", method, path);
    }

    /** 获取API定义 */
    public ApiDefinition get(String path, String method) {
        return registry.get(buildKey(path, method));
    }

    /** 检查API是否存在且启用 */
    public boolean isAvailable(String path, String method) {
        ApiDefinition def = get(path, method);
        return def != null && "ENABLED".equals(def.getStatus());
    }

    /** 获取所有已注册API */
    public Map<String, ApiDefinition> getAll() {
        return Map.copyOf(registry);
    }

    /** 按apiId获取API定义（遍历查找，注册量小可接受） */
    public ApiDefinition getById(String apiId) {
        if (apiId == null) return null;
        return registry.values().stream()
                .filter(def -> apiId.equals(def.getApiId()))
                .findFirst()
                .orElse(null);
    }

    /** 按apiId注销API（重载，转调双参版） */
    public void unregister(String apiId) {
        if (apiId == null) return;
        registry.entrySet().removeIf(e -> apiId.equals(e.getValue().getApiId()));
        log.info("API注销: apiId={}", apiId);
    }

    private String buildKey(String path, String method) {
        return method.toUpperCase() + ":" + path;
    }
}
