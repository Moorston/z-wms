package com.xwms.integration.api.monitor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/** API监控服务 统计每个API的调用量、成功率、平均耗时、错误分布 支持实时看板和告警 */
@Slf4j
@Service
public class ApiMonitorService {

    /** apiId -> 统计数据 */
    private final Map<String, ApiStats> stats = new ConcurrentHashMap<>();

    @Data
    public static class ApiStats {
        private final AtomicLong totalCalls = new AtomicLong(0);
        private final AtomicLong successCalls = new AtomicLong(0);
        private final AtomicLong failedCalls = new AtomicLong(0);
        private final AtomicLong totalDurationMs = new AtomicLong(0);
        private final AtomicLong maxDurationMs = new AtomicLong(0);
        private volatile LocalDateTime lastCallTime;
        private volatile LocalDateTime lastErrorTime;
        private volatile String lastError;

        public double getSuccessRate() {
            long total = totalCalls.get();
            return total == 0 ? 100.0 : (successCalls.get() * 100.0 / total);
        }

        public double getAvgDurationMs() {
            long total = totalCalls.get();
            return total == 0 ? 0 : totalDurationMs.get() / (double) total;
        }
    }

    /** 记录调用 */
    public void recordCall(String apiId, boolean success, long durationMs, String error) {
        ApiStats s = stats.computeIfAbsent(apiId, k -> new ApiStats());
        s.totalCalls.incrementAndGet();
        s.lastCallTime = LocalDateTime.now();
        s.totalDurationMs.addAndGet(durationMs);
        if (durationMs > s.maxDurationMs.get()) {
            s.maxDurationMs.set(durationMs);
        }
        if (success) {
            s.successCalls.incrementAndGet();
        } else {
            s.failedCalls.incrementAndGet();
            s.lastErrorTime = LocalDateTime.now();
            s.lastError = error;
            // 成功率低于90%告警
            if (s.totalCalls.get() > 100 && s.getSuccessRate() < 90) {
                log.error(
                        "API告警: apiId={}, 成功率={}%, 最近错误={}",
                        apiId, String.format("%.1f", s.getSuccessRate()), error);
            }
        }
    }

    /** 获取API统计 */
    public ApiStats getStats(String apiId) {
        return stats.get(apiId);
    }

    /** 获取所有统计（用于看板） */
    public Map<String, ApiStats> getAllStats() {
        return Map.copyOf(stats);
    }

    /** 重置统计（每日零点定时任务调用） */
    public void resetStats() {
        stats.clear();
        log.info("API监控统计已重置");
    }
}
