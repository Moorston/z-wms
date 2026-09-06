package com.xwms.core.operation.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import com.xwms.common.exception.BizException;
import com.xwms.core.operation.entity.WorkTask;
import com.xwms.core.operation.mapper.WorkTaskMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 作业任务服务 核心能力： 1. 任务创建/分配/领取/完成/取消 2. PDA任务推送（Redis Pub/Sub实时通知） 3. 任务优先级调度 4. 任务异常处理 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkTaskService {

    private final WorkTaskMapper workTaskMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    /** 创建任务 */
    @Transactional(rollbackFor = Exception.class)
    public WorkTask create(WorkTask task) {
        task.setStatus("PENDING");
        task.setTaskNo("TASK" + System.currentTimeMillis());
        workTaskMapper.insert(task);
        log.info("作业任务创建: taskNo={}, type={}", task.getTaskNo(), task.getTaskType());
        return task;
    }

    /** 领取任务（PDA扫码领取） 防止重复领取：Redis分布式锁 */
    @Transactional(rollbackFor = Exception.class)
    public WorkTask claim(String taskNo, String operator, String deviceId) {
        String lockKey = "task:claim:" + taskNo;
        Boolean locked =
                redisTemplate.opsForValue().setIfAbsent(lockKey, operator, 30, TimeUnit.SECONDS);
        if (!Boolean.TRUE.equals(locked)) {
            throw new BizException("任务已被他人领取: " + taskNo);
        }
        try {
            WorkTask task = getByTaskNo(taskNo);
            if (!"PENDING".equals(task.getStatus())) {
                throw new BizException("任务状态不允许领取: " + task.getStatus());
            }
            task.setOperator(operator);
            task.setDeviceId(deviceId);
            task.setStatus("PROCESSING");
            task.setActualStartTime(LocalDateTime.now());
            workTaskMapper.updateById(task);
            // 实时通知PDA
            redisTemplate.convertAndSend("channel:task:" + deviceId, task);
            return task;
        } finally {
            redisTemplate.delete(lockKey);
        }
    }

    /** 完成任务 */
    @Transactional(rollbackFor = Exception.class)
    public void complete(String taskNo, BigDecimal actualQty, String operator) {
        WorkTask task = getByTaskNo(taskNo);
        if (!"PROCESSING".equals(task.getStatus())) {
            throw new BizException("任务状态不允许完成: " + task.getStatus());
        }
        task.setActualQty(actualQty);
        task.setCompletedTime(LocalDateTime.now());
        task.setStatus("COMPLETED");
        workTaskMapper.updateById(task);
        log.info("作业任务完成: taskNo={}, type={}, qty={}", taskNo, task.getTaskType(), actualQty);
    }

    /** 任务异常 */
    @Transactional(rollbackFor = Exception.class)
    public void reportException(String taskNo, String reason, String operator) {
        WorkTask task = getByTaskNo(taskNo);
        task.setStatus("EXCEPTION");
        task.setExceptionReason(reason);
        workTaskMapper.updateById(task);
        log.warn("作业任务异常: taskNo={}, reason={}", taskNo, reason);
    }

    /** 查询待执行任务（按优先级排序） */
    public List<WorkTask> listPending(String warehouse, String taskType, int limit) {
        return workTaskMapper.selectList(
                new LambdaQueryWrapper<WorkTask>()
                        .eq(WorkTask::getWarehouse, warehouse)
                        .eq(taskType != null, WorkTask::getTaskType, taskType)
                        .eq(WorkTask::getStatus, "PENDING")
                        .orderByDesc(WorkTask::getPriority)
                        .orderByAsc(WorkTask::getPlannedStartTime)
                        .last("LIMIT " + limit + ""));
    }

    private WorkTask getByTaskNo(String taskNo) {
        WorkTask task =
                workTaskMapper.selectOne(
                        new LambdaQueryWrapper<WorkTask>().eq(WorkTask::getTaskNo, taskNo));
        if (task == null) {
            throw new BizException("任务不存在: " + taskNo);
        }
        return task;
    }
}
