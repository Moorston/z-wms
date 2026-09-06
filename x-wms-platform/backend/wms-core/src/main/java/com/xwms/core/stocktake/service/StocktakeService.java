package com.xwms.core.stocktake.service;

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
import com.xwms.core.stocktake.dto.StocktakeCountRequest;
import com.xwms.core.stocktake.dto.StocktakeCreateRequest;
import com.xwms.core.stocktake.entity.*;
import com.xwms.core.stocktake.enums.StocktakeTaskStatus;
import com.xwms.core.stocktake.mapper.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 盘点核心服务 包含: 创建盘点/生成明细/执行盘点/复盘/差异处理/库存调整 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StocktakeService {

    private final StocktakeTaskMapper taskMapper;
    private final StocktakeItemMapper itemMapper;
    private final InventoryMapper inventoryMapper;

    private static final AtomicInteger TASK_SEQ = new AtomicInteger(0);
    private static final AtomicInteger ADJUST_SEQ = new AtomicInteger(0);
    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    // 差异审批阈值(超过此数量需审批)
    private static final BigDecimal APPROVE_THRESHOLD = new BigDecimal("10");

    // ============================================================
    // 1. 创建盘点任务
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public StocktakeTask createStocktake(StocktakeCreateRequest request) {
        String taskNo = generateTaskNo();

        StocktakeTask task = new StocktakeTask();
        task.setTaskNo(taskNo);
        task.setTaskName(request.getTaskName());
        task.setStocktakeType(request.getStocktakeType());
        task.setWarehouseCode(request.getWarehouseCode());
        task.setAreaCode(request.getAreaCode());
        task.setLocationFrom(request.getLocationFrom());
        task.setLocationTo(request.getLocationTo());
        task.setSkuList(request.getSkuList());
        task.setOwnerCode(request.getOwnerCode());
        task.setBatchNo(request.getBatchNo());
        task.setAbcClass(request.getAbcClass());
        task.setPlanStartTime(request.getPlanStartTime());
        task.setPlanEndTime(request.getPlanEndTime());
        task.setStatus(StocktakeTaskStatus.DRAFT.getCode());
        task.setFreezeFlag(request.getFreezeFlag() != null ? request.getFreezeFlag() : 0);
        task.setBlindCount(request.getBlindCount() != null ? request.getBlindCount() : 0);
        task.setRecountThreshold(request.getRecountThreshold());
        task.setPriority(request.getPriority() != null ? request.getPriority() : 5);
        task.setAssignee(request.getAssignee());
        task.setChecker(request.getChecker());
        task.setTotalSkuCount(0);
        task.setTotalLocationCount(0);
        task.setCountedSkuCount(0);
        task.setDiffSkuCount(0);
        task.setDiffQty(BigDecimal.ZERO);
        task.setOwnerCodeCol(request.getOwnerCode());
        task.setWarehouseCodeCol(request.getWarehouseCode());

        taskMapper.insert(task);
        log.info("创建盘点任务: {}, 类型={}", taskNo, request.getStocktakeType());
        return task;
    }

    // ============================================================
    // 2. 生成盘点明细 (根据盘点范围从库存表生成)
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public int generateItems(Long taskId) {
        StocktakeTask task = getTask(taskId);
        if (!StocktakeTaskStatus.DRAFT.getCode().equals(task.getStatus())) {
            throw new BizException("只有草稿状态才能生成明细");
        }

        // 查询库存生成明细
        LambdaQueryWrapper<Inventory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Inventory::getWarehouseCode, task.getWarehouseCode());
        if (task.getOwnerCode() != null)
            wrapper.eq(Inventory::getOwnerCodeCol, task.getOwnerCode());
        if (task.getAreaCode() != null)
            wrapper.like(Inventory::getLocationCode, task.getAreaCode());
        if (task.getBatchNo() != null) wrapper.eq(Inventory::getBatchNo, task.getBatchNo());
        wrapper.gt(Inventory::getAvailableQty, BigDecimal.ZERO);
        wrapper.orderByAsc(Inventory::getLocationCode, Inventory::getSkuCode);

        List<Inventory> inventories = inventoryMapper.selectList(wrapper);
        int count = 0;

        for (Inventory inv : inventories) {
            StocktakeItem item = new StocktakeItem();
            item.setTaskId(taskId);
            item.setLocationCode(inv.getLocationCode());
            item.setSku(inv.getSkuCode());
            item.setBarcode(inv.getBarcode());
            item.setProductName(inv.getProductName());
            item.setBatchNo(inv.getBatchNo());
            item.setOwnerCode(inv.getOwnerCodeCol());
            item.setSystemQty(inv.getAvailableQty());
            item.setDiffQty(BigDecimal.ZERO);
            item.setCountStatus("PENDING");
            item.setOwnerCodeCol(task.getOwnerCodeCol());
            item.setWarehouseCodeCol(task.getWarehouseCodeCol());
            itemMapper.insert(item);
            count++;
        }

        task.setTotalSkuCount(count);
        task.setTotalLocationCount(
                (int) inventories.stream().map(Inventory::getLocationCode).distinct().count());
        task.setStatus(StocktakeTaskStatus.PENDING.getCode());
        taskMapper.updateById(task);

        log.info("盘点任务{}生成明细{}条", task.getTaskNo(), count);
        return count;
    }

    // ============================================================
    // 3. 开始盘点
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public StocktakeTask startStocktake(Long taskId) {
        StocktakeTask task = getTask(taskId);
        if (!StocktakeTaskStatus.PENDING.getCode().equals(task.getStatus())) {
            throw new BizException("只有待执行状态才能开始盘点");
        }
        task.setStatus(StocktakeTaskStatus.COUNTING.getCode());
        task.setActualStartTime(LocalDateTime.now());
        taskMapper.updateById(task);

        // 冻结库存(如果配置)
        if (task.getFreezeFlag() != null && task.getFreezeFlag() == 1) {
            log.info("盘点任务{}冻结库存", task.getTaskNo());
            // TODO: 冻结盘点范围内的库存
        }
        return task;
    }

    // ============================================================
    // 4. 执行盘点 (录入初盘数量)
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public StocktakeItem countItem(StocktakeCountRequest request) {
        StocktakeItem item = itemMapper.selectById(request.getItemId());
        if (item == null) throw new BizException("盘点明细不存在");

        // 初盘
        if (item.getFirstCountQty() == null) {
            item.setFirstCountQty(request.getCountQty());
            item.setFirstCounter(request.getCounter());
            item.setFirstCountTime(LocalDateTime.now());
            item.setCountStatus("COUNTED");
        } else {
            // 复盘
            item.setSecondCountQty(request.getCountQty());
            item.setSecondCounter(request.getCounter());
            item.setSecondCountTime(LocalDateTime.now());
            item.setCountStatus("CONFIRMED");
        }

        // 计算差异(非盲盘时)
        StocktakeTask task = taskMapper.selectById(item.getTaskId());
        if (task.getBlindCount() == null || task.getBlindCount() == 0) {
            BigDecimal finalQty =
                    item.getSecondCountQty() != null
                            ? item.getSecondCountQty()
                            : item.getFirstCountQty();
            item.setFinalCountQty(finalQty);
            item.setDiffQty(finalQty.subtract(item.getSystemQty()));
        }

        itemMapper.updateById(item);

        // 检查是否需要复盘
        checkRecount(item, task);

        // 更新任务进度
        updateTaskProgress(item.getTaskId());

        return item;
    }

    /** 检查是否需要复盘: 初盘与系统差异超过阈值, 或初盘复盘不一致 */
    private void checkRecount(StocktakeItem item, StocktakeTask task) {
        if (task.getRecountThreshold() == null) return;

        if (item.getFirstCountQty() != null && item.getSecondCountQty() == null) {
            BigDecimal diff = item.getFirstCountQty().subtract(item.getSystemQty()).abs();
            if (diff.compareTo(task.getRecountThreshold()) > 0) {
                item.setCountStatus("RECOUNT");
                itemMapper.updateById(item);
                log.info("盘点明细{}差异{}超过阈值{}, 触发复盘", item.getId(), diff, task.getRecountThreshold());
            }
        }
    }

    // ============================================================
    // 5. 完成盘点, 生成差异
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public int finishStocktake(Long taskId) {
        StocktakeTask task = getTask(taskId);
        if (!StocktakeTaskStatus.COUNTING.getCode().equals(task.getStatus())
                && !StocktakeTaskStatus.RECOUNTING.getCode().equals(task.getStatus())) {
            throw new BizException("盘点状态不允许完成");
        }

        // 检查是否所有明细都已盘点
        int pending = itemMapper.countPendingByTaskId(taskId);
        if (pending > 0) {
            throw new BizException("还有" + pending + "条明细未盘点完成");
        }

        // 统计差异
        List<StocktakeItem> items = itemMapper.selectByTaskId(taskId);
        int diffCount = 0;
        BigDecimal totalDiff = BigDecimal.ZERO;
        for (StocktakeItem item : items) {
            if (item.getDiffQty() != null && item.getDiffQty().compareTo(BigDecimal.ZERO) != 0) {
                diffCount++;
                totalDiff = totalDiff.add(item.getDiffQty());
            }
        }

        task.setStatus(StocktakeTaskStatus.ADJUSTING.getCode());
        task.setDiffSkuCount(diffCount);
        task.setDiffQty(totalDiff);
        task.setActualEndTime(LocalDateTime.now());
        taskMapper.updateById(task);

        // 解冻库存
        if (task.getFreezeFlag() != null && task.getFreezeFlag() == 1) {
            log.info("盘点任务{}解冻库存", task.getTaskNo());
        }

        log.info("盘点任务{}完成, 差异{}条, 总差异{}", task.getTaskNo(), diffCount, totalDiff);
        return diffCount;
    }

    // ============================================================
    // 9. 完成盘点任务
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public StocktakeTask completeTask(Long taskId) {
        StocktakeTask task = getTask(taskId);
        if (!StocktakeTaskStatus.ADJUSTING.getCode().equals(task.getStatus())) {
            throw new BizException("只有调整中状态才能完成");
        }
        task.setStatus(StocktakeTaskStatus.COMPLETED.getCode());
        taskMapper.updateById(task);
        log.info("盘点任务{}全部完成", task.getTaskNo());
        return task;
    }

    // ============================================================
    // 10. 查询
    // ============================================================

    public StocktakeTask getTask(Long id) {
        StocktakeTask task = taskMapper.selectById(id);
        if (task == null) throw new BizException("盘点任务不存在: " + id);
        return task;
    }

    public Page<StocktakeTask> pageTasks(
            Page<StocktakeTask> page, String status, String type, String warehouse) {
        LambdaQueryWrapper<StocktakeTask> wrapper = new LambdaQueryWrapper<>();
        if (status != null) wrapper.eq(StocktakeTask::getStatus, status);
        if (type != null) wrapper.eq(StocktakeTask::getStocktakeType, type);
        if (warehouse != null) wrapper.eq(StocktakeTask::getWarehouseCode, warehouse);
        wrapper.orderByDesc(StocktakeTask::getCreatedAt);
        return taskMapper.selectPage(page, wrapper);
    }

    public List<StocktakeItem> getItems(Long taskId) {
        return itemMapper.selectByTaskId(taskId);
    }

    // ============================================================
    // 工具方法
    // ============================================================

    private void updateTaskProgress(Long taskId) {
        StocktakeTask task = taskMapper.selectById(taskId);
        int counted =
                (int)
                        itemMapper
                                .selectList(
                                        new LambdaQueryWrapper<StocktakeItem>()
                                                .eq(StocktakeItem::getTaskId, taskId)
                                                .in(
                                                        StocktakeItem::getCountStatus,
                                                        "COUNTED",
                                                        "CONFIRMED",
                                                        "ADJUSTED"))
                                .stream()
                                .count();
        task.setCountedSkuCount(counted);
        taskMapper.updateById(task);
    }

    private String generateTaskNo() {
        return "ST"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", TASK_SEQ.incrementAndGet() % 1000);
    }

    private String generateAdjustNo() {
        return "SA"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", ADJUST_SEQ.incrementAndGet() % 1000);
    }
}
