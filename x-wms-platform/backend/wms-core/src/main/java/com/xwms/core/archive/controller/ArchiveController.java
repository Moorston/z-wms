package com.xwms.core.archive.controller;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.common.core.Result;
import com.xwms.core.archive.entity.*;
import com.xwms.core.archive.service.ArchiveService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/** 数据归档 Controller */
@Tag(name = "数据归档", description = "归档规则/归档任务/批次处理/归档查询")
@RestController
@RequestMapping("/api/archive")
@RequiredArgsConstructor
public class ArchiveController {

    private final ArchiveService archiveService;

    // ============================================================

    // 归档规则
    // ============================================================

    @Operation(summary = "创建归档规则")
    @PostMapping("/rules")
    public Result<ArchiveRule> createRule(@RequestBody ArchiveRule rule) {
        return Result.success(archiveService.createRule(rule));
    }

    @Operation(summary = "查询归档规则详情")
    @GetMapping("/rules/{id}")
    public Result<ArchiveRule> getRule(@PathVariable Long id) {
        return Result.success(archiveService.getRule(id));
    }

    @Operation(summary = "分页查询归档规则")
    @GetMapping("/rules")
    public Result<Page<ArchiveRule>> pageRules(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String tableName,
            @RequestParam(required = false) Integer enabled) {
        return Result.success(archiveService.pageRules(new Page<>(page, size), tableName, enabled));
    }

    @Operation(summary = "查询启用的归档规则")
    @GetMapping("/rules/enabled")
    public Result<List<ArchiveRule>> getEnabledRules() {
        return Result.success(archiveService.getEnabledRules());
    }

    @Operation(summary = "启用/禁用归档规则")
    @PutMapping("/rules/{id}/toggle")
    public Result<ArchiveRule> toggleRule(@PathVariable Long id, @RequestParam boolean enabled) {
        return Result.success(archiveService.toggleRule(id, enabled));
    }

    // ============================================================

    // 归档任务
    // ============================================================

    @Operation(summary = "执行归档任务")
    @PostMapping("/execute/{ruleId}")
    public Result<ArchiveTask> executeArchive(
            @PathVariable Long ruleId,
            @RequestParam(required = false, defaultValue = "SYSTEM") String triggeredBy) {
        return Result.success(archiveService.executeArchive(ruleId, triggeredBy));
    }

    @Operation(summary = "查询归档任务详情")
    @GetMapping("/tasks/{id}")
    public Result<ArchiveTask> getTask(@PathVariable Long id) {
        return Result.success(archiveService.getTask(id));
    }

    @Operation(summary = "分页查询归档任务")
    @GetMapping("/tasks")
    public Result<Page<ArchiveTask>> pageTasks(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long ruleId,
            @RequestParam(required = false) String status) {
        return Result.success(archiveService.pageTasks(new Page<>(page, size), ruleId, status));
    }

    @Operation(summary = "查询规则的归档任务")
    @GetMapping("/tasks/rule/{ruleId}")
    public Result<List<ArchiveTask>> getTasksByRule(@PathVariable Long ruleId) {
        return Result.success(archiveService.getTasksByRule(ruleId));
    }

    @Operation(summary = "查询任务批次")
    @GetMapping("/tasks/{taskId}/batches")
    public Result<List<ArchiveBatch>> getTaskBatches(@PathVariable Long taskId) {
        return Result.success(archiveService.getTaskBatches(taskId));
    }

    // ============================================================

    // 归档数据查询
    // ============================================================

    @Operation(summary = "查询归档记录")
    @GetMapping("/records")
    public Result<ArchiveRecord> getArchiveRecord(
            @RequestParam String tableName, @RequestParam Long sourceId) {
        return Result.success(archiveService.getArchiveRecord(tableName, sourceId));
    }

    @Operation(summary = "查询批次归档记录")
    @GetMapping("/records/batch/{batchId}")
    public Result<List<ArchiveRecord>> getBatchRecords(@PathVariable Long batchId) {
        return Result.success(archiveService.getBatchRecords(batchId));
    }
}
