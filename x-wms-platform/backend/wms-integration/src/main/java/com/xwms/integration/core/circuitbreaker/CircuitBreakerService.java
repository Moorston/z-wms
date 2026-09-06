package com.xwms.integration.core.circuitbreaker;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

/**
 * 熔断器服务 三态模型：CLOSED（闭合）-> OPEN（打开）-> HALF_OPEN（半开）-> CLOSED - CLOSED：正常调用，统计失败率 -
 * OPEN：失败率超阈值，直接拒绝，等待冷却时间 - HALF_OPEN：冷却后放行少量请求探测，成功则恢复，失败则继续打开
 */
@Slf4j
@Service
public class CircuitBreakerService {

    /** 熔断器状态 */
    public enum State {
        CLOSED,
        OPEN,
        HALF_OPEN
    }

    /** 熔断器配置 */
    private static final int FAILURE_THRESHOLD = 5; // 连续失败5次打开

    private static final long OPEN_TIMEOUT_MS = 30000; // 打开状态30秒后半开
    private static final int HALF_OPEN_MAX_CALLS = 3; // 半开状态最多3个探测请求

    private final Map<String, CircuitBreaker> breakers = new ConcurrentHashMap<>();

    /**
     * 检查是否允许调用
     *
     * @param key 熔断器key（适配器ID或API ID）
     * @return true=允许调用，false=熔断拒绝
     */
    public boolean allowRequest(String key) {
        CircuitBreaker cb = breakers.computeIfAbsent(key, k -> new CircuitBreaker());
        synchronized (cb) {
            if (cb.state == State.OPEN) {
                if (System.currentTimeMillis() - cb.openTime > OPEN_TIMEOUT_MS) {
                    cb.state = State.HALF_OPEN;
                    cb.halfOpenCount.set(0);
                    log.info("熔断器半开: key={}", key);
                } else {
                    return false;
                }
            }
            if (cb.state == State.HALF_OPEN && cb.halfOpenCount.get() >= HALF_OPEN_MAX_CALLS) {
                return false;
            }
            if (cb.state == State.HALF_OPEN) {
                cb.halfOpenCount.incrementAndGet();
            }
            return true;
        }
    }

    /** 记录成功 */
    public void recordSuccess(String key) {
        CircuitBreaker cb = breakers.get(key);
        if (cb == null) return;
        synchronized (cb) {
            cb.failureCount.set(0);
            if (cb.state == State.HALF_OPEN) {
                cb.state = State.CLOSED;
                log.info("熔断器恢复闭合: key={}", key);
            }
        }
    }

    /** 记录失败 */
    public void recordFailure(String key) {
        CircuitBreaker cb = breakers.computeIfAbsent(key, k -> new CircuitBreaker());
        synchronized (cb) {
            cb.failureCount.incrementAndGet();
            if (cb.state == State.HALF_OPEN) {
                cb.state = State.OPEN;
                cb.openTime = System.currentTimeMillis();
                log.warn("熔断器半开失败，重新打开: key={}", key);
            } else if (cb.failureCount.get() >= FAILURE_THRESHOLD && cb.state == State.CLOSED) {
                cb.state = State.OPEN;
                cb.openTime = System.currentTimeMillis();
                log.warn("熔断器打开: key={}, 连续失败{}次", key, FAILURE_THRESHOLD);
            }
        }
    }

    /** 获取熔断器状态 */
    public State getState(String key) {
        CircuitBreaker cb = breakers.get(key);
        return cb == null ? State.CLOSED : cb.state;
    }

    /** 熔断器内部状态 */
    private static class CircuitBreaker {
        volatile State state = State.CLOSED;
        final AtomicInteger failureCount = new AtomicInteger(0);
        final AtomicInteger halfOpenCount = new AtomicInteger(0);
        volatile long openTime = 0;
    }
}
