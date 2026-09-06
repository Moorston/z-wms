package com.xwms.core.move.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.core.move.entity.*;
import com.xwms.core.move.mapper.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 库存移库管理核心服务 核心能力: 移库单/移库任务/移库执行/移库流水 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MoveService {

    private final MoveOrderMapper orderMapper;
    private final MoveDetailMapper detailMapper;
    private final MoveTaskMapper taskMapper;
    private final MoveLogMapper logMapper;

    private static final AtomicInteger SEQ = new AtomicInteger(0);
    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    // ============================================================

    // 1. 移库单管理
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public MoveOrder createMoveOrder(MoveOrder order, List<MoveDetail> details, String operator) {
        order.setMoveNo(generateMoveNo());
        order.setStatus("DRAFT");
        order.setCreatedBy(operator);
        if (order.getTotalSku() == null) order.setTotalSku(details.size());
        if (order.getTotalQty() == null) {
            BigDecimal totalQty =
                    details.stream()
                            .map(MoveDetail::getPlanQty)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
            order.setTotalQty(totalQty);
        }
        if (order.getMovedQty() == null) order.setMovedQty(BigDecimal.ZERO);
        if (order.getPriority() == null) order.setPriority(5);
        orderMapper.insert(order);

        // 保存明细
        int lineNo = 1;
        for (MoveDetail detail : details) {
            detail.setMoveNo(order.getMoveNo());
            detail.setLineNo(lineNo++);
            if (detail.getMovedQty() == null) detail.setMovedQty(BigDecimal.ZERO);
            if (detail.getStatus() == null) detail.setStatus("PENDING");
            detailMapper.insert(detail);
        }

        log.info(
                "创建移库单: {}, type={}, detailCount={}",
                order.getMoveNo(),
                order.getMoveType(),
                details.size());
        return order;
    }

    /** 下发移库单（生成移库任务） */
    @Transactional(rollbackFor = Exception.class)
    public MoveOrder releaseMoveOrder(String moveNo, String operator) {
        MoveOrder order = orderMapper.selectByMoveNo(moveNo);
        if (order == null) throw new RuntimeException("移库单不存在: " + moveNo);
        if (!"DRAFT".equals(order.getStatus())) {
            throw new RuntimeException("移库单状态不正确: " + order.getStatus());
        }

        List<MoveDetail> details = detailMapper.selectByMoveNo(moveNo);
        for (MoveDetail detail : details) {
            // 为每条明细生成移库任务
            MoveTask task = new MoveTask();
            task.setTaskNo(generateTaskNo());
            task.setMoveNo(moveNo);
            task.setLineNo(detail.getLineNo());
            task.setTaskType("MANUAL");
            task.setFromLocation(detail.getFromLocation());
            task.setToLocation(detail.getToLocation());
            task.setSkuCode(detail.getSkuCode());
            task.setBatchNo(detail.getBatchNo());
            task.setPlanQty(detail.getPlanQty());
            task.setMovedQty(BigDecimal.ZERO);
            task.setStatus("PENDING");
            taskMapper.insert(task);
        }

        order.setStatus("RELEASED");
        orderMapper.updateById(order);

        log.info("下发移库单: {}, taskCount={}", moveNo, details.size());
        return order;
    }

    /** 开始执行移库单 */
    @Transactional(rollbackFor = Exception.class)
    public MoveOrder startMoveOrder(String moveNo, String operator) {
        MoveOrder order = orderMapper.selectByMoveNo(moveNo);
        if (order == null) throw new RuntimeException("移库单不存在: " + moveNo);
        if (!"RELEASED".equals(order.getStatus())) {
            throw new RuntimeException("移库单状态不正确: " + order.getStatus());
        }
        order.setStatus("EXECUTING");
        order.setStartedTime(LocalDateTime.now());
        order.setExecutedBy(operator);
        orderMapper.updateById(order);
        log.info("开始执行移库单: {}", moveNo);
        return order;
    }

    /** 取消移库单 */
    @Transactional(rollbackFor = Exception.class)
    public MoveOrder cancelMoveOrder(String moveNo, String operator) {
        MoveOrder order = orderMapper.selectByMoveNo(moveNo);
        if (order == null) throw new RuntimeException("移库单不存在: " + moveNo);
        if ("COMPLETED".equals(order.getStatus())) {
            throw new RuntimeException("已完成的移库单不能取消");
        }
        order.setStatus("CANCELLED");
        orderMapper.updateById(order);

        // 取消所有未完成任务
        List<MoveTask> tasks = taskMapper.selectByMoveNo(moveNo);
        for (MoveTask task : tasks) {
            if (!"COMPLETED".equals(task.getStatus())) {
                task.setStatus("CANCELLED");
                taskMapper.updateById(task);
            }
        }

        log.info("取消移库单: {}", moveNo);
        return order;
    }

    public Page<MoveOrder> pageMoveOrders(
            Page<MoveOrder> page, String warehouseCode, String moveType, String status) {
        LambdaQueryWrapper<MoveOrder> wrapper = new LambdaQueryWrapper<>();
        if (warehouseCode != null) wrapper.eq(MoveOrder::getWarehouseCode, warehouseCode);
        if (moveType != null) wrapper.eq(MoveOrder::getMoveType, moveType);
        if (status != null) wrapper.eq(MoveOrder::getStatus, status);
        wrapper.orderByDesc(MoveOrder::getCreatedTime);
        return orderMapper.selectPage(page, wrapper);
    }

    public MoveOrder getMoveOrderByNo(String moveNo) {
        return orderMapper.selectByMoveNo(moveNo);
    }

    public List<MoveDetail> getMoveDetails(String moveNo) {
        return detailMapper.selectByMoveNo(moveNo);
    }

    // ============================================================

    // 2. 移库任务管理
    // ============================================================

    /** 分配移库任务 */
    @Transactional(rollbackFor = Exception.class)
    public MoveTask assignTask(String taskNo, String assignee) {
        MoveTask task = taskMapper.selectByTaskNo(taskNo);
        if (task == null) throw new RuntimeException("移库任务不存在: " + taskNo);
        if (!"PENDING".equals(task.getStatus())) {
            throw new RuntimeException("任务状态不正确: " + task.getStatus());
        }
        task.setAssignee(assignee);
        task.setStatus("ASSIGNED");
        task.setAssignedTime(LocalDateTime.now());
        taskMapper.updateById(task);
        log.info("分配移库任务: {} -> {}", taskNo, assignee);
        return task;
    }

    /** 开始执行移库任务 */
    @Transactional(rollbackFor = Exception.class)
    public MoveTask startTask(String taskNo, String operator) {
        MoveTask task = taskMapper.selectByTaskNo(taskNo);
        if (task == null) throw new RuntimeException("移库任务不存在: " + taskNo);
        if (!"ASSIGNED".equals(task.getStatus()) && !"PENDING".equals(task.getStatus())) {
            throw new RuntimeException("任务状态不正确: " + task.getStatus());
        }
        task.setStatus("EXECUTING");
        task.setStartedTime(LocalDateTime.now());
        taskMapper.updateById(task);
        log.info("开始执行移库任务: {}", taskNo);
        return task;
    }

    /** 执行移库（核心：Oracle原子移动库存） */
    @Transactional(rollbackFor = Exception.class)
    public MoveTask executeMove(
            String taskNo,
            BigDecimal qty,
            String fromContainer,
            String toContainer,
            String operator) {
        MoveTask task = taskMapper.selectByTaskNo(taskNo);
        if (task == null) throw new RuntimeException("移库任务不存在: " + taskNo);
        if (!"EXECUTING".equals(task.getStatus())) {
            throw new RuntimeException("任务状态不正确: " + task.getStatus());
        }

        // 检查数量
        BigDecimal remaining = task.getPlanQty().subtract(task.getMovedQty());
        if (qty.compareTo(remaining) > 0) {
            throw new RuntimeException("移库数量超过剩余数量: " + remaining);
        }

        // TODO: 调用库存服务执行原子移库
        // 1. 从来源库位扣减库存
        // 2. 向目标库位增加库存

        // 更新任务数量
        taskMapper.addMovedQty(taskNo, qty);
        task = taskMapper.selectByTaskNo(taskNo);

        // 更新明细数量
        List<MoveDetail> details = detailMapper.selectByMoveNo(task.getMoveNo());
        for (MoveDetail detail : details) {
            if (detail.getLineNo().equals(task.getLineNo())) {
                detailMapper.addMovedQty(detail.getId(), qty);
                break;
            }
        }

        // 更新移库单数量
        orderMapper.addMovedQty(task.getMoveNo(), qty);

        // 记录移库流水
        recordMoveLog(
                task.getMoveNo(),
                taskNo,
                task.getSkuCode(),
                task.getBatchNo(),
                task.getFromLocation(),
                task.getToLocation(),
                fromContainer,
                toContainer,
                qty,
                operator,
                "移库执行");

        // 检查任务是否完成
        if (task.getMovedQty().compareTo(task.getPlanQty()) >= 0) {
            task.setStatus("COMPLETED");
            task.setCompletedTime(LocalDateTime.now());
            taskMapper.updateById(task);
            log.info("移库任务完成: {}", taskNo);
        }

        // 检查移库单是否完成
        checkMoveOrderCompleted(task.getMoveNo());

        log.info("执行移库: task={}, qty={}", taskNo, qty);
        return task;
    }

    /** 检查移库单是否全部完成 */
    private void checkMoveOrderCompleted(String moveNo) {
        MoveOrder order = orderMapper.selectByMoveNo(moveNo);
        if (order == null) return;

        List<MoveTask> tasks = taskMapper.selectByMoveNo(moveNo);
        boolean allCompleted =
                tasks.stream()
                        .allMatch(
                                t ->
                                        "COMPLETED".equals(t.getStatus())
                                                || "CANCELLED".equals(t.getStatus()));
        boolean anyExecuting = tasks.stream().anyMatch(t -> "EXECUTING".equals(t.getStatus()));

        if (allCompleted && !anyExecuting) {
            order.setStatus("COMPLETED");
            order.setCompletedTime(LocalDateTime.now());
            orderMapper.updateById(order);
            log.info("移库单完成: {}", moveNo);
        } else if (order.getMovedQty().compareTo(BigDecimal.ZERO) > 0
                && order.getMovedQty().compareTo(order.getTotalQty()) < 0) {
            order.setStatus("PARTIAL");
            orderMapper.updateById(order);
        }
    }

    public Page<MoveTask> pageTasks(
            Page<MoveTask> page, String moveNo, String assignee, String status) {
        LambdaQueryWrapper<MoveTask> wrapper = new LambdaQueryWrapper<>();
        if (moveNo != null) wrapper.eq(MoveTask::getMoveNo, moveNo);
        if (assignee != null) wrapper.eq(MoveTask::getAssignee, assignee);
        if (status != null) wrapper.eq(MoveTask::getStatus, status);
        wrapper.orderByDesc(MoveTask::getCreatedTime);
        return taskMapper.selectPage(page, wrapper);
    }

    public MoveTask getTaskByNo(String taskNo) {
        return taskMapper.selectByTaskNo(taskNo);
    }

    public List<MoveTask> getMyTasks(String assignee) {
        return taskMapper.selectByAssignee(assignee);
    }

    public List<MoveTask> getTasksByMoveNo(String moveNo) {
        return taskMapper.selectByMoveNo(moveNo);
    }

    // ============================================================

    // 3. 移库流水
    // ============================================================

    public List<MoveLog> getMoveLogsByMoveNo(String moveNo) {
        return logMapper.selectByMoveNo(moveNo);
    }

    public List<MoveLog> getMoveLogsBySku(String skuCode, String batchNo) {
        return logMapper.selectBySku(skuCode, batchNo);
    }

    // ============================================================

    // 工具方法
    // ============================================================

    private void recordMoveLog(
            String moveNo,
            String taskNo,
            String skuCode,
            String batchNo,
            String fromLocation,
            String toLocation,
            String fromContainer,
            String toContainer,
            BigDecimal qty,
            String operator,
            String remark) {
        MoveLog log = new MoveLog();
        log.setLogNo(generateLogNo());
        log.setMoveNo(moveNo);
        log.setTaskNo(taskNo);
        log.setSkuCode(skuCode);
        log.setBatchNo(batchNo);
        log.setFromLocation(fromLocation);
        log.setToLocation(toLocation);
        log.setFromContainer(fromContainer);
        log.setToContainer(toContainer);
        log.setMoveQty(qty);
        log.setOperator(operator);
        log.setActionTime(LocalDateTime.now());
        log.setRemark(remark);
        logMapper.insert(log);
    }

    private String generateMoveNo() {
        return "MV"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }

    private String generateTaskNo() {
        return "MVT"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }

    private String generateLogNo() {
        return "MVL"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }
}
