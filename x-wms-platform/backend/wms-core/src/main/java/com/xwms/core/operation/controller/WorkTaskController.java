package com.xwms.core.operation.controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.common.core.PageResult;
import com.xwms.common.core.Result;
import com.xwms.core.operation.entity.WorkTask;
import com.xwms.core.operation.mapper.WorkTaskMapper;
import com.xwms.core.operation.service.WorkTaskService;

import lombok.RequiredArgsConstructor;

/** 作业任务Controller PDA端核心接口：待执行列表/领取任务/完成/异常上报 */
@RestController
@RequestMapping("/api/work-task")
@RequiredArgsConstructor
public class WorkTaskController {

    private final WorkTaskService workTaskService;
    private final WorkTaskMapper workTaskMapper;

    /** 创建作业任务 */
    @PostMapping
    public Result<WorkTask> create(@RequestBody WorkTask task) {
        return Result.success(workTaskService.create(task));
    }

    /** 分页查询作业任务 */
    @GetMapping("/page")
    public Result<PageResult<WorkTask>> page(
            @RequestParam(required = false) String taskNo,
            @RequestParam(required = false) String taskType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String warehouse,
            @RequestParam(required = false) String operator,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        Page<WorkTask> page =
                workTaskMapper.selectPage(
                        new Page<>(pageNum, pageSize),
                        new LambdaQueryWrapper<WorkTask>()
                                .like(taskNo != null, WorkTask::getTaskNo, taskNo)
                                .eq(taskType != null, WorkTask::getTaskType, taskType)
                                .eq(status != null, WorkTask::getStatus, status)
                                .eq(warehouse != null, WorkTask::getWarehouse, warehouse)
                                .eq(operator != null, WorkTask::getOperator, operator)
                                .orderByDesc(WorkTask::getPriority)
                                .orderByAsc(WorkTask::getCreatedAt));
        return Result.success(
                PageResult.of(
                        page.getRecords(),
                        page.getTotal(),
                        (int) page.getCurrent(),
                        (int) page.getSize()));
    }

    /** PDA端：查询待执行任务（按优先级排序） */
    @GetMapping("/pending")
    public Result<List<WorkTask>> listPending(
            @RequestParam String warehouse,
            @RequestParam(required = false) String taskType,
            @RequestParam(defaultValue = "20") Integer limit) {
        return Result.success(workTaskService.listPending(warehouse, taskType, limit));
    }

    /** PDA端：领取任务（Redis分布式锁防重复领取） */
    @PostMapping("/{taskNo}/claim")
    public Result<WorkTask> claim(
            @PathVariable String taskNo,
            @RequestParam String operator,
            @RequestParam String deviceId) {
        return Result.success(workTaskService.claim(taskNo, operator, deviceId));
    }

    /** PDA端：完成任务 */
    @PostMapping("/{taskNo}/complete")
    public Result<Void> complete(
            @PathVariable String taskNo,
            @RequestParam BigDecimal actualQty,
            @RequestParam String operator) {
        workTaskService.complete(taskNo, actualQty, operator);
        return Result.success();
    }

    /** PDA端：异常上报 */
    @PostMapping("/{taskNo}/exception")
    public Result<Void> reportException(
            @PathVariable String taskNo,
            @RequestParam String reason,
            @RequestParam String operator) {
        workTaskService.reportException(taskNo, reason, operator);
        return Result.success();
    }
}
