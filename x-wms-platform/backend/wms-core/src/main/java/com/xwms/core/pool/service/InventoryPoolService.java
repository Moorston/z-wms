package com.xwms.core.pool.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.core.pool.entity.*;
import com.xwms.core.pool.mapper.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 库存共享/分配池管理核心服务 核心能力: 共享规则/分配池/池化库存/池化分配/库存共享 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryPoolService {

    private final ShareRuleMapper shareRuleMapper;
    private final AllocationPoolMapper poolMapper;
    private final PoolInventoryMapper poolInventoryMapper;
    private final PoolAllocationMapper poolAllocationMapper;

    private static final AtomicInteger SEQ = new AtomicInteger(0);
    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    // ============================================================

    // 1. 共享规则管理
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public ShareRule createShareRule(ShareRule rule) {
        rule.setStatus("ACTIVE");
        if (rule.getShareRatio() == null) rule.setShareRatio(new BigDecimal("100"));
        if (rule.getPriority() == null) rule.setPriority(5);
        shareRuleMapper.insert(rule);
        log.info(
                "创建共享规则: {}={}, type={}, pool={}",
                rule.getRuleCode(),
                rule.getRuleName(),
                rule.getShareType(),
                rule.getPoolCode());
        return rule;
    }

    public ShareRule getShareRuleByCode(String ruleCode) {
        return shareRuleMapper.selectByRuleCode(ruleCode);
    }

    public List<ShareRule> getShareRulesByPool(String poolCode) {
        return shareRuleMapper.selectByPoolCode(poolCode);
    }

    public List<ShareRule> getShareRulesByWarehouseAndType(String warehouseCode, String shareType) {
        return shareRuleMapper.selectByWarehouseAndType(warehouseCode, shareType);
    }

    public List<ShareRule> getShareRulesByOwnerPair(String sourceOwner, String targetOwner) {
        return shareRuleMapper.selectByOwnerPair(sourceOwner, targetOwner);
    }

    public Page<ShareRule> pageShareRules(
            Page<ShareRule> page, String warehouseCode, String shareType) {
        LambdaQueryWrapper<ShareRule> wrapper = new LambdaQueryWrapper<>();
        if (warehouseCode != null) wrapper.eq(ShareRule::getWarehouseCode, warehouseCode);
        if (shareType != null) wrapper.eq(ShareRule::getShareType, shareType);
        wrapper.eq(ShareRule::getStatus, "ACTIVE");
        wrapper.orderByDesc(ShareRule::getPriority);
        return shareRuleMapper.selectPage(page, wrapper);
    }

    // ============================================================

    // 2. 分配池管理
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public AllocationPool createPool(AllocationPool pool) {
        pool.setStatus("ACTIVE");
        if (pool.getAllocationMode() == null) pool.setAllocationMode("FIFO");
        if (pool.getPriority() == null) pool.setPriority(5);
        poolMapper.insert(pool);
        log.info(
                "创建分配池: {}={}, type={}, warehouse={}",
                pool.getPoolCode(),
                pool.getPoolName(),
                pool.getPoolType(),
                pool.getWarehouseCode());
        return pool;
    }

    public AllocationPool getPoolByCode(String poolCode) {
        return poolMapper.selectByPoolCode(poolCode);
    }

    public List<AllocationPool> getPoolsByWarehouse(String warehouseCode) {
        return poolMapper.selectByWarehouse(warehouseCode);
    }

    public List<AllocationPool> getPoolsByWarehouseAndType(String warehouseCode, String poolType) {
        return poolMapper.selectByWarehouseAndType(warehouseCode, poolType);
    }

    public List<AllocationPool> getPoolsByOwner(String ownerCode) {
        return poolMapper.selectByOwner(ownerCode);
    }

    public Page<AllocationPool> pagePools(
            Page<AllocationPool> page, String warehouseCode, String poolType) {
        LambdaQueryWrapper<AllocationPool> wrapper = new LambdaQueryWrapper<>();
        if (warehouseCode != null) wrapper.eq(AllocationPool::getWarehouseCode, warehouseCode);
        if (poolType != null) wrapper.eq(AllocationPool::getPoolType, poolType);
        wrapper.eq(AllocationPool::getStatus, "ACTIVE");
        wrapper.orderByDesc(AllocationPool::getPriority);
        return poolMapper.selectPage(page, wrapper);
    }

    // ============================================================

    // 3. 池化库存管理
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public PoolInventory addPoolInventory(PoolInventory inventory) {
        inventory.setStatus("ACTIVE");
        if (inventory.getTotalQty() == null) inventory.setTotalQty(BigDecimal.ZERO);
        if (inventory.getAvailableQty() == null) inventory.setAvailableQty(BigDecimal.ZERO);
        if (inventory.getAllocatedQty() == null) inventory.setAllocatedQty(BigDecimal.ZERO);
        if (inventory.getReservedQty() == null) inventory.setReservedQty(BigDecimal.ZERO);
        if (inventory.getFrozenQty() == null) inventory.setFrozenQty(BigDecimal.ZERO);
        poolInventoryMapper.insert(inventory);
        log.info(
                "添加池化库存: pool={}, sku={}, qty={}",
                inventory.getPoolCode(),
                inventory.getSkuCode(),
                inventory.getTotalQty());
        return inventory;
    }

    public List<PoolInventory> getPoolInventoryByPool(String poolCode) {
        return poolInventoryMapper.selectByPoolCode(poolCode);
    }

    public List<PoolInventory> getPoolInventoryByPoolAndSku(String poolCode, String skuCode) {
        return poolInventoryMapper.selectByPoolAndSku(poolCode, skuCode);
    }

    public List<PoolInventory> getAvailableInventoryByWarehouseAndSku(
            String warehouseCode, String skuCode) {
        return poolInventoryMapper.selectAvailableByWarehouseAndSku(warehouseCode, skuCode);
    }

    public Page<PoolInventory> pagePoolInventory(
            Page<PoolInventory> page, String poolCode, String warehouseCode, String skuCode) {
        LambdaQueryWrapper<PoolInventory> wrapper = new LambdaQueryWrapper<>();
        if (poolCode != null) wrapper.eq(PoolInventory::getPoolCode, poolCode);
        if (warehouseCode != null) wrapper.eq(PoolInventory::getWarehouseCode, warehouseCode);
        if (skuCode != null) wrapper.eq(PoolInventory::getSkuCode, skuCode);
        wrapper.eq(PoolInventory::getStatus, "ACTIVE");
        wrapper.orderByDesc(PoolInventory::getUpdatedTime);
        return poolInventoryMapper.selectPage(page, wrapper);
    }

    // ============================================================

    // 4. 池化分配（核心）
    // ============================================================

    /** 从分配池分配库存 */
    @Transactional(rollbackFor = Exception.class)
    public String allocateFromPool(
            String orderNo,
            String warehouseCode,
            String targetOwner,
            String skuCode,
            BigDecimal needQty,
            String operator) {
        String allocationId = generateAllocationId();

        // 获取所有可用池化库存（按池优先级排序）
        List<PoolInventory> availableInventory =
                poolInventoryMapper.selectAvailableByWarehouseAndSku(warehouseCode, skuCode);

        if (availableInventory.isEmpty()) {
            throw new RuntimeException("无可用池化库存: warehouse=" + warehouseCode + ", sku=" + skuCode);
        }

        BigDecimal remaining = needQty;
        for (PoolInventory inv : availableInventory) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;

            BigDecimal available = inv.getAvailableQty();
            if (available.compareTo(BigDecimal.ZERO) <= 0) continue;

            BigDecimal allocQty = available.min(remaining);

            // 原子扣减可用库存，增加已分配库存
            int updated = poolInventoryMapper.allocateInventory(inv.getId(), allocQty);
            if (updated == 0) {
                log.warn("池化库存分配失败(并发冲突): id={}, retry", inv.getId());
                continue;
            }

            // 查找共享规则
            String shareRuleCode = null;
            String sourceOwner = inv.getOwnerCode();
            if (sourceOwner != null && !sourceOwner.equals(targetOwner)) {
                List<ShareRule> rules = shareRuleMapper.selectByOwnerPair(sourceOwner, targetOwner);
                if (!rules.isEmpty()) {
                    shareRuleCode = rules.get(0).getRuleCode();
                }
            }

            // 创建池化分配记录
            PoolAllocation allocation = new PoolAllocation();
            allocation.setAllocationId(allocationId);
            allocation.setPoolCode(inv.getPoolCode());
            allocation.setWarehouseCode(warehouseCode);
            allocation.setOrderNo(orderNo);
            allocation.setSkuCode(skuCode);
            allocation.setSkuName(inv.getSkuName());
            allocation.setBatchNo(inv.getBatchNo());
            allocation.setLocationCode(inv.getLocationCode());
            allocation.setAllocatedQty(allocQty);
            allocation.setPickedQty(BigDecimal.ZERO);
            allocation.setRemainQty(allocQty);
            allocation.setSourceOwner(sourceOwner);
            allocation.setTargetOwner(targetOwner);
            allocation.setShareRuleCode(shareRuleCode);
            allocation.setStatus("ALLOCATED");
            allocation.setAllocateTime(LocalDateTime.now());
            allocation.setOperator(operator);
            poolAllocationMapper.insert(allocation);

            remaining = remaining.subtract(allocQty);
        }

        if (remaining.compareTo(BigDecimal.ZERO) > 0) {
            log.warn(
                    "池化库存分配不足: order={}, sku={}, need={}, allocated={}",
                    orderNo,
                    skuCode,
                    needQty,
                    needQty.subtract(remaining));
        }

        log.info(
                "池化分配完成: allocationId={}, order={}, sku={}, need={}, allocated={}",
                allocationId,
                orderNo,
                skuCode,
                needQty,
                needQty.subtract(remaining));
        return allocationId;
    }

    /** 释放池化分配 */
    @Transactional(rollbackFor = Exception.class)
    public int releasePoolAllocation(String allocationId, String reason, String operator) {
        List<PoolAllocation> allocations =
                poolAllocationMapper.selectList(
                        new LambdaQueryWrapper<PoolAllocation>()
                                .eq(PoolAllocation::getAllocationId, allocationId)
                                .in(PoolAllocation::getStatus, "ALLOCATED", "PICKING"));

        int count = 0;
        for (PoolAllocation alloc : allocations) {
            // 释放池化库存
            List<PoolInventory> inventories =
                    poolInventoryMapper.selectByPoolAndSku(alloc.getPoolCode(), alloc.getSkuCode());
            for (PoolInventory inv : inventories) {
                if (inv.getBatchNo() != null
                        && inv.getBatchNo().equals(alloc.getBatchNo())
                        && inv.getLocationCode() != null
                        && inv.getLocationCode().equals(alloc.getLocationCode())) {
                    poolInventoryMapper.releaseInventory(inv.getId(), alloc.getRemainQty());
                    break;
                }
            }

            // 更新分配记录状态
            poolAllocationMapper.updateStatus(
                    alloc.getId(), "RELEASED", alloc.getPickedQty(), BigDecimal.ZERO);
            count++;
        }

        log.info("释放池化分配: allocationId={}, count={}, reason={}", allocationId, count, reason);
        return count;
    }

    /** 池化分配拣货确认 */
    @Transactional(rollbackFor = Exception.class)
    public PoolAllocation confirmPoolPick(
            Long allocationId, BigDecimal pickedQty, String operator) {
        PoolAllocation alloc = poolAllocationMapper.selectById(allocationId);
        if (alloc == null) throw new RuntimeException("池化分配记录不存在: " + allocationId);
        if (!"ALLOCATED".equals(alloc.getStatus()) && !"PICKING".equals(alloc.getStatus())) {
            throw new RuntimeException("池化分配状态不正确: " + alloc.getStatus());
        }

        BigDecimal newPickedQty = alloc.getPickedQty().add(pickedQty);
        BigDecimal newRemainQty = alloc.getAllocatedQty().subtract(newPickedQty);
        String newStatus = newRemainQty.compareTo(BigDecimal.ZERO) <= 0 ? "PICKED" : "PICKING";

        poolAllocationMapper.updateStatus(allocationId, newStatus, newPickedQty, newRemainQty);

        log.info(
                "池化分配拣货确认: allocationId={}, picked={}, remain={}",
                allocationId,
                pickedQty,
                newRemainQty);
        return poolAllocationMapper.selectById(allocationId);
    }

    public PoolAllocation getPoolAllocationById(String allocationId) {
        return poolAllocationMapper.selectByAllocationId(allocationId);
    }

    public List<PoolAllocation> getPoolAllocationsByOrder(String orderNo) {
        return poolAllocationMapper.selectByOrderNo(orderNo);
    }

    public List<PoolAllocation> getActiveAllocationsByPool(String poolCode) {
        return poolAllocationMapper.selectActiveByPool(poolCode);
    }

    public List<PoolAllocation> getActiveAllocationsBySku(String skuCode) {
        return poolAllocationMapper.selectActiveBySku(skuCode);
    }

    public Page<PoolAllocation> pagePoolAllocations(
            Page<PoolAllocation> page,
            String poolCode,
            String orderNo,
            String skuCode,
            String status) {
        LambdaQueryWrapper<PoolAllocation> wrapper = new LambdaQueryWrapper<>();
        if (poolCode != null) wrapper.eq(PoolAllocation::getPoolCode, poolCode);
        if (orderNo != null) wrapper.eq(PoolAllocation::getOrderNo, orderNo);
        if (skuCode != null) wrapper.eq(PoolAllocation::getSkuCode, skuCode);
        if (status != null) wrapper.eq(PoolAllocation::getStatus, status);
        wrapper.orderByDesc(PoolAllocation::getAllocateTime);
        return poolAllocationMapper.selectPage(page, wrapper);
    }

    // ============================================================

    // 5. 库存共享匹配
    // ============================================================

    /** 匹配可用共享库存 */
    public List<Map<String, Object>> matchSharedInventory(
            String warehouseCode, String targetOwner, String skuCode, BigDecimal needQty) {
        List<Map<String, Object>> result = new ArrayList<>();

        // 获取所有可用池化库存
        List<PoolInventory> availableInventory =
                poolInventoryMapper.selectAvailableByWarehouseAndSku(warehouseCode, skuCode);

        for (PoolInventory inv : availableInventory) {
            String sourceOwner = inv.getOwnerCode();

            // 同货主直接可用
            if (sourceOwner == null || sourceOwner.equals(targetOwner)) {
                Map<String, Object> item = new HashMap<>();
                item.put("poolCode", inv.getPoolCode());
                item.put("sourceOwner", sourceOwner);
                item.put("targetOwner", targetOwner);
                item.put("skuCode", skuCode);
                item.put("batchNo", inv.getBatchNo());
                item.put("locationCode", inv.getLocationCode());
                item.put("availableQty", inv.getAvailableQty());
                item.put("shareType", "SELF");
                item.put("shareRatio", new BigDecimal("100"));
                result.add(item);
                continue;
            }

            // 跨货主需要匹配共享规则
            List<ShareRule> rules = shareRuleMapper.selectByOwnerPair(sourceOwner, targetOwner);
            for (ShareRule rule : rules) {
                if (rule.getShareRatio() != null
                        && rule.getShareRatio().compareTo(BigDecimal.ZERO) > 0) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("poolCode", inv.getPoolCode());
                    item.put("sourceOwner", sourceOwner);
                    item.put("targetOwner", targetOwner);
                    item.put("skuCode", skuCode);
                    item.put("batchNo", inv.getBatchNo());
                    item.put("locationCode", inv.getLocationCode());
                    item.put("availableQty", inv.getAvailableQty());
                    item.put("shareRuleCode", rule.getRuleCode());
                    item.put("shareType", rule.getShareType());
                    item.put("shareRatio", rule.getShareRatio());
                    result.add(item);
                }
            }
        }

        return result;
    }

    // ============================================================

    // 工具方法
    // ============================================================

    private String generateAllocationId() {
        return "PA"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }
}
