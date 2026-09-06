package com.xwms.core.lock.controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.common.core.Result;
import com.xwms.core.lock.entity.*;
import com.xwms.core.lock.service.InventoryLockService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/** 库存锁管理 Controller */
@Tag(name = "库存锁管理", description = "锁规则/锁申请/锁释放/死锁检测")
@RestController
@RequestMapping("/api/lock")
@RequiredArgsConstructor
public class InventoryLockController {

    private final InventoryLockService lockService;

    // ============================================================

    // 锁规则
    // ============================================================

    @Operation(summary = "创建锁规则")
    @PostMapping("/rule")
    public Result<LockRule> createRule(@RequestBody LockRule rule) {
        return Result.success(lockService.createRule(rule));
    }

    @Operation(summary = "分页查询锁规则")
    @GetMapping("/rule")
    public Result<Page<LockRule>> pageRules(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.success(lockService.pageRules(new Page<>(page, size)));
    }

    @Operation(summary = "按编码查询锁规则")
    @GetMapping("/rule/{ruleCode}")
    public Result<LockRule> getRuleByCode(@PathVariable String ruleCode) {
        return Result.success(lockService.getRuleByCode(ruleCode));
    }

    // ============================================================

    // 锁申请与释放
    // ============================================================

    @Operation(summary = "申请库存锁")
    @PostMapping("/acquire")
    public Result<InventoryLock> acquireLock(
            @RequestParam String lockKey,
            @RequestParam String lockScope,
            @RequestParam String lockType,
            @RequestParam(required = false) String businessType,
            @RequestParam(required = false) String businessNo,
            @RequestParam String holder,
            @RequestParam(required = false) BigDecimal lockQuantity) {
        return Result.success(
                lockService.acquireLock(
                        lockKey,
                        lockScope,
                        lockType,
                        businessType,
                        businessNo,
                        holder,
                        lockQuantity));
    }

    @Operation(summary = "释放库存锁")
    @PostMapping("/release/{lockId}")
    public Result<Boolean> releaseLock(@PathVariable String lockId) {
        return Result.success(lockService.releaseLock(lockId));
    }

    @Operation(summary = "释放业务单关联的所有锁")
    @PostMapping("/release/business/{businessNo}")
    public Result<Integer> releaseLocksByBusinessNo(@PathVariable String businessNo) {
        return Result.success(lockService.releaseLocksByBusinessNo(businessNo));
    }

    // ============================================================

    // 死锁检测
    // ============================================================

    @Operation(summary = "执行死锁检测")
    @PostMapping("/deadlock/detect")
    public Result<List<DeadlockLog>> detectDeadlocks() {
        return Result.success(lockService.detectDeadlocks());
    }

    @Operation(summary = "查询最近死锁日志")
    @GetMapping("/deadlock/recent")
    public Result<List<DeadlockLog>> getRecentDeadlocks(
            @RequestParam(defaultValue = "20") int limit) {
        return Result.success(lockService.getRecentDeadlocks(limit));
    }

    // ============================================================

    // 超时清理
    // ============================================================

    @Operation(summary = "清理超时的锁和等待")
    @PostMapping("/cleanup/timeout")
    public Result<Integer> cleanupTimeout() {
        return Result.success(lockService.cleanupTimeout());
    }

    // ============================================================

    // 查询
    // ============================================================

    @Operation(summary = "按ID查询锁")
    @GetMapping("/{lockId}")
    public Result<InventoryLock> getLockById(@PathVariable String lockId) {
        return Result.success(lockService.getLockById(lockId));
    }

    @Operation(summary = "按锁键查询持有中的锁")
    @GetMapping("/held/key/{lockKey}")
    public Result<List<InventoryLock>> getHeldLocksByLockKey(@PathVariable String lockKey) {
        return Result.success(lockService.getHeldLocksByLockKey(lockKey));
    }

    @Operation(summary = "按业务单号查询持有中的锁")
    @GetMapping("/held/business/{businessNo}")
    public Result<List<InventoryLock>> getHeldLocksByBusinessNo(@PathVariable String businessNo) {
        return Result.success(lockService.getHeldLocksByBusinessNo(businessNo));
    }

    @Operation(summary = "按持有者查询持有中的锁")
    @GetMapping("/held/holder/{holder}")
    public Result<List<InventoryLock>> getHeldLocksByHolder(@PathVariable String holder) {
        return Result.success(lockService.getHeldLocksByHolder(holder));
    }

    @Operation(summary = "分页查询锁记录")
    @GetMapping("/list")
    public Result<Page<InventoryLock>> pageLocks(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status) {
        return Result.success(lockService.pageLocks(new Page<>(page, size), status));
    }

    @Operation(summary = "分页查询等待队列")
    @GetMapping("/wait/list")
    public Result<Page<LockWaitQueue>> pageWaitQueue(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status) {
        return Result.success(lockService.pageWaitQueue(new Page<>(page, size), status));
    }
}
