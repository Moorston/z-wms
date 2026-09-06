package com.xwms.core.putawayrule.cache;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import com.xwms.core.putawayrule.entity.PutawayRule;
import com.xwms.core.putawayrule.entity.PutawayRuleLine;
import com.xwms.core.putawayrule.mapper.PutawayRuleLineMapper;
import com.xwms.core.putawayrule.mapper.PutawayRuleMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 上架规则多级缓存服务 一级缓存：本地Caffeine（热点数据，超低延迟） 二级缓存：Redis（分布式共享，容量大） 三级存储：Oracle（持久化，最终一致）
 *
 * <p>缓存更新策略（Cache-Aside）： - 读操作：本地→Redis→Oracle，回写Redis和本地 - 写操作：更新Oracle → 删除Redis缓存 →
 * 删除本地缓存（延迟双删）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PutawayRuleCacheService {

    private final PutawayRuleMapper ruleMapper;
    private final PutawayRuleLineMapper ruleLineMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    private Cache<Long, PutawayRule> localRuleCache;
    private Cache<String, List<Long>> localRuleIdCache;
    private ScheduledExecutorService cacheEvictExecutor;

    private static final String REDIS_RULE_PREFIX = "wms:putaway:rule:";
    private static final String REDIS_RULE_IDS_PREFIX = "wms:putaway:rule:ids:";
    private static final long REDIS_EXPIRE_HOURS = 2;
    private static final long LOCAL_MAX_SIZE = 1000;
    private static final long LOCAL_EXPIRE_MINUTES = 10;

    @PostConstruct
    public void init() {
        localRuleCache =
                Caffeine.newBuilder()
                        .maximumSize(LOCAL_MAX_SIZE)
                        .expireAfterWrite(LOCAL_EXPIRE_MINUTES, TimeUnit.MINUTES)
                        .recordStats()
                        .build();

        localRuleIdCache =
                Caffeine.newBuilder()
                        .maximumSize(LOCAL_MAX_SIZE)
                        .expireAfterWrite(LOCAL_EXPIRE_MINUTES, TimeUnit.MINUTES)
                        .recordStats()
                        .build();

        log.info("上架规则多级缓存初始化完成: 本地最大容量={}, 过期时间={}分钟", LOCAL_MAX_SIZE, LOCAL_EXPIRE_MINUTES);

        cacheEvictExecutor =
                Executors.newSingleThreadScheduledExecutor(
                        r -> {
                            Thread t = new Thread(r, "putaway-cache-evict");
                            t.setDaemon(true);
                            return t;
                        });
    }

    @PreDestroy
    public void destroy() {
        if (cacheEvictExecutor != null && !cacheEvictExecutor.isShutdown()) {
            cacheEvictExecutor.shutdown();
            try {
                if (!cacheEvictExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    cacheEvictExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                cacheEvictExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    public PutawayRule getRuleWithLines(Long ruleId) {
        if (ruleId == null) return null;

        PutawayRule rule = localRuleCache.getIfPresent(ruleId);
        if (rule != null) {
            return rule;
        }

        String redisKey = REDIS_RULE_PREFIX + ruleId;
        try {
            Object cached = redisTemplate.opsForValue().get(redisKey);
            if (cached instanceof PutawayRule) {
                rule = (PutawayRule) cached;
                localRuleCache.put(ruleId, rule);
                return rule;
            }
        } catch (Exception e) {
            log.warn("Redis缓存读取失败: ruleId={}", ruleId);
        }

        rule = ruleMapper.selectById(ruleId);
        if (rule == null) return null;

        List<PutawayRuleLine> ruleLines = ruleLineMapper.selectByRuleId(ruleId);
        rule.setRuleLines(ruleLines);

        try {
            redisTemplate.opsForValue().set(redisKey, rule, REDIS_EXPIRE_HOURS, TimeUnit.HOURS);
        } catch (Exception e) {
            log.warn("Redis缓存写入失败: ruleId={}", ruleId);
        }
        localRuleCache.put(ruleId, rule);

        return rule;
    }

    public List<Long> getMatchedRuleIds(String warehouseCode, String ownerCode, String skuCode) {
        String cacheKey = buildCacheKey(warehouseCode, ownerCode, skuCode);

        List<Long> ruleIds = localRuleIdCache.getIfPresent(cacheKey);
        if (ruleIds != null) {
            return ruleIds;
        }

        String redisKey = REDIS_RULE_IDS_PREFIX + cacheKey;
        try {
            Object cached = redisTemplate.opsForValue().get(redisKey);
            if (cached instanceof List) {
                ruleIds = (List<Long>) cached;
                localRuleIdCache.put(cacheKey, ruleIds);
                return ruleIds;
            }
        } catch (Exception e) {
            log.warn("Redis缓存读取失败: key={}", cacheKey);
        }

        ruleIds = ruleMapper.selectMatchedRuleIds(warehouseCode, ownerCode, skuCode);

        if (ruleIds != null && !ruleIds.isEmpty()) {
            try {
                redisTemplate
                        .opsForValue()
                        .set(redisKey, ruleIds, REDIS_EXPIRE_HOURS, TimeUnit.HOURS);
            } catch (Exception e) {
                log.warn("Redis缓存写入失败: key={}", cacheKey);
            }
            localRuleIdCache.put(cacheKey, ruleIds);
        }

        return ruleIds;
    }

    public void evictRuleCache(Long ruleId) {
        if (ruleId == null) return;

        String redisKey = REDIS_RULE_PREFIX + ruleId;
        doEvict(redisKey, ruleId);

        // 延迟双删：500ms后再次删除，防止并发读写导致的缓存不一致
        cacheEvictExecutor.schedule(() -> doEvict(redisKey, ruleId), 500, TimeUnit.MILLISECONDS);
    }

    public void evictRuleIdsCache(String warehouseCode, String ownerCode, String skuCode) {
        String cacheKey = buildCacheKey(warehouseCode, ownerCode, skuCode);
        String redisKey = REDIS_RULE_IDS_PREFIX + cacheKey;

        try {
            redisTemplate.delete(redisKey);
        } catch (Exception e) {
            log.warn("Redis缓存删除失败: key={}", cacheKey);
        }
        localRuleIdCache.invalidate(cacheKey);
    }

    public void clearAllCache() {
        localRuleCache.invalidateAll();
        localRuleIdCache.invalidateAll();
    }

    public CacheStats getCacheStats() {
        CacheStats stats = new CacheStats();
        stats.setRuleCacheSize(localRuleCache.estimatedSize());
        stats.setRuleIdCacheSize(localRuleIdCache.estimatedSize());
        stats.setRuleHitRate(localRuleCache.stats().hitRate());
        stats.setRuleIdHitRate(localRuleIdCache.stats().hitRate());
        return stats;
    }

    private void doEvict(String redisKey, Long ruleId) {
        try {
            redisTemplate.delete(redisKey);
        } catch (Exception e) {
            log.warn("Redis缓存删除失败: ruleId={}", ruleId);
        }
        localRuleCache.invalidate(ruleId);
    }

    private String buildCacheKey(String warehouseCode, String ownerCode, String skuCode) {
        return (warehouseCode != null ? warehouseCode : "*")
                + ":"
                + (ownerCode != null ? ownerCode : "*")
                + ":"
                + (skuCode != null ? skuCode : "*");
    }

    @lombok.Data
    public static class CacheStats {
        private long ruleCacheSize;
        private long ruleIdCacheSize;
        private double ruleHitRate;
        private double ruleIdHitRate;
    }
}
