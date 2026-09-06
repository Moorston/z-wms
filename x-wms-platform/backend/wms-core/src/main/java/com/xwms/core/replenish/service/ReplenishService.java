package com.xwms.core.replenish.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.common.exception.BizException;
import com.xwms.core.inventory.entity.Inventory;
import com.xwms.core.inventory.mapper.InventoryMapper;
import com.xwms.core.replenish.dto.ReplenishCreateRequest;
import com.xwms.core.replenish.entity.ReplenishInTransit;
import com.xwms.core.replenish.entity.ReplenishRule;
import com.xwms.core.replenish.entity.ReplenishTask;
import com.xwms.core.replenish.enums.ReplenishTaskStatus;
import com.xwms.core.replenish.enums.ReplenishType;
import com.xwms.core.replenish.mapper.ReplenishInTransitMapper;
import com.xwms.core.replenish.mapper.ReplenishRuleMapper;
import com.xwms.core.replenish.mapper.ReplenishTaskMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 补货核心服务 包含: 补货触发/补货量计算/源库位查找/任务创建/补货执行/异常处理 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReplenishService {

    private final ReplenishRuleMapper ruleMapper;
    private final ReplenishTaskMapper taskMapper;
    private final ReplenishInTransitMapper inTransitMapper;
    private final InventoryMapper inventoryMapper;

    private static final AtomicInteger TASK_SEQ = new AtomicInteger(0);
    private static final DateTimeFormatter TASK_NO_FMT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    // ============================================================

    // 1. 补货触发 - 库存变动后检查
    // ============================================================

    /**
     * 检查并触发补货 (库存变动后调用)
     *
     * @param sku SKU
     * @param pickLocation 拣货区库位
     * @param currentQty 当前库存
     */
    @Transactional(rollbackFor = Exception.class)
    public ReplenishTask checkAndTrigger(
            String sku,
            String pickLocation,
            BigDecimal currentQty,
            String ownerCode,
            String warehouseCode) {
        // 1. 检查是否已有进行中的补货任务
        int activeCount = taskMapper.countActiveBySku(sku);
        if (activeCount > 0) {
            log.info("SKU{}已有进行中的补货任务({}个), 跳过", sku, activeCount);
            return null;
        }

        // 2. 匹配补货规则
        ReplenishRule rule = matchRule(sku, pickLocation);
        if (rule == null) {
            log.info("SKU{}无补货规则, 跳过", sku);
            return null;
        }

        // 3. 检查是否低于补货点
        if (currentQty.compareTo(rule.getReorderPoint()) > 0) {
            log.debug("SKU{}当前库存{}高于补货点{}, 不触发", sku, currentQty, rule.getReorderPoint());
            return null;
        }

        // 4. 计算补货量
        BigDecimal replenishQty = calculateReplenishQty(rule, currentQty);
        if (replenishQty.compareTo(BigDecimal.ZERO) <= 0) {
            log.info("SKU{}补货量计算为0, 跳过", sku);
            return null;
        }

        // 5. 查找源库位(存储区有库存的库位)
        String fromLocation = findSourceLocation(sku, ownerCode, warehouseCode, replenishQty);
        if (fromLocation == null) {
            log.warn("SKU{}存储区无库存, 无法补货, 触发采购建议", sku);
            // TODO: 发送采购建议事件
            return null;
        }

        // 6. 创建补货任务
        ReplenishCreateRequest request = new ReplenishCreateRequest();
        request.setSku(sku);
        request.setOwnerCode(ownerCode);
        request.setWarehouseCode(warehouseCode);
        request.setFromLocation(fromLocation);
        request.setToLocation(pickLocation);
        request.setPlanQty(replenishQty);
        request.setReplenishType(ReplenishType.NORMAL.getCode());
        request.setPriority(rule.getPriority());
        request.setTriggerSource("AUTO");
        request.setRuleId(rule.getId());

        return createReplenishTask(request);
    }

    /** 紧急补货触发 (拣货缺货时调用) */
    @Transactional(rollbackFor = Exception.class)
    public ReplenishTask triggerUrgent(
            String sku,
            String pickLocation,
            BigDecimal needQty,
            String ownerCode,
            String warehouseCode) {
        String fromLocation = findSourceLocation(sku, ownerCode, warehouseCode, needQty);
        if (fromLocation == null) {
            throw new BizException("存储区无SKU" + sku + "库存, 无法紧急补货");
        }

        ReplenishCreateRequest request = new ReplenishCreateRequest();
        request.setSku(sku);
        request.setOwnerCode(ownerCode);
        request.setWarehouseCode(warehouseCode);
        request.setFromLocation(fromLocation);
        request.setToLocation(pickLocation);
        request.setPlanQty(needQty);
        request.setReplenishType(ReplenishType.URGENT.getCode());
        request.setPriority(1); // 最高优先级
        request.setTriggerSource("SHORTAGE");

        ReplenishTask task = createReplenishTask(request);
        log.warn("紧急补货触发: SKU={}, 数量={}, 任务={}", sku, needQty, task.getTaskNo());
        return task;
    }

    // ============================================================

    // 2. 补货量计算
    // ============================================================

    /** 计算补货量 FIXED: 固定批量 TO_LEVEL: 补到上限 (maxStock - currentQty) EOQ: 经济订货量 (简化为固定批量) */
    private BigDecimal calculateReplenishQty(ReplenishRule rule, BigDecimal currentQty) {
        String type = rule.getReplenishType();
        if ("FIXED".equals(type) || "EOQ".equals(type)) {
            return rule.getReplenishQty() != null ? rule.getReplenishQty() : BigDecimal.ZERO;
        }
        if ("TO_LEVEL".equals(type)) {
            BigDecimal max = rule.getMaxStock() != null ? rule.getMaxStock() : BigDecimal.ZERO;
            return max.subtract(currentQty).max(BigDecimal.ZERO);
        }
        // 默认固定批量
        return rule.getReplenishQty() != null ? rule.getReplenishQty() : BigDecimal.ZERO;
    }

    // ============================================================

    // 3. 源库位查找 (存储区有库存的库位, 按FIFO/FEFO)
    // ============================================================

    private String findSourceLocation(
            String sku, String ownerCode, String warehouseCode, BigDecimal needQty) {
        LambdaQueryWrapper<Inventory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Inventory::getSkuCode, sku)
                .eq(Inventory::getOwnerCodeCol, ownerCode)
                .eq(Inventory::getWarehouseCode, warehouseCode)
                .gt(Inventory::getAvailableQty, BigDecimal.ZERO)
                .orderByAsc(Inventory::getProductionDate) // FIFO
                .last("LIMIT 1");
        Inventory inv = inventoryMapper.selectOne(wrapper);
        return inv != null ? inv.getLocationCode() : null;
    }

    // ============================================================

    // 4. 创建补货任务
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public ReplenishTask createReplenishTask(ReplenishCreateRequest request) {
        String taskNo = generateTaskNo();

        ReplenishTask task = new ReplenishTask();
        task.setTaskNo(taskNo);
        task.setRuleId(request.getRuleId());
        task.setSku(request.getSku());
        task.setBarcode(request.getBarcode());
        task.setProductName(request.getProductName());
        task.setOwnerCode(request.getOwnerCode());
        task.setWarehouseCode(request.getWarehouseCode());
        task.setFromLocation(request.getFromLocation());
        task.setToLocation(request.getToLocation());
        task.setPlanQty(request.getPlanQty());
        task.setActualQty(BigDecimal.ZERO);
        task.setReplenishType(request.getReplenishType());
        task.setPriority(request.getPriority() != null ? request.getPriority() : 5);
        task.setStatus(ReplenishTaskStatus.PENDING.getCode());
        task.setTriggerSource(request.getTriggerSource());
        task.setOwnerCodeCol(request.getOwnerCode());
        task.setWarehouseCodeCol(request.getWarehouseCode());

        taskMapper.insert(task);
        log.info(
                "创建补货任务: {}, SKU={}, 数量={}, 类型={}",
                taskNo,
                request.getSku(),
                request.getPlanQty(),
                request.getReplenishType());
        return task;
    }

    // ============================================================

    // 5. 补货执行 - 拣货
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public ReplenishTask startPick(Long taskId, String assignee) {
        ReplenishTask task = getTask(taskId);
        if (!ReplenishTaskStatus.PENDING.getCode().equals(task.getStatus())
                && !ReplenishTaskStatus.ASSIGNED.getCode().equals(task.getStatus())) {
            throw new BizException("补货任务状态不允许拣货: " + task.getStatus());
        }

        task.setStatus(ReplenishTaskStatus.PICKING.getCode());
        task.setAssignee(assignee);
        task.setAssignTime(LocalDateTime.now());
        task.setStartTime(LocalDateTime.now());
        taskMapper.updateById(task);

        // 创建在途库存记录 (源库位库存预占)
        ReplenishInTransit transit = new ReplenishInTransit();
        transit.setTaskId(task.getId());
        transit.setSku(task.getSku());
        transit.setBatchNo(task.getBatchNo());
        transit.setFromLocation(task.getFromLocation());
        transit.setToLocation(task.getToLocation());
        transit.setQty(task.getPlanQty());
        transit.setStatus("IN_TRANSIT");
        transit.setOwnerCode(task.getOwnerCode());
        transit.setWarehouseCode(task.getWarehouseCode());
        inTransitMapper.insert(transit);

        // TODO: Oracle原子扣减源库位可用库存, 增加在途
        log.info("补货拣货开始: {}, 作业员={}", task.getTaskNo(), assignee);
        return task;
    }

    // ============================================================

    // 6. 补货执行 - 上架确认
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public ReplenishTask confirmPutaway(Long taskId, BigDecimal actualQty) {
        ReplenishTask task = getTask(taskId);
        if (!ReplenishTaskStatus.PICKING.getCode().equals(task.getStatus())
                && !ReplenishTaskStatus.PICKED.getCode().equals(task.getStatus())) {
            throw new BizException("补货任务状态不允许上架: " + task.getStatus());
        }

        task.setActualQty(actualQty);
        task.setStatus(ReplenishTaskStatus.COMPLETED.getCode());
        task.setFinishTime(LocalDateTime.now());
        taskMapper.updateById(task);

        // 更新在途状态
        LambdaQueryWrapper<ReplenishInTransit> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReplenishInTransit::getTaskId, taskId);
        ReplenishInTransit transit = inTransitMapper.selectOne(wrapper);
        if (transit != null) {
            transit.setStatus("ARRIVED");
            transit.setArrivedTime(LocalDateTime.now());
            inTransitMapper.updateById(transit);
        }

        // TODO: Oracle原子更新: 源库位扣减, 目标库位增加, 清除在途
        // 数量差异处理
        if (actualQty.compareTo(task.getPlanQty()) != 0) {
            log.warn("补货数量差异: 计划={}, 实际={}", task.getPlanQty(), actualQty);
            task.setExceptionReason("补货数量差异: 计划" + task.getPlanQty() + "实际" + actualQty);
            taskMapper.updateById(task);
        }

        log.info("补货完成: {}, 实际数量={}", task.getTaskNo(), actualQty);
        return task;
    }

    // ============================================================

    // 7. 异常处理
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public ReplenishTask handleException(Long taskId, String reason) {
        ReplenishTask task = getTask(taskId);
        task.setStatus(ReplenishTaskStatus.EXCEPTION.getCode());
        task.setExceptionReason(reason);
        taskMapper.updateById(task);

        // 释放在途库存
        LambdaQueryWrapper<ReplenishInTransit> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReplenishInTransit::getTaskId, taskId);
        ReplenishInTransit transit = inTransitMapper.selectOne(wrapper);
        if (transit != null) {
            transit.setStatus("CANCELLED");
            inTransitMapper.updateById(transit);
        }

        log.warn("补货异常: {}, 原因={}", task.getTaskNo(), reason);
        return task;
    }

    @Transactional(rollbackFor = Exception.class)
    public ReplenishTask cancelTask(Long taskId) {
        ReplenishTask task = getTask(taskId);
        if (ReplenishTaskStatus.COMPLETED.getCode().equals(task.getStatus())) {
            throw new BizException("已完成的补货任务不能取消");
        }
        task.setStatus(ReplenishTaskStatus.CANCELLED.getCode());
        taskMapper.updateById(task);

        // 释放在途
        LambdaQueryWrapper<ReplenishInTransit> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReplenishInTransit::getTaskId, taskId);
        ReplenishInTransit transit = inTransitMapper.selectOne(wrapper);
        if (transit != null && "IN_TRANSIT".equals(transit.getStatus())) {
            transit.setStatus("CANCELLED");
            inTransitMapper.updateById(transit);
        }

        log.info("补货任务取消: {}", task.getTaskNo());
        return task;
    }

    // ============================================================

    // 8. 定时补货检查 (PowerJob调用)
    // ============================================================

    /** 定时检查所有拣货区库存, 批量触发补货 */
    public int batchCheckReplenish(String warehouseCode) {
        List<ReplenishTask> pending = taskMapper.selectPendingTasks(warehouseCode);
        log.info("定时补货检查: 仓库={}, 当前待处理任务={}", warehouseCode, pending.size());
        // TODO: 查询所有拣货区库存, 低于补货点的触发补货
        return pending.size();
    }

    // ============================================================

    // 9. 查询
    // ============================================================

    public ReplenishTask getTask(Long id) {
        ReplenishTask task = taskMapper.selectById(id);
        if (task == null) throw new BizException("补货任务不存在: " + id);
        return task;
    }

    public Page<ReplenishTask> pageTasks(
            Page<ReplenishTask> page, String status, String sku, String type) {
        LambdaQueryWrapper<ReplenishTask> wrapper = new LambdaQueryWrapper<>();
        if (status != null) wrapper.eq(ReplenishTask::getStatus, status);
        if (sku != null) wrapper.like(ReplenishTask::getSku, sku);
        if (type != null) wrapper.eq(ReplenishTask::getReplenishType, type);
        wrapper.orderByAsc(ReplenishTask::getPriority).orderByDesc(ReplenishTask::getCreatedAt);
        return taskMapper.selectPage(page, wrapper);
    }

    public List<ReplenishTask> getPendingTasks(String warehouseCode) {
        return taskMapper.selectPendingTasks(warehouseCode);
    }

    // ============================================================

    // 工具方法
    // ============================================================

    private ReplenishRule matchRule(String sku, String pickArea) {
        if (sku != null && pickArea != null) {
            ReplenishRule rule = ruleMapper.matchRule(sku, pickArea);
            if (rule != null) return rule;
        }
        if (sku != null) {
            return ruleMapper.matchSkuRule(sku);
        }
        return null;
    }

    private String generateTaskNo() {
        return "RP"
                + LocalDateTime.now().format(TASK_NO_FMT)
                + String.format("%03d", TASK_SEQ.incrementAndGet() % 1000);
    }
}
