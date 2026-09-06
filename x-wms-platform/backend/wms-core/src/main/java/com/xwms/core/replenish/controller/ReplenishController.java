package com.xwms.core.replenish.controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.common.core.Result;
import com.xwms.core.replenish.dto.ReplenishCreateRequest;
import com.xwms.core.replenish.entity.ReplenishTask;
import com.xwms.core.replenish.service.ReplenishService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/** 补货管理 Controller */
@Tag(name = "补货管理", description = "补货规则/补货任务/在途库存")
@RestController
@RequestMapping("/api/replenish")
@RequiredArgsConstructor
public class ReplenishController {

    private final ReplenishService replenishService;

    // ============================================================

    // 补货任务
    // ============================================================

    @Operation(summary = "创建补货任务")
    @PostMapping("/tasks")
    public Result<ReplenishTask> createTask(@RequestBody ReplenishCreateRequest request) {
        return Result.success(replenishService.createReplenishTask(request));
    }

    @Operation(summary = "紧急补货触发")
    @PostMapping("/tasks/urgent")
    public Result<ReplenishTask> triggerUrgent(
            @RequestParam String sku,
            @RequestParam String pickLocation,
            @RequestParam BigDecimal needQty,
            @RequestParam String ownerCode,
            @RequestParam String warehouseCode) {
        return Result.success(
                replenishService.triggerUrgent(
                        sku, pickLocation, needQty, ownerCode, warehouseCode));
    }

    @Operation(summary = "开始补货拣货")
    @PutMapping("/tasks/{id}/pick")
    public Result<ReplenishTask> startPick(@PathVariable Long id, @RequestParam String assignee) {
        return Result.success(replenishService.startPick(id, assignee));
    }

    @Operation(summary = "确认补货上架")
    @PutMapping("/tasks/{id}/putaway")
    public Result<ReplenishTask> confirmPutaway(
            @PathVariable Long id, @RequestParam BigDecimal actualQty) {
        return Result.success(replenishService.confirmPutaway(id, actualQty));
    }

    @Operation(summary = "补货异常处理")
    @PutMapping("/tasks/{id}/exception")
    public Result<ReplenishTask> handleException(
            @PathVariable Long id, @RequestParam String reason) {
        return Result.success(replenishService.handleException(id, reason));
    }

    @Operation(summary = "取消补货任务")
    @PutMapping("/tasks/{id}/cancel")
    public Result<ReplenishTask> cancelTask(@PathVariable Long id) {
        return Result.success(replenishService.cancelTask(id));
    }

    @Operation(summary = "查询补货任务详情")
    @GetMapping("/tasks/{id}")
    public Result<ReplenishTask> getTask(@PathVariable Long id) {
        return Result.success(replenishService.getTask(id));
    }

    @Operation(summary = "分页查询补货任务")
    @GetMapping("/tasks")
    public Result<Page<ReplenishTask>> pageTasks(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String sku,
            @RequestParam(required = false) String type) {
        return Result.success(
                replenishService.pageTasks(new Page<>(page, size), status, sku, type));
    }

    @Operation(summary = "查询待处理补货任务")
    @GetMapping("/tasks/pending")
    public Result<List<ReplenishTask>> getPendingTasks(@RequestParam String warehouseCode) {
        return Result.success(replenishService.getPendingTasks(warehouseCode));
    }

    @Operation(summary = "定时补货检查(手动触发)")
    @PostMapping("/batch-check")
    public Result<Integer> batchCheck(@RequestParam String warehouseCode) {
        return Result.success(replenishService.batchCheckReplenish(warehouseCode));
    }
}
