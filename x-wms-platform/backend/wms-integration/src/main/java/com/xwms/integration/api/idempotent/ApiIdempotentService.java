package com.xwms.integration.api.idempotent;

import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

/**
 * API幂等服务 防止外部系统重复调用导致数据重复（如重复下单、重复扣减） 实现方式： 1. 业务幂等键（如订单号+操作类型） 2. Redis SETNX去重（24小时有效） 3.
 * 处理中状态锁定（防止并发重复处理）
 */
@Slf4j
@Service
public class ApiIdempotentService {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String KEY_PREFIX = "idempotent:";
    private static final long DEFAULT_TTL_HOURS = 24;

    /** 幂等状态 */
    public enum IdempotentStatus {
        PROCESSING, // 处理中
        SUCCESS, // 处理成功
        FAILED // 处理失败
    }

    public ApiIdempotentService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 尝试获取幂等锁
     *
     * @param idempotentKey 幂等键（如 order:create:ORDER123）
     * @return true=首次请求，false=重复请求
     */
    public boolean tryAcquire(String idempotentKey) {
        String key = KEY_PREFIX + idempotentKey;
        Boolean success =
                redisTemplate
                        .opsForValue()
                        .setIfAbsent(
                                key,
                                IdempotentStatus.PROCESSING.name(),
                                DEFAULT_TTL_HOURS,
                                TimeUnit.HOURS);
        if (Boolean.TRUE.equals(success)) {
            log.info("幂等锁获取成功: key={}", idempotentKey);
            return true;
        }
        log.warn("幂等锁获取失败（重复请求）: key={}", idempotentKey);
        return false;
    }

    /** 标记处理完成 */
    public void markSuccess(String idempotentKey, Object result) {
        String key = KEY_PREFIX + idempotentKey;
        redisTemplate
                .opsForValue()
                .set(
                        key,
                        IdempotentStatus.SUCCESS.name()
                                + ":"
                                + (result != null ? result.toString() : ""),
                        DEFAULT_TTL_HOURS,
                        TimeUnit.HOURS);
    }

    /** 标记处理失败（允许重试） */
    public void markFailed(String idempotentKey) {
        String key = KEY_PREFIX + idempotentKey;
        redisTemplate.delete(key); // 失败时删除，允许重试
    }

    /** 获取已处理结果（重复请求时返回缓存结果） */
    public Object getCachedResult(String idempotentKey) {
        String key = KEY_PREFIX + idempotentKey;
        Object value = redisTemplate.opsForValue().get(key);
        if (value != null && value.toString().startsWith("SUCCESS:")) {
            return value.toString().substring(8);
        }
        return null;
    }

    /** 检查是否正在处理中 */
    public boolean isProcessing(String idempotentKey) {
        String key = KEY_PREFIX + idempotentKey;
        Object value = redisTemplate.opsForValue().get(key);
        return value != null && IdempotentStatus.PROCESSING.name().equals(value.toString());
    }
}
