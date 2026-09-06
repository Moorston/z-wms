package com.xwms.core.rf.controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.common.core.Result;
import com.xwms.core.rf.entity.*;
import com.xwms.core.rf.service.RfService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/** RF作业管理 Controller (PDA端专用) */
@Tag(name = "RF作业管理", description = "PDA现场作业: 任务/会话/扫码/菜单")
@RestController
@RequestMapping("/api/rf")
@RequiredArgsConstructor
public class RfController {

    private final RfService rfService;

    // ============================================================

    // RF会话
    // ============================================================

    @Operation(summary = "RF登录")
    @PostMapping("/login")
    public Result<RfSession> login(
            @RequestParam String userId,
            @RequestParam String userName,
            @RequestParam(required = false) String pdaDeviceId,
            @RequestParam String warehouse,
            @RequestParam(required = false) String ipAddress) {
        return Result.success(rfService.login(userId, userName, pdaDeviceId, warehouse, ipAddress));
    }

    @Operation(summary = "RF登出")
    @PostMapping("/logout")
    public Result<Void> logout(@RequestParam String sessionId) {
        rfService.logout(sessionId);
        return Result.success();
    }

    @Operation(summary = "心跳保活")
    @PostMapping("/heartbeat")
    public Result<Void> heartbeat(@RequestParam String sessionId) {
        rfService.heartbeat(sessionId);
        return Result.success();
    }

    @Operation(summary = "查询会话")
    @GetMapping("/session/{sessionId}")
    public Result<RfSession> getSession(@PathVariable String sessionId) {
        return Result.success(rfService.getSession(sessionId));
    }

    // ============================================================

    // RF任务
    // ============================================================

    @Operation(summary = "创建RF任务")
    @PostMapping("/tasks")
    public Result<RfTask> createTask(@RequestBody RfTask task) {
        return Result.success(rfService.createTask(task));
    }

    @Operation(summary = "查询任务详情")
    @GetMapping("/tasks/{id}")
    public Result<RfTask> getTask(@PathVariable Long id) {
        return Result.success(rfService.getTask(id));
    }

    @Operation(summary = "分页查询任务")
    @GetMapping("/tasks")
    public Result<Page<RfTask>> pageTasks(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String warehouse) {
        return Result.success(rfService.pageTasks(new Page<>(page, size), type, status, warehouse));
    }

    @Operation(summary = "查询待领取任务")
    @GetMapping("/tasks/pending")
    public Result<List<RfTask>> getPendingTasks(
            @RequestParam String type, @RequestParam String warehouse) {
        return Result.success(rfService.getPendingTasks(type, warehouse));
    }

    @Operation(summary = "查询我的任务")
    @GetMapping("/tasks/my")
    public Result<List<RfTask>> getMyTasks(@RequestParam String userId) {
        return Result.success(rfService.getMyTasks(userId));
    }

    @Operation(summary = "领取任务")
    @PutMapping("/tasks/{id}/accept")
    public Result<RfTask> acceptTask(
            @PathVariable Long id,
            @RequestParam String userId,
            @RequestParam String userName,
            @RequestParam(required = false) String pdaDeviceId) {
        return Result.success(rfService.acceptTask(id, userId, userName, pdaDeviceId));
    }

    @Operation(summary = "开始任务")
    @PutMapping("/tasks/{id}/start")
    public Result<RfTask> startTask(
            @PathVariable Long id,
            @RequestParam String userId,
            @RequestParam String userName,
            @RequestParam(required = false) String pdaDeviceId) {
        return Result.success(rfService.startTask(id, userId, userName, pdaDeviceId));
    }

    @Operation(summary = "扫码确认")
    @PutMapping("/tasks/{id}/scan")
    public Result<RfTask> scanConfirm(
            @PathVariable Long id,
            @RequestParam String userId,
            @RequestParam String userName,
            @RequestParam(required = false) String pdaDeviceId,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String barcode,
            @RequestParam(required = false) String sku,
            @RequestParam(required = false) String batchNo,
            @RequestParam(required = false) BigDecimal quantity) {
        return Result.success(
                rfService.scanConfirm(
                        id,
                        userId,
                        userName,
                        pdaDeviceId,
                        location,
                        barcode,
                        sku,
                        batchNo,
                        quantity));
    }

    @Operation(summary = "完成任务")
    @PutMapping("/tasks/{id}/complete")
    public Result<RfTask> completeTask(
            @PathVariable Long id,
            @RequestParam String userId,
            @RequestParam String userName,
            @RequestParam(required = false) String pdaDeviceId) {
        return Result.success(rfService.completeTask(id, userId, userName, pdaDeviceId));
    }

    @Operation(summary = "暂停任务")
    @PutMapping("/tasks/{id}/pause")
    public Result<RfTask> pauseTask(
            @PathVariable Long id,
            @RequestParam String userId,
            @RequestParam String userName,
            @RequestParam(required = false) String pdaDeviceId) {
        return Result.success(rfService.pauseTask(id, userId, userName, pdaDeviceId));
    }

    @Operation(summary = "继续任务")
    @PutMapping("/tasks/{id}/resume")
    public Result<RfTask> resumeTask(
            @PathVariable Long id,
            @RequestParam String userId,
            @RequestParam String userName,
            @RequestParam(required = false) String pdaDeviceId) {
        return Result.success(rfService.resumeTask(id, userId, userName, pdaDeviceId));
    }

    @Operation(summary = "取消任务")
    @PutMapping("/tasks/{id}/cancel")
    public Result<RfTask> cancelTask(
            @PathVariable Long id,
            @RequestParam String userId,
            @RequestParam String userName,
            @RequestParam(required = false) String pdaDeviceId,
            @RequestParam(required = false) String remark) {
        return Result.success(rfService.cancelTask(id, userId, userName, pdaDeviceId, remark));
    }

    // ============================================================

    // RF作业记录
    // ============================================================

    @Operation(summary = "查询任务作业日志")
    @GetMapping("/tasks/{id}/logs")
    public Result<List<RfWorkLog>> getTaskLogs(@PathVariable Long id) {
        return Result.success(rfService.getTaskLogs(id));
    }

    @Operation(summary = "查询用户作业日志")
    @GetMapping("/logs/user/{userId}")
    public Result<List<RfWorkLog>> getUserLogs(@PathVariable String userId) {
        return Result.success(rfService.getUserLogs(userId));
    }

    // ============================================================

    // RF菜单
    // ============================================================

    @Operation(summary = "查询所有菜单")
    @GetMapping("/menus")
    public Result<List<RfMenu>> getAllMenus() {
        return Result.success(rfService.getAllMenus());
    }

    @Operation(summary = "查询子菜单")
    @GetMapping("/menus/children")
    public Result<List<RfMenu>> getChildMenus(@RequestParam String parentCode) {
        return Result.success(rfService.getChildMenus(parentCode));
    }

    @Operation(summary = "创建菜单")
    @PostMapping("/menus")
    public Result<RfMenu> createMenu(@RequestBody RfMenu menu) {
        return Result.success(rfService.createMenu(menu));
    }
}
