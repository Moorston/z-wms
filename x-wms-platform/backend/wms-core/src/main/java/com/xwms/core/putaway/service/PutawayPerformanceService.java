package com.xwms.core.putaway.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Service;

import com.xwms.core.putawayrule.cache.PutawayRuleCacheService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 上架性能监控服务 提供缓存统计、推荐性能指标、事件统计、作业效率监控 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PutawayPerformanceService {

    private final PutawayRuleCacheService cacheService;

    private final AtomicLong recommendCount = new AtomicLong(0);
    private final AtomicLong recommendSuccessCount = new AtomicLong(0);
    private final AtomicLong recommendTotalTimeMs = new AtomicLong(0);
    private final AtomicLong recommendMaxTimeMs = new AtomicLong(0);

    private final AtomicLong taskCreatedCount = new AtomicLong(0);
    private final AtomicLong taskCompletedCount = new AtomicLong(0);
    private final AtomicLong taskExceptionCount = new AtomicLong(0);
    private final AtomicLong taskOverrideCount = new AtomicLong(0);

    private final AtomicLong putawayTotalQty = new AtomicLong(0);
    private final AtomicLong putawayTotalTimeMs = new AtomicLong(0);

    /** 记录推荐请求 */
    public void recordRecommend(long responseTimeMs, boolean success) {
        recommendCount.incrementAndGet();
        if (success) recommendSuccessCount.incrementAndGet();
        recommendTotalTimeMs.addAndGet(responseTimeMs);
        recommendMaxTimeMs.accumulateAndGet(responseTimeMs, Math::max);
    }

    /** 记录任务创建 */
    public void recordTaskCreated() {
        taskCreatedCount.incrementAndGet();
    }

    /** 记录任务完成 */
    public void recordTaskCompleted(long durationMs, long qty) {
        taskCompletedCount.incrementAndGet();
        putawayTotalTimeMs.addAndGet(durationMs);
        putawayTotalQty.addAndGet(qty);
    }

    /** 记录任务异常 */
    public void recordTaskException() {
        taskExceptionCount.incrementAndGet();
    }

    /** 记录人工覆盖 */
    public void recordTaskOverride() {
        taskOverrideCount.incrementAndGet();
    }

    /** 获取完整性能指标 */
    public Map<String, Object> getPerformanceMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        // 缓存统计
        PutawayRuleCacheService.CacheStats cacheStats = cacheService.getCacheStats();
        Map<String, Object> cacheMetrics = new HashMap<>();
        cacheMetrics.put("ruleCacheSize", cacheStats.getRuleCacheSize());
        cacheMetrics.put("ruleIdCacheSize", cacheStats.getRuleIdCacheSize());
        cacheMetrics.put("ruleHitRate", String.format("%.2f%%", cacheStats.getRuleHitRate() * 100));
        cacheMetrics.put(
                "ruleIdHitRate", String.format("%.2f%%", cacheStats.getRuleIdHitRate() * 100));
        metrics.put("cache", cacheMetrics);

        // 推荐性能
        Map<String, Object> recommendMetrics = new HashMap<>();
        long total = recommendCount.get();
        recommendMetrics.put("totalCount", total);
        recommendMetrics.put("successCount", recommendSuccessCount.get());
        recommendMetrics.put(
                "successRate",
                total > 0
                        ? String.format("%.2f%%", recommendSuccessCount.get() * 100.0 / total)
                        : "0%");
        recommendMetrics.put("avgTimeMs", total > 0 ? recommendTotalTimeMs.get() / total : 0);
        recommendMetrics.put("maxTimeMs", recommendMaxTimeMs.get());
        metrics.put("recommend", recommendMetrics);

        // 任务统计
        Map<String, Object> taskMetrics = new HashMap<>();
        taskMetrics.put("createdCount", taskCreatedCount.get());
        taskMetrics.put("completedCount", taskCompletedCount.get());
        taskMetrics.put("exceptionCount", taskExceptionCount.get());
        taskMetrics.put("overrideCount", taskOverrideCount.get());
        long completed = taskCompletedCount.get();
        taskMetrics.put(
                "exceptionRate",
                completed > 0
                        ? String.format(
                                "%.2f%%",
                                taskExceptionCount.get()
                                        * 100.0
                                        / (completed + taskExceptionCount.get()))
                        : "0%");
        taskMetrics.put(
                "overrideRate",
                completed > 0
                        ? String.format("%.2f%%", taskOverrideCount.get() * 100.0 / completed)
                        : "0%");
        metrics.put("task", taskMetrics);

        // 作业效率
        Map<String, Object> efficiencyMetrics = new HashMap<>();
        efficiencyMetrics.put("totalPutawayQty", putawayTotalQty.get());
        efficiencyMetrics.put(
                "avgTaskDurationMs", completed > 0 ? putawayTotalTimeMs.get() / completed : 0);
        efficiencyMetrics.put(
                "qtyPerHour",
                completed > 0 && putawayTotalTimeMs.get() > 0
                        ? putawayTotalQty.get() * 3600000L / putawayTotalTimeMs.get()
                        : 0);
        metrics.put("efficiency", efficiencyMetrics);

        metrics.put("reportTime", LocalDateTime.now().toString());

        return metrics;
    }

    /** 重置统计指标 */
    public void resetMetrics() {
        recommendCount.set(0);
        recommendSuccessCount.set(0);
        recommendTotalTimeMs.set(0);
        recommendMaxTimeMs.set(0);
        taskCreatedCount.set(0);
        taskCompletedCount.set(0);
        taskExceptionCount.set(0);
        taskOverrideCount.set(0);
        putawayTotalQty.set(0);
        putawayTotalTimeMs.set(0);
        log.info("上架性能统计指标已重置");
    }
}
