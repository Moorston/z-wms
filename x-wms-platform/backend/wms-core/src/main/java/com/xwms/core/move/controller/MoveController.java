package com.xwms.core.move.controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.common.core.Result;
import com.xwms.core.move.entity.*;
import com.xwms.core.move.service.MoveService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/** 库存移库管理 Controller */
@Tag(name = "库存移库管理", description = "移库单/移库任务/移库执行/移库流水")
@RestController
@RequestMapping("/api/move")
@RequiredArgsConstructor
public class MoveController {

    private final MoveService moveService;

    // ============================================================

    // 移库单
    // ============================================================

    @Operation(summary = "创建移库单")
    @PostMapping
    public Result<MoveOrder> createMoveOrder(
            @RequestBody MoveOrder order, @RequestParam(required = false) String operator) {
        return Result.success(
                moveService.createMoveOrder(
                        order,
                        order.getDetails() != null ? order.getDetails() : List.of(),
                        operator));
    }

    @Operation(summary = "下发移库单")
    @PostMapping("/{moveNo}/release")
    public Result<MoveOrder> releaseMoveOrder(
            @PathVariable String moveNo, @RequestParam(required = false) String operator) {
        return Result.success(moveService.releaseMoveOrder(moveNo, operator));
    }

    @Operation(summary = "开始执行移库单")
    @PostMapping("/{moveNo}/start")
    public Result<MoveOrder> startMoveOrder(
            @PathVariable String moveNo, @RequestParam(required = false) String operator) {
        return Result.success(moveService.startMoveOrder(moveNo, operator));
    }

    @Operation(summary = "取消移库单")
    @PostMapping("/{moveNo}/cancel")
    public Result<MoveOrder> cancelMoveOrder(
            @PathVariable String moveNo, @RequestParam(required = false) String operator) {
        return Result.success(moveService.cancelMoveOrder(moveNo, operator));
    }

    @Operation(summary = "分页查询移库单")
    @GetMapping
    public Result<Page<MoveOrder>> pageMoveOrders(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String warehouseCode,
            @RequestParam(required = false) String moveType,
            @RequestParam(required = false) String status) {
        return Result.success(
                moveService.pageMoveOrders(
                        new Page<>(page, size), warehouseCode, moveType, status));
    }

    @Operation(summary = "按单号查询移库单")
    @GetMapping("/{moveNo}")
    public Result<MoveOrder> getMoveOrderByNo(@PathVariable String moveNo) {
        return Result.success(moveService.getMoveOrderByNo(moveNo));
    }

    @Operation(summary = "查询移库明细")
    @GetMapping("/{moveNo}/details")
    public Result<List<MoveDetail>> getMoveDetails(@PathVariable String moveNo) {
        return Result.success(moveService.getMoveDetails(moveNo));
    }

    // ============================================================

    // 移库任务
    // ============================================================

    @Operation(summary = "分配移库任务")
    @PostMapping("/task/{taskNo}/assign")
    public Result<MoveTask> assignTask(@PathVariable String taskNo, @RequestParam String assignee) {
        return Result.success(moveService.assignTask(taskNo, assignee));
    }

    @Operation(summary = "开始执行移库任务")
    @PostMapping("/task/{taskNo}/start")
    public Result<MoveTask> startTask(
            @PathVariable String taskNo, @RequestParam(required = false) String operator) {
        return Result.success(moveService.startTask(taskNo, operator));
    }

    @Operation(summary = "执行移库")
    @PostMapping("/task/{taskNo}/execute")
    public Result<MoveTask> executeMove(
            @PathVariable String taskNo,
            @RequestParam BigDecimal qty,
            @RequestParam(required = false) String fromContainer,
            @RequestParam(required = false) String toContainer,
            @RequestParam(required = false) String operator) {
        return Result.success(
                moveService.executeMove(taskNo, qty, fromContainer, toContainer, operator));
    }

    @Operation(summary = "分页查询移库任务")
    @GetMapping("/task")
    public Result<Page<MoveTask>> pageTasks(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String moveNo,
            @RequestParam(required = false) String assignee,
            @RequestParam(required = false) String status) {
        return Result.success(
                moveService.pageTasks(new Page<>(page, size), moveNo, assignee, status));
    }

    @Operation(summary = "按任务号查询")
    @GetMapping("/task/{taskNo}")
    public Result<MoveTask> getTaskByNo(@PathVariable String taskNo) {
        return Result.success(moveService.getTaskByNo(taskNo));
    }

    @Operation(summary = "查询我的移库任务")
    @GetMapping("/task/my/{assignee}")
    public Result<List<MoveTask>> getMyTasks(@PathVariable String assignee) {
        return Result.success(moveService.getMyTasks(assignee));
    }

    @Operation(summary = "按移库单查询任务")
    @GetMapping("/{moveNo}/tasks")
    public Result<List<MoveTask>> getTasksByMoveNo(@PathVariable String moveNo) {
        return Result.success(moveService.getTasksByMoveNo(moveNo));
    }

    // ============================================================

    // 移库流水
    // ============================================================

    @Operation(summary = "按移库单查询流水")
    @GetMapping("/{moveNo}/logs")
    public Result<List<MoveLog>> getMoveLogsByMoveNo(@PathVariable String moveNo) {
        return Result.success(moveService.getMoveLogsByMoveNo(moveNo));
    }

    @Operation(summary = "按SKU查询移库流水")
    @GetMapping("/log/sku")
    public Result<List<MoveLog>> getMoveLogsBySku(
            @RequestParam String skuCode, @RequestParam(required = false) String batchNo) {
        return Result.success(moveService.getMoveLogsBySku(skuCode, batchNo));
    }
}
