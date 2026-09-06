package com.xwms.core.putaway.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.common.exception.BizException;
import com.xwms.common.lock.DistributedLock;
import com.xwms.core.inventory.service.InventoryService;
import com.xwms.core.putaway.entity.*;
import com.xwms.core.putaway.enums.PutawayStatus;
import com.xwms.core.putaway.event.PutawayEvent;
import com.xwms.core.putaway.event.PutawayEventPublisher;
import com.xwms.core.putaway.mapper.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 上架任务管理服务（Sprint 3） 核心能力：任务派发/领取/释放、上架确认（库存扣减）、人工覆盖、异常处理、批量生成
 * 状态机：PENDING→ASSIGNED→CLAIMED→PUTAWAYING→PARTIAL→COMPLETED
 * PENDING→ASSIGNED→CLAIMED→EXCEPTION→RESOLVED 任意状态→CANCELLED（未开始上架时）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PutawayTaskService {

    private final PutawayTaskMapper taskMapper;
    private final PutawayTaskDetailMapper taskDetailMapper;
    private final PutawayRecordMapper recordMapper;
    private final PutawayReasonCodeMapper reasonCodeMapper;
    private final PutawayRecommendLogMapper recommendLogMapper;
    private final PutawayExceptionLogMapper exceptionLogMapper;
    private final InventoryService inventoryService;
    private final PutawayEventPublisher eventPublisher;
    private final PutawayPerformanceService performanceService;
    private final DistributedLock distributedLock;

    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final String LOCK_PUTAWAY_CONFIRM = "putaway:confirm:";

    // ============================================================
    // 1. 任务派发
    // ============================================================

    /** 派发上架任务给指定人员 状态流转：PENDING → ASSIGNED */
    @Transactional(rollbackFor = Exception.class)
    public PutawayTask assignTask(
            String taskNo, String assignee, String assigner, String workZone) {
        log.info("派发上架任务: taskNo={}, assignee={}, workZone={}", taskNo, assignee, workZone);

        PutawayTask task = taskMapper.selectByTaskNo(taskNo);
        if (task == null) throw new BizException("上架任务不存在: " + taskNo);

        if (!PutawayStatus.PENDING.getCode().equals(task.getStatus())) {
            throw new BizException("任务状态不允许派发: 当前状态=" + task.getStatus());
        }

        task.setAssignee(assignee);
        task.setAssigner(assigner);
        task.setAssignTime(LocalDateTime.now());
        if (workZone != null) task.setWorkZone(workZone);
        task.setStatus(PutawayStatus.ASSIGNED.getCode());
        task.setUpdatedBy(assigner);
        task.setUpdatedTime(LocalDateTime.now());
        taskMapper.updateById(task);

        log.info("派发上架任务完成: taskNo={}, assignee={}", taskNo, assignee);
        return task;
    }

    /** 批量派发上架任务 */
    @Transactional(rollbackFor = Exception.class)
    public List<PutawayTask> batchAssignTasks(
            List<String> taskNos, String assignee, String assigner, String workZone) {
        List<PutawayTask> results = new ArrayList<>();
        for (String taskNo : taskNos) {
            try {
                results.add(assignTask(taskNo, assignee, assigner, workZone));
            } catch (Exception e) {
                log.warn("批量派发任务失败: taskNo={}, error={}", taskNo, e.getMessage());
            }
        }
        return results;
    }

    // ============================================================
    // 2. 任务领取
    // ============================================================

    /** 领取上架任务 状态流转：PENDING/ASSIGNED → CLAIMED */
    @Transactional(rollbackFor = Exception.class)
    public PutawayTask claimTask(String taskNo, String operator) {
        log.info("领取上架任务: taskNo={}, operator={}", taskNo, operator);

        PutawayTask task = taskMapper.selectByTaskNo(taskNo);
        if (task == null) throw new BizException("上架任务不存在: " + taskNo);

        String status = task.getStatus();
        if (!PutawayStatus.PENDING.getCode().equals(status)
                && !PutawayStatus.ASSIGNED.getCode().equals(status)) {
            throw new BizException("任务状态不允许领取: 当前状态=" + status);
        }

        // 如果已被他人领取，不允许重复领取
        if (task.getAssignee() != null
                && !task.getAssignee().equals(operator)
                && PutawayStatus.CLAIMED.getCode().equals(status)) {
            throw new BizException("任务已被他人领取: assignee=" + task.getAssignee());
        }

        task.setAssignee(operator);
        task.setClaimTime(LocalDateTime.now());
        task.setStartTime(LocalDateTime.now());
        task.setStatus(PutawayStatus.CLAIMED.getCode());
        task.setUpdatedBy(operator);
        task.setUpdatedTime(LocalDateTime.now());
        taskMapper.updateById(task);

        log.info("领取上架任务完成: taskNo={}, operator={}", taskNo, operator);
        return task;
    }

    /** 获取待领取任务列表（按工作区和优先级排序） */
    public Page<PutawayTask> getPendingTasks(
            Page<PutawayTask> page, String workZone, String warehouseCode, Integer priority) {
        LambdaQueryWrapper<PutawayTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(
                PutawayTask::getStatus,
                PutawayStatus.PENDING.getCode(),
                PutawayStatus.ASSIGNED.getCode());
        if (workZone != null) wrapper.eq(PutawayTask::getWorkZone, workZone);
        if (warehouseCode != null) wrapper.eq(PutawayTask::getWarehouseCode, warehouseCode);
        if (priority != null) wrapper.eq(PutawayTask::getPriority, priority);
        wrapper.orderByAsc(PutawayTask::getPriority).orderByAsc(PutawayTask::getCreatedTime);
        return taskMapper.selectPage(page, wrapper);
    }

    // ============================================================
    // 3. 任务释放
    // ============================================================

    /** 释放上架任务（退回待领取池） 状态流转：ASSIGNED/CLAIMED → PENDING */
    @Transactional(rollbackFor = Exception.class)
    public PutawayTask releaseTask(String taskNo, String reason, String operator) {
        log.info("释放上架任务: taskNo={}, reason={}, operator={}", taskNo, reason, operator);

        PutawayTask task = taskMapper.selectByTaskNo(taskNo);
        if (task == null) throw new BizException("上架任务不存在: " + taskNo);

        String status = task.getStatus();
        if (!PutawayStatus.ASSIGNED.getCode().equals(status)
                && !PutawayStatus.CLAIMED.getCode().equals(status)) {
            throw new BizException("任务状态不允许释放: 当前状态=" + status);
        }

        // 如果已开始上架，不允许释放
        if (task.getPutawayQty() != null && task.getPutawayQty().compareTo(BigDecimal.ZERO) > 0) {
            throw new BizException("已开始上架的任务不允许释放: taskNo=" + taskNo);
        }

        task.setAssignee(null);
        task.setClaimTime(null);
        task.setStartTime(null);
        task.setStatus(PutawayStatus.PENDING.getCode());
        task.setRemark(reason);
        task.setUpdatedBy(operator);
        task.setUpdatedTime(LocalDateTime.now());
        taskMapper.updateById(task);

        log.info("释放上架任务完成: taskNo={}", taskNo);
        return task;
    }

    // ============================================================
    // 4. 上架确认（含库存扣减）
    // ============================================================

    /**
     * 上架确认（核心方法） 1. 校验任务和明细状态 2. 扣减源库位（收货过渡库位）库存 3. 增加目标库位库存 4. 创建上架记录 5. 更新任务明细和任务状态 状态流转：CLAIMED
     * → PUTAWAYING → PARTIAL/COMPLETED
     */
    @Transactional(rollbackFor = Exception.class)
    public PutawayRecord confirmPutaway(
            String taskNo,
            String detailNo,
            String targetLocation,
            BigDecimal putawayQty,
            String batchNo,
            String operator) {
        // 0. 参数校验
        validatePutawayParams(taskNo, detailNo, targetLocation, putawayQty, operator);

        String lockKey = LOCK_PUTAWAY_CONFIRM + taskNo + ":" + detailNo;
        if (!distributedLock.tryLock(lockKey, 30)) {
            throw new BizException("任务正在处理中，请稍后重试: " + taskNo);
        }

        try {
            log.info(
                    "上架确认: taskNo={}, detailNo={}, location={}, qty={}",
                    taskNo,
                    detailNo,
                    targetLocation,
                    putawayQty);

            // 1. 校验任务
            PutawayTask task = taskMapper.selectByTaskNo(taskNo);
            if (task == null) throw new BizException("上架任务不存在: " + taskNo);

            String taskStatus = task.getStatus();
            if (!PutawayStatus.CLAIMED.getCode().equals(taskStatus)
                    && !PutawayStatus.PUTAWAYING.getCode().equals(taskStatus)
                    && !PutawayStatus.PARTIAL.getCode().equals(taskStatus)) {
                throw new BizException("任务状态不允许上架: 当前状态=" + taskStatus);
            }

            // 2. 校验明细（加锁后重新查询，防止脏读）
            PutawayTaskDetail detail = taskDetailMapper.selectByDetailNo(detailNo);
            if (detail == null) throw new BizException("上架任务明细不存在: " + detailNo);
            if (!taskNo.equals(detail.getTaskNo())) {
                throw new BizException("明细不属于该任务: taskNo=" + taskNo + ", detailNo=" + detailNo);
            }

            BigDecimal detailPutawayQty =
                    detail.getPutawayQty() != null ? detail.getPutawayQty() : BigDecimal.ZERO;
            BigDecimal remainingQty = detail.getExpectedQty().subtract(detailPutawayQty);
            if (putawayQty.compareTo(remainingQty) > 0) {
                throw new BizException(
                        String.format(
                                "上架数量超过剩余数量: 明细=%s, 剩余=%s, 请求=%s",
                                detailNo, remainingQty, putawayQty));
            }

            // 3. 校验库位（如果不允许修改库位）
            if ("N".equals(task.getAllowLocationChange())
                    && !targetLocation.equals(detail.getRecommendLocation())) {
                throw new BizException("不允许修改推荐库位: 推荐=" + detail.getRecommendLocation());
            }

            // 4. 库存操作（核心）
            String sourceLocation = detail.getSourceLocation();
            String skuCode = detail.getSkuCode();
            String ownerCode = task.getOwnerCode();
            String warehouseCode = task.getWarehouseCode();
            String actualBatchNo =
                    batchNo != null && !batchNo.trim().isEmpty() ? batchNo : detail.getBatchNo();
            if (actualBatchNo == null || actualBatchNo.trim().isEmpty()) {
                throw new BizException("批次号不能为空: detailNo=" + detailNo);
            }

            // 4.1 扣减源库位库存（收货过渡库位）
            if (sourceLocation != null && !sourceLocation.equals(targetLocation)) {
                inventoryService.deductInventory(
                        warehouseCode,
                        sourceLocation,
                        skuCode,
                        actualBatchNo,
                        ownerCode,
                        putawayQty,
                        "PUTAWAY",
                        taskNo,
                        operator);
            }

            // 4.2 增加目标库位库存
            inventoryService.addInventory(
                    warehouseCode,
                    targetLocation,
                    skuCode,
                    actualBatchNo,
                    ownerCode,
                    putawayQty,
                    "PUTAWAY",
                    taskNo,
                    operator);

            // 5. 创建上架记录
            PutawayRecord record =
                    buildPutawayRecord(
                            task, detail, putawayQty, targetLocation, actualBatchNo, operator);
            recordMapper.insert(record);

            // 6. 更新任务明细
            updateDetailAfterPutaway(detail, putawayQty, targetLocation, operator);

            // 7. 更新任务
            updateTaskAfterPutaway(task, putawayQty, operator);

            // 8. 事务提交后发布事件（避免事件先于事务可见）
            PutawayEvent event =
                    buildPutawayEvent(task, detail, putawayQty, targetLocation, actualBatchNo);
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            try {
                                eventPublisher.publishTaskCompleted(event);
                            } catch (Exception e) {
                                log.warn("上架完成事件发布失败: taskNo={}", taskNo, e);
                            }
                        }
                    });

            log.info(
                    "上架确认完成: taskNo={}, detailNo={}, qty={}, 源库位={}, 目标库位={}",
                    taskNo,
                    detailNo,
                    putawayQty,
                    sourceLocation,
                    targetLocation);
            return record;
        } finally {
            distributedLock.unlock(lockKey);
        }
    }

    private void validatePutawayParams(
            String taskNo,
            String detailNo,
            String targetLocation,
            BigDecimal putawayQty,
            String operator) {
        if (taskNo == null || taskNo.trim().isEmpty()) {
            throw new BizException("任务号不能为空");
        }
        if (detailNo == null || detailNo.trim().isEmpty()) {
            throw new BizException("明细号不能为空");
        }
        if (targetLocation == null || targetLocation.trim().isEmpty()) {
            throw new BizException("目标库位不能为空");
        }
        if (putawayQty == null || putawayQty.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException("上架数量必须大于0");
        }
        if (operator == null || operator.trim().isEmpty()) {
            throw new BizException("操作人不能为空");
        }
    }

    private PutawayRecord buildPutawayRecord(
            PutawayTask task,
            PutawayTaskDetail detail,
            BigDecimal putawayQty,
            String targetLocation,
            String actualBatchNo,
            String operator) {
        PutawayRecord record = new PutawayRecord();
        record.setRecordNo(generateRecordNo());
        record.setTaskNo(task.getTaskNo());
        record.setInboundNo(task.getInboundNo());
        record.setAsnNo(task.getAsnNo());
        record.setTaskDetailNo(detail.getDetailNo());
        record.setInboundDetailNo(detail.getInboundDetailNo());
        record.setSkuCode(detail.getSkuCode());
        record.setSkuName(detail.getSkuName());
        record.setBarcode(detail.getBarcode());
        record.setBatchNo(actualBatchNo);
        record.setPutawayQty(putawayQty);
        record.setUnit(detail.getUnit());
        record.setSourceLocation(detail.getSourceLocation());
        record.setRecommendLocation(detail.getRecommendLocation());
        record.setTargetLocation(targetLocation);
        record.setPutawayType(task.getPutawayType());
        record.setPutawayStrategy(task.getPutawayStrategy());
        record.setUseSystemRecommend(
                targetLocation.equals(detail.getRecommendLocation()) ? "Y" : "N");
        record.setOperator(operator);
        record.setPutawayTime(LocalDateTime.now());
        return record;
    }

    private void updateDetailAfterPutaway(
            PutawayTaskDetail detail,
            BigDecimal putawayQty,
            String targetLocation,
            String operator) {
        BigDecimal newPutawayQty =
                (detail.getPutawayQty() != null ? detail.getPutawayQty() : BigDecimal.ZERO)
                        .add(putawayQty);
        detail.setPutawayQty(newPutawayQty);
        detail.setActualLocation(targetLocation);
        detail.setDifferenceQty(detail.getExpectedQty().subtract(newPutawayQty));
        if (newPutawayQty.compareTo(detail.getExpectedQty()) >= 0) {
            detail.setStatus(PutawayStatus.COMPLETED.getCode());
        } else {
            detail.setStatus(PutawayStatus.PARTIAL.getCode());
        }
        detail.setUpdatedBy(operator);
        detail.setUpdatedTime(LocalDateTime.now());
        taskDetailMapper.updateById(detail);
    }

    private void updateTaskAfterPutaway(PutawayTask task, BigDecimal putawayQty, String operator) {
        BigDecimal taskNewPutawayQty =
                (task.getPutawayQty() != null ? task.getPutawayQty() : BigDecimal.ZERO)
                        .add(putawayQty);
        task.setPutawayQty(taskNewPutawayQty);
        task.setDifferenceQty(task.getExpectedQty().subtract(taskNewPutawayQty));
        task.setOperator(operator);
        if (taskNewPutawayQty.compareTo(BigDecimal.ZERO) == 0) {
            task.setStatus(PutawayStatus.PENDING.getCode());
        } else if (taskNewPutawayQty.compareTo(task.getExpectedQty()) >= 0) {
            task.setStatus(PutawayStatus.COMPLETED.getCode());
            task.setCompleteTime(LocalDateTime.now());
        } else {
            task.setStatus(PutawayStatus.PARTIAL.getCode());
        }
        task.setUpdatedBy(operator);
        task.setUpdatedTime(LocalDateTime.now());
        taskMapper.updateById(task);
    }

    // ============================================================
    // 5. 人工覆盖推荐库位
    // ============================================================

    /** 人工覆盖推荐库位 记录覆盖原因代码，用于后续分析推荐命中率 */
    @Transactional(rollbackFor = Exception.class)
    public PutawayTaskDetail overrideLocation(
            String detailNo, String newLocation, String reasonCode, String operator) {
        log.info(
                "人工覆盖推荐库位: detailNo={}, newLocation={}, reasonCode={}",
                detailNo,
                newLocation,
                reasonCode);

        PutawayTaskDetail detail = taskDetailMapper.selectByDetailNo(detailNo);
        if (detail == null) throw new BizException("上架任务明细不存在: " + detailNo);

        String originalLocation = detail.getRecommendLocation();

        // 校验原因代码
        if (reasonCode != null) {
            PutawayReasonCode reason = reasonCodeMapper.selectByCode(reasonCode);
            if (reason == null) {
                throw new BizException("原因代码不存在: " + reasonCode);
            }
        }

        // 更新明细推荐库位
        detail.setRecommendLocation(newLocation);
        detail.setUpdatedBy(operator);
        detail.setUpdatedTime(LocalDateTime.now());
        taskDetailMapper.updateById(detail);

        // 记录例外日志
        PutawayExceptionLog exceptionLog = new PutawayExceptionLog();
        exceptionLog.setLogNo(generateExceptionLogNo());
        exceptionLog.setTaskNo(detail.getTaskNo());
        exceptionLog.setDetailNo(detailNo);
        exceptionLog.setSkuCode(detail.getSkuCode());
        exceptionLog.setExceptionType("OVERRIDE");
        exceptionLog.setReasonCode(reasonCode);
        exceptionLog.setReasonDesc("人工覆盖推荐库位");
        exceptionLog.setOriginalLocation(originalLocation);
        exceptionLog.setActualLocation(newLocation);
        exceptionLog.setOperator(operator);
        exceptionLog.setOperateTime(LocalDateTime.now());
        exceptionLog.setHandleStatus("RESOLVED");
        exceptionLog.setHandler(operator);
        exceptionLog.setHandleTime(LocalDateTime.now());
        exceptionLog.setHandleResult("人工覆盖为: " + newLocation);
        exceptionLogMapper.insert(exceptionLog);

        log.info(
                "人工覆盖推荐库位完成: detailNo={}, 原库位={}, 新库位={}", detailNo, originalLocation, newLocation);

        // 发布人工覆盖事件（异步）
        try {
            PutawayTask task = taskMapper.selectByTaskNo(detail.getTaskNo());
            PutawayEvent event =
                    eventPublisher.buildEvent(
                            detail.getTaskNo(),
                            task != null ? task.getWarehouseCode() : null,
                            operator);
            event.setDetailNo(detailNo);
            event.setSkuCode(detail.getSkuCode());
            event.setSourceLocation(originalLocation);
            event.setTargetLocation(newLocation);
            event.setReasonCode(reasonCode);
            eventPublisher.publishTaskOverride(event);
        } catch (Exception e) {
            log.warn("人工覆盖事件发布失败: detailNo={}", detailNo);
        }

        return detail;
    }

    // ============================================================
    // 6. 异常处理
    // ============================================================

    /** 上报上架异常 状态流转：CLAIMED/PUTAWAYING → EXCEPTION */
    @Transactional(rollbackFor = Exception.class)
    public PutawayExceptionLog reportException(
            String taskNo,
            String detailNo,
            String exceptionType,
            String reasonCode,
            String reasonDesc,
            String operator) {
        log.info(
                "上报上架异常: taskNo={}, detailNo={}, type={}, reason={}",
                taskNo,
                detailNo,
                exceptionType,
                reasonCode);

        PutawayTask task = taskMapper.selectByTaskNo(taskNo);
        if (task == null) throw new BizException("上架任务不存在: " + taskNo);

        // 更新任务状态为异常
        task.setStatus(PutawayStatus.EXCEPTION.getCode());
        task.setExceptionReasonCode(reasonCode);
        task.setExceptionRemark(reasonDesc);
        task.setUpdatedBy(operator);
        task.setUpdatedTime(LocalDateTime.now());
        taskMapper.updateById(task);

        // 创建例外日志
        PutawayExceptionLog exceptionLog = new PutawayExceptionLog();
        exceptionLog.setLogNo(generateExceptionLogNo());
        exceptionLog.setTaskNo(taskNo);
        exceptionLog.setDetailNo(detailNo);
        exceptionLog.setWarehouseCode(task.getWarehouseCode());
        exceptionLog.setSkuCode(
                taskDetailMapper.selectByDetailNo(detailNo) != null
                        ? taskDetailMapper.selectByDetailNo(detailNo).getSkuCode()
                        : null);
        exceptionLog.setExceptionType(exceptionType);
        exceptionLog.setReasonCode(reasonCode);
        exceptionLog.setReasonDesc(reasonDesc);
        exceptionLog.setOperator(operator);
        exceptionLog.setOperateTime(LocalDateTime.now());
        exceptionLog.setHandleStatus("PENDING");
        exceptionLogMapper.insert(exceptionLog);

        log.info("上报上架异常完成: taskNo={}, type={}", taskNo, exceptionType);

        // 发布上架异常事件（异步）
        try {
            PutawayEvent event =
                    eventPublisher.buildEvent(taskNo, task.getWarehouseCode(), operator);
            event.setDetailNo(detailNo);
            event.setExceptionType(exceptionType);
            event.setReasonCode(reasonCode);
            event.setRemark(reasonDesc);
            eventPublisher.publishTaskException(event);
        } catch (Exception e) {
            log.warn("上架异常事件发布失败: taskNo={}", taskNo);
        }

        return exceptionLog;
    }

    /** 解决上架异常 状态流转：EXCEPTION → CLAIMED/PUTAWAYING */
    @Transactional(rollbackFor = Exception.class)
    public PutawayExceptionLog resolveException(
            Long exceptionLogId, String handleResult, String handler, String resumeStatus) {
        log.info(
                "解决上架异常: logId={}, handler={}, resumeStatus={}",
                exceptionLogId,
                handler,
                resumeStatus);

        PutawayExceptionLog exceptionLog = exceptionLogMapper.selectById(exceptionLogId);
        if (exceptionLog == null) throw new BizException("例外日志不存在: " + exceptionLogId);

        exceptionLog.setHandleStatus("RESOLVED");
        exceptionLog.setHandler(handler);
        exceptionLog.setHandleTime(LocalDateTime.now());
        exceptionLog.setHandleResult(handleResult);
        exceptionLogMapper.updateById(exceptionLog);

        // 恢复任务状态
        PutawayTask task = taskMapper.selectByTaskNo(exceptionLog.getTaskNo());
        if (task != null) {
            task.setStatus(resumeStatus != null ? resumeStatus : PutawayStatus.CLAIMED.getCode());
            task.setUpdatedBy(handler);
            task.setUpdatedTime(LocalDateTime.now());
            taskMapper.updateById(task);
        }

        log.info("解决上架异常完成: logId={}", exceptionLogId);
        return exceptionLog;
    }

    // ============================================================
    // 7. 原因代码管理
    // ============================================================

    public List<PutawayReasonCode> getReasonCodesByType(String reasonType) {
        return reasonCodeMapper.selectByType(reasonType);
    }

    // ============================================================
    // 8. 推荐日志查询
    // ============================================================

    public List<PutawayRecommendLog> getRecommendLogsByTask(String taskNo) {
        return recommendLogMapper.selectByTaskNo(taskNo);
    }

    // ============================================================
    // 9. 例外日志查询
    // ============================================================

    public List<PutawayExceptionLog> getPendingExceptions() {
        return exceptionLogMapper.selectPending();
    }

    public List<PutawayExceptionLog> getExceptionLogsByTask(String taskNo) {
        return exceptionLogMapper.selectByTaskNo(taskNo);
    }

    // ============================================================
    // 10. 编号生成
    // ============================================================

    private String generateRecordNo() {
        return "PAR"
                + LocalDateTime.now().format(NO_FMT)
                + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    private String generateExceptionLogNo() {
        return "PEL"
                + LocalDateTime.now().format(NO_FMT)
                + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    private PutawayEvent buildPutawayEvent(
            PutawayTask task,
            PutawayTaskDetail detail,
            BigDecimal putawayQty,
            String targetLocation,
            String actualBatchNo) {
        return PutawayEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventTime(LocalDateTime.now())
                .taskNo(task.getTaskNo())
                .detailNo(detail.getDetailNo())
                .warehouseCode(task.getWarehouseCode())
                .ownerCode(task.getOwnerCode())
                .skuCode(detail.getSkuCode())
                .batchNo(actualBatchNo)
                .sourceLocation(detail.getSourceLocation())
                .targetLocation(targetLocation)
                .putawayQty(putawayQty)
                .operator(task.getOperator())
                .sourceService("wms-core")
                .build();
    }
}
