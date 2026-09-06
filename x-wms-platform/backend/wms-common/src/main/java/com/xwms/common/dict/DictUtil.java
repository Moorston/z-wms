package com.xwms.common.dict;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.xwms.common.core.Result;
import com.xwms.common.feign.client.DictFeignClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 数据字典翻译工具
 *
 * <p>封装DictFeignClient调用，添加本地缓存（ConcurrentHashMap+TTL）， 减少远程调用，提高翻译性能。
 *
 * <p>使用方式：
 *
 * <pre>
 * String statusLabel = dictUtil.translate("inbound_status", "CREATED");
 * List<Map<String, Object>> items = dictUtil.listItems("inbound_type");
 * </pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DictUtil {

    private final DictFeignClient dictFeignClient;

    /** 本地缓存：dictCode → (itemValue → itemLabel) */
    private final Map<String, CacheEntry> localCache = new ConcurrentHashMap<>();

    /** 缓存过期时间：5分钟 */
    private static final long CACHE_TTL_MS = 5 * 60 * 1000L;

    /** 字典翻译：itemValue → itemLabel */
    public String translate(String dictCode, String itemValue) {
        if (itemValue == null || itemValue.isEmpty()) {
            return itemValue;
        }
        Map<String, String> map = getTranslateMap(dictCode);
        return map.getOrDefault(itemValue, itemValue);
    }

    /** 获取字典翻译Map（带本地缓存） */
    public Map<String, String> getTranslateMap(String dictCode) {
        CacheEntry entry = localCache.get(dictCode);
        if (entry != null && !entry.isExpired()) {
            return entry.data;
        }
        try {
            Result<Map<String, String>> result = dictFeignClient.translateMap(dictCode);
            Map<String, String> data =
                    result != null && result.getData() != null
                            ? result.getData()
                            : Collections.emptyMap();
            localCache.put(dictCode, new CacheEntry(data, System.currentTimeMillis()));
            return data;
        } catch (Exception e) {
            log.warn("字典翻译失败: dictCode={}, error={}", dictCode, e.getMessage());
            return entry != null ? entry.data : Collections.emptyMap();
        }
    }

    /** 获取字典项列表 */
    public List<Map<String, Object>> listItems(String dictCode) {
        try {
            Result<List<Map<String, Object>>> result = dictFeignClient.listItems(dictCode);
            return result != null && result.getData() != null
                    ? result.getData()
                    : Collections.emptyList();
        } catch (Exception e) {
            log.warn("获取字典项失败: dictCode={}, error={}", dictCode, e.getMessage());
            return Collections.emptyList();
        }
    }

    /** 清除本地缓存 */
    public void clearCache() {
        localCache.clear();
        log.info("清除字典本地缓存");
    }

    /** 清除指定字典的本地缓存 */
    public void clearCache(String dictCode) {
        localCache.remove(dictCode);
    }

    /** 缓存条目 */
    private static class CacheEntry {
        final Map<String, String> data;
        final long createTime;

        CacheEntry(Map<String, String> data, long createTime) {
            this.data = data;
            this.createTime = createTime;
        }

        boolean isExpired() {
            return System.currentTimeMillis() - createTime > CACHE_TTL_MS;
        }
    }
}
