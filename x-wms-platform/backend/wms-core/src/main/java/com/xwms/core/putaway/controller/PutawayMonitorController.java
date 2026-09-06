package com.xwms.core.putaway.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.*;

import com.xwms.common.core.Result;
import com.xwms.core.putaway.service.PutawayPerformanceService;
import com.xwms.core.putawayrule.cache.PutawayRuleCacheService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 上架性能监控Controller（Sprint 4） 提供缓存统计、推荐性能、任务统计、作业效率等监控接口 */
@Slf4j
@RestController
@RequestMapping("/api/wms/putaway/monitor")
@RequiredArgsConstructor
@Tag(name = "上架性能监控", description = "缓存统计/推荐性能/任务统计/作业效率")
public class PutawayMonitorController {

    private final PutawayPerformanceService performanceService;
    private final PutawayRuleCacheService cacheService;

    @Operation(summary = "获取完整性能指标")
    @GetMapping("/metrics")
    public Result<Map<String, Object>> getPerformanceMetrics() {
        return Result.success(performanceService.getPerformanceMetrics());
    }

    @Operation(summary = "获取缓存统计")
    @GetMapping("/cache/stats")
    public Result<PutawayRuleCacheService.CacheStats> getCacheStats() {
        return Result.success(cacheService.getCacheStats());
    }

    @Operation(summary = "清空规则缓存")
    @PostMapping("/cache/clear")
    public Result<String> clearCache() {
        cacheService.clearAllCache();
        return Result.success("缓存已清空");
    }

    @Operation(summary = "失效指定规则缓存")
    @PostMapping("/cache/evict/{ruleId}")
    public Result<String> evictRuleCache(@PathVariable Long ruleId) {
        cacheService.evictRuleCache(ruleId);
        return Result.success("规则缓存已失效: ruleId=" + ruleId);
    }

    @Operation(summary = "重置性能统计指标")
    @PostMapping("/metrics/reset")
    public Result<String> resetMetrics() {
        performanceService.resetMetrics();
        return Result.success("性能统计指标已重置");
    }
}
