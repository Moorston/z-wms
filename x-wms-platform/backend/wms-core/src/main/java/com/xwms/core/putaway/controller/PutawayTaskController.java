package com.xwms.core.putaway.controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.common.core.Result;
import com.xwms.core.putaway.entity.*;
import com.xwms.core.putaway.service.PutawayTaskService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 上架任务管理Controller（Sprint 3） 提供任务派发/领取/释放/上架确认/人工覆盖/异常处理等REST接口 */
@Slf4j
@RestController
@RequestMapping("/api/wms/putaway/task")
@RequiredArgsConstructor
@Tag(name = "上架任务管理", description = "上架任务派发/领取/释放/确认/覆盖/异常")
public class PutawayTaskController {

    private final PutawayTaskService putawayTaskService;

    // ============================================================
    // 任务派发
    // ============================================================

    @Operation(summary = "派发上架任务")
    @PostMapping("/{taskNo}/assign")
    public Result<PutawayTask> assignTask(
            @Parameter(description = "任务号") @PathVariable String taskNo,
            @Parameter(description = "指派人") @RequestParam String assignee,
            @Parameter(description = "派发人") @RequestParam String assigner,
            @Parameter(description = "工作区") @RequestParam(required = false) String workZone) {
        return Result.success(putawayTaskService.assignTask(taskNo, assignee, assigner, workZone));
    }

    @Operation(summary = "批量派发上架任务")
    @PostMapping("/batch-assign")
    public Result<List<PutawayTask>> batchAssignTasks(
            @Parameter(description = "任务号列表") @RequestBody List<String> taskNos,
            @Parameter(description = "指派人") @RequestParam String assignee,
            @Parameter(description = "派发人") @RequestParam String assigner,
            @Parameter(description = "工作区") @RequestParam(required = false) String workZone) {
        return Result.success(
                putawayTaskService.batchAssignTasks(taskNos, assignee, assigner, workZone));
    }

    // ============================================================
    // 任务领取
    // ============================================================

    @Operation(summary = "领取上架任务")
    @PostMapping("/{taskNo}/claim")
    public Result<PutawayTask> claimTask(
            @Parameter(description = "任务号") @PathVariable String taskNo,
            @Parameter(description = "操作人") @RequestParam String operator) {
        return Result.success(putawayTaskService.claimTask(taskNo, operator));
    }

    @Operation(summary = "获取待领取任务列表")
    @GetMapping("/pending")
    public Result<Page<PutawayTask>> getPendingTasks(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") int pageSize,
            @Parameter(description = "工作区") @RequestParam(required = false) String workZone,
            @Parameter(description = "仓库编码") @RequestParam(required = false) String warehouseCode,
            @Parameter(description = "优先级") @RequestParam(required = false) Integer priority) {
        Page<PutawayTask> page = new Page<>(pageNum, pageSize);
        return Result.success(
                putawayTaskService.getPendingTasks(page, workZone, warehouseCode, priority));
    }

    // ============================================================
    // 任务释放
    // ============================================================

    @Operation(summary = "释放上架任务")
    @PostMapping("/{taskNo}/release")
    public Result<PutawayTask> releaseTask(
            @Parameter(description = "任务号") @PathVariable String taskNo,
            @Parameter(description = "释放原因") @RequestParam(required = false) String reason,
            @Parameter(description = "操作人") @RequestParam String operator) {
        return Result.success(putawayTaskService.releaseTask(taskNo, reason, operator));
    }

    // ============================================================
    // 上架确认（含库存扣减）
    // ============================================================

