package com.xwms.integration.api.ratelimit;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

/**
 * API限流服务 支持两种算法： 1. TOKEN_BUCKET - 令牌桶（允许突发） 2. SLIDING_WINDOW - 滑动窗口（精确控制QPS）
 *
 * <p>分布式限流用Redis，本地限流用AtomicLong
 */
@Slf4j
@Service
public class ApiRateLimiter {

    private final RedisTemplate<String, Object> redisTemplate;

    /** 本地令牌桶：key -> 剩余令牌数 */
    private final Map<String, AtomicLong> localBuckets = new ConcurrentHashMap<>();

    /** 本地最后填充时间 */
    private final Map<String, AtomicLong> lastFillTime = new ConcurrentHashMap<>();

    public ApiRateLimiter(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 尝试获取令牌
     *
     * @param key 限流key（apiId 或 appId:apiId）
     * @param qps 每秒令牌数
     * @param burst 突发容量
     * @param algorithm 算法
     * @return true=允许通过，false=限流拒绝
     */
    public boolean tryAcquire(String key, int qps, int burst, String algorithm) {
        return "SLIDING_WINDOW".equals(algorithm)
                ? slidingWindowCheck(key, qps)
                : tokenBucketCheck(key, qps, burst);
    }

    /** 令牌桶算法（本地实现，高性能） */
    private boolean tokenBucketCheck(String key, int qps, int burst) {
        long now = System.currentTimeMillis();
        AtomicLong tokens = localBuckets.computeIfAbsent(key, k -> new AtomicLong(burst));
        AtomicLong lastTime = lastFillTime.computeIfAbsent(key, k -> new AtomicLong(now));

        // 填充令牌
        long elapsed = now - lastTime.get();
        if (elapsed > 0) {
            long newTokens = (elapsed * qps) / 1000;
            if (newTokens > 0) {
                tokens.updateAndGet(t -> Math.min(burst, t + newTokens));
                lastTime.set(now);
            }
        }
        // 尝试获取
        if (tokens.get() > 0) {
            tokens.decrementAndGet();
            return true;
        }
        log.warn("API限流触发(令牌桶): key={}, qps={}", key, qps);
        return false;
    }

    /** 滑动窗口算法（Redis实现，分布式精确） 使用Redis ZSet记录请求时间戳，窗口内计数 */
    private boolean slidingWindowCheck(String key, int qps) {
        try {
            String redisKey = "ratelimit:sliding:" + key;
            long now = System.currentTimeMillis();
            long windowStart = now - 1000; // 1秒窗口

            // 移除窗口外的记录
            redisTemplate.opsForZSet().removeRangeByScore(redisKey, 0, windowStart);
            // 当前窗口请求数
            Long count = redisTemplate.opsForZSet().zCard(redisKey);
            if (count != null && count >= qps) {
                log.warn("API限流触发(滑动窗口): key={}, count={}, qps={}", key, count, qps);
                return false;
            }
            // 添加当前请求
            redisTemplate.opsForZSet().add(redisKey, String.valueOf(now), now);
            return true;
        } catch (Exception e) {
            log.error("滑动窗口限流异常，降级放行: key={}", key, e);
            return true; // Redis故障时降级放行，保护可用性
        }
    }
}
