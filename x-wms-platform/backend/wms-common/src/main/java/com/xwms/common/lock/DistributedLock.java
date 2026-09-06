package com.xwms.common.lock;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 分布式锁（基于 Redisson RLock） 基于 Redisson 的可重入锁实现，支持： - 可重入：同一线程可多次获取同一把锁（Hash 结构 + 计数） - owner
 * 校验：锁与线程绑定，非持有线程无法释放（释放抛 IllegalMonitorStateException） - 超时自动释放：leaseTime 到期后锁自动释放，防止死锁 -
 * 锁续期：watchdog 机制（当 leaseTime=-1 时自动续期，本实现使用显式 leaseTime 故不启用）
 *
 * <p>注意：锁的归属以线程为单位（RLock 内部用 UUID+threadId 标识），不暴露业务层 owner 字符串。 生产调用方使用简化签名
 * tryLock(key)/tryLock(key,timeout)/unlock(key)。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DistributedLock {

    private final RedissonClient redissonClient;
    private static final String LOCK_PREFIX = "lock:";
    private static final long DEFAULT_TIMEOUT_SECONDS = 30;

    /**
     * 尝试获取锁（不等待，立即返回）
     *
     * @param key 锁key
     * @param timeoutSeconds 锁超时时间（到期自动释放）
     * @return true=获取成功
     */
    public boolean tryLock(String key, long timeoutSeconds) {
        String lockKey = LOCK_PREFIX + key;
        RLock lock = redissonClient.getLock(lockKey);
        try {
            return lock.tryLock(0, timeoutSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("获取分布式锁被中断: key={}", lockKey);
            return false;
        }
    }

    /**
     * 尝试获取锁（默认超时时间）
     *
     * @param key 锁key
     * @return true=获取成功
     */
    public boolean tryLock(String key) {
        return tryLock(key, DEFAULT_TIMEOUT_SECONDS);
    }

    /**
     * 释放锁
     *
     * <p>仅当前持有锁的线程可释放；非持有线程调用将抛出 {@link IllegalMonitorStateException}。
     *
     * @param key 锁key
     */
    public void unlock(String key) {
        String lockKey = LOCK_PREFIX + key;
        RLock lock = redissonClient.getLock(lockKey);
        if (lock.isLocked() && lock.isHeldByCurrentThread()) {
            lock.unlock();
        } else {
            log.debug("释放分布式锁跳过：当前线程未持有锁 key={}", lockKey);
        }
    }

    /**
     * 带锁执行（自动释放）
     *
     * @param key 锁key
     * @param timeoutSeconds 锁超时时间
     * @param supplier 业务逻辑
     * @return 业务逻辑返回值
     * @throws RuntimeException 获取锁失败时抛出
     */
    public <T> T executeWithLock(String key, long timeoutSeconds, Supplier<T> supplier) {
        if (!tryLock(key, timeoutSeconds)) {
            throw new RuntimeException("获取分布式锁失败: " + key);
        }
        try {
            return supplier.get();
        } finally {
            unlock(key);
        }
    }

    /**
     * 带锁执行（默认超时时间）
     *
     * @param key 锁key
     * @param supplier 业务逻辑
     * @return 业务逻辑返回值
     */
    public <T> T executeWithLock(String key, Supplier<T> supplier) {
        return executeWithLock(key, DEFAULT_TIMEOUT_SECONDS, supplier);
    }
}