    @Operation(summary = "上架确认（含库存扣减）")
    @PostMapping("/confirm")
    public Result<PutawayRecord> confirmPutaway(
            @Parameter(description = "任务号") @RequestParam String taskNo,
            @Parameter(description = "明细号") @RequestParam String detailNo,
            @Parameter(description = "目标库位") @RequestParam String targetLocation,
            @Parameter(description = "上架数量") @RequestParam BigDecimal putawayQty,
            @Parameter(description = "批次号") @RequestParam(required = false) String batchNo,
            @Parameter(description = "操作人") @RequestParam String operator) {
        return Result.success(
                putawayTaskService.confirmPutaway(
                        taskNo, detailNo, targetLocation, putawayQty, batchNo, operator));
    }

    // ============================================================
    // 人工覆盖推荐库位
    // ============================================================

    @Operation(summary = "人工覆盖推荐库位")
    @PostMapping("/detail/{detailNo}/override")
    public Result<PutawayTaskDetail> overrideLocation(
            @Parameter(description = "明细号") @PathVariable String detailNo,
            @Parameter(description = "新库位") @RequestParam String newLocation,
            @Parameter(description = "原因代码") @RequestParam(required = false) String reasonCode,
            @Parameter(description = "操作人") @RequestParam String operator) {
        return Result.success(
                putawayTaskService.overrideLocation(detailNo, newLocation, reasonCode, operator));
    }

    // ============================================================
    // 异常处理
    // ============================================================

    @Operation(summary = "上报上架异常")
    @PostMapping("/{taskNo}/exception")
    public Result<PutawayExceptionLog> reportException(
            @Parameter(description = "任务号") @PathVariable String taskNo,
            @Parameter(description = "明细号") @RequestParam(required = false) String detailNo,
            @Parameter(description = "异常类型") @RequestParam String exceptionType,
            @Parameter(description = "原因代码") @RequestParam(required = false) String reasonCode,
            @Parameter(description = "原因描述") @RequestParam(required = false) String reasonDesc,
            @Parameter(description = "操作人") @RequestParam String operator) {
        return Result.success(
                putawayTaskService.reportException(
                        taskNo, detailNo, exceptionType, reasonCode, reasonDesc, operator));
    }

    @Operation(summary = "解决上架异常")
    @PostMapping("/exception/{logId}/resolve")
    public Result<PutawayExceptionLog> resolveException(
            @Parameter(description = "例外日志ID") @PathVariable Long logId,
            @Parameter(description = "处理结果") @RequestParam String handleResult,
            @Parameter(description = "处理人") @RequestParam String handler,
            @Parameter(description = "恢复状态") @RequestParam(required = false) String resumeStatus) {
        return Result.success(
                putawayTaskService.resolveException(logId, handleResult, handler, resumeStatus));
    }

    @Operation(summary = "获取待处理异常列表")
    @GetMapping("/exception/pending")
    public Result<List<PutawayExceptionLog>> getPendingExceptions() {
        return Result.success(putawayTaskService.getPendingExceptions());
    }

    @Operation(summary = "获取任务异常日志")
    @GetMapping("/{taskNo}/exception")
    public Result<List<PutawayExceptionLog>> getExceptionLogsByTask(
            @Parameter(description = "任务号") @PathVariable String taskNo) {
        return Result.success(putawayTaskService.getExceptionLogsByTask(taskNo));
    }

    // ============================================================
    // 原因代码管理
    // ============================================================

    @Operation(summary = "按类型获取原因代码列表")
    @GetMapping("/reason-code")
    public Result<List<PutawayReasonCode>> getReasonCodesByType(
            @Parameter(description = "原因类型：OVERRIDE/EXCEPTION/DIFFERENCE") @RequestParam
                    String reasonType) {
        return Result.success(putawayTaskService.getReasonCodesByType(reasonType));
    }

    // ============================================================
    // 推荐日志查询
    // ============================================================

    @Operation(summary = "获取任务推荐日志")
    @GetMapping("/{taskNo}/recommend-log")
    public Result<List<PutawayRecommendLog>> getRecommendLogsByTask(
            @Parameter(description = "任务号") @PathVariable String taskNo) {
        return Result.success(putawayTaskService.getRecommendLogsByTask(taskNo));
    }
}
