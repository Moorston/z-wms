package com.xwms.core.inventory.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.core.inventory.entity.Inventory;
import com.xwms.core.inventory.mapper.InventoryMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 库存管理核心服务 核心能力: Oracle直接扣减库存(原子SQL+乐观锁)、库存预占 职责边界: 只负责库存核心操作(查询/增减/预占/释放预占) 库存冻结 -> freeze模块 库存调整
 * -> adjust模块 库存流水 -> transaction模块
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryMapper inventoryMapper;

    private static final AtomicInteger SEQ = new AtomicInteger(0);
    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final int MAX_RETRY = 3;

    // ============================================================

    // 1. 库存查询
    // ============================================================

    public Page<Inventory> pageInventory(
            Page<Inventory> page,
            String warehouseCode,
            String locationCode,
            String skuCode,
            String batchNo,
            String ownerCode) {
        LambdaQueryWrapper<Inventory> wrapper = new LambdaQueryWrapper<>();
        if (warehouseCode != null) wrapper.eq(Inventory::getWarehouseCode, warehouseCode);
        if (locationCode != null) wrapper.eq(Inventory::getLocationCode, locationCode);
        if (skuCode != null) wrapper.eq(Inventory::getSkuCode, skuCode);
        if (batchNo != null) wrapper.eq(Inventory::getBatchNo, batchNo);
        if (ownerCode != null) wrapper.eq(Inventory::getOwnerCodeCol, ownerCode);
        wrapper.orderByAsc(Inventory::getWarehouseCode).orderByAsc(Inventory::getLocationCode);
        return inventoryMapper.selectPage(page, wrapper);
    }

    public Inventory getInventory(
            String warehouseCode,
            String locationCode,
            String skuCode,
            String batchNo,
            String ownerCode) {
        return inventoryMapper.selectByUniqueKey(
                warehouseCode, locationCode, skuCode, batchNo, ownerCode);
    }

    public List<Inventory> getInventoryBySku(String skuCode, String ownerCode) {
        return inventoryMapper.selectBySkuAndOwner(skuCode, ownerCode);
    }

    public List<Inventory> getInventoryByWarehouse(String warehouseCode, String ownerCode) {
        return inventoryMapper.selectByWarehouseAndOwner(warehouseCode, ownerCode);
    }

    // ============================================================

    // 2. 库存增加（入库）- Oracle原子SQL+乐观锁
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public Inventory addInventory(
            String warehouseCode,
            String locationCode,
            String skuCode,
            String batchNo,
            String ownerCode,
            BigDecimal qty,
            String refType,
            String refNo,
            String operator) {
        // 1. 查询或创建库存记录
        Inventory inventory =
                inventoryMapper.selectByUniqueKey(
                        warehouseCode, locationCode, skuCode, batchNo, ownerCode);

        if (inventory == null) {
            inventory = new Inventory();
            inventory.setInventoryNo(generateInventoryNo());
            inventory.setWarehouseCode(warehouseCode);
            inventory.setLocationCode(locationCode);
            inventory.setSkuCode(skuCode);
            inventory.setBatchNo(batchNo);
            inventory.setOwnerCodeCol(ownerCode);
            inventory.setQuantity(BigDecimal.ZERO);
            inventory.setAvailableQty(BigDecimal.ZERO);
            inventory.setAllocatedQty(BigDecimal.ZERO);
            inventory.setPickingQty(BigDecimal.ZERO);
            inventory.setFrozenQty(BigDecimal.ZERO);
            inventory.setStatus("NORMAL");
            inventory.setVersion(0);
            inventoryMapper.insert(inventory);
        }

        // 2. 乐观锁重试增加库存
        BigDecimal beforeQty = inventory.getQuantity();
        boolean success = false;
        for (int i = 0; i < MAX_RETRY; i++) {
            int rows = inventoryMapper.addInventory(inventory.getId(), qty, inventory.getVersion());
            if (rows > 0) {
                success = true;
                break;
            }
            // 重试：重新查询
            inventory = inventoryMapper.selectById(inventory.getId());
        }
        if (!success) {
            throw new RuntimeException("库存增加失败，乐观锁冲突: " + inventory.getInventoryNo());
        }

        log.info(
                "库存增加: inv={}, qty={}, before={}, after={}",
                inventory.getInventoryNo(),
                qty,
                beforeQty,
                beforeQty.add(qty));
        return inventoryMapper.selectById(inventory.getId());
    }

    // ============================================================

    // 3. 库存扣减（出库）- Oracle原子SQL+乐观锁
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public Inventory deductInventory(
            String warehouseCode,
            String locationCode,
            String skuCode,
            String batchNo,
            String ownerCode,
            BigDecimal qty,
            String refType,
            String refNo,
            String operator) {
        Inventory inventory =
                inventoryMapper.selectByUniqueKey(
                        warehouseCode, locationCode, skuCode, batchNo, ownerCode);
        if (inventory == null) {
            throw new RuntimeException(
                    "库存不存在: " + warehouseCode + "/" + locationCode + "/" + skuCode);
        }
        if (inventory.getQuantity().compareTo(qty) < 0) {
            throw new RuntimeException("库存不足: 需要" + qty + ", 实际" + inventory.getQuantity());
        }

        BigDecimal beforeQty = inventory.getQuantity();

        // 乐观锁重试扣减
        boolean success = false;
        for (int i = 0; i < MAX_RETRY; i++) {
            int rows =
                    inventoryMapper.deductInventory(inventory.getId(), qty, inventory.getVersion());
            if (rows > 0) {
                success = true;
                break;
            }
            inventory = inventoryMapper.selectById(inventory.getId());
        }
        if (!success) {
            throw new RuntimeException("库存扣减失败，乐观锁冲突: " + inventory.getInventoryNo());
        }

        log.info(
                "库存扣减: inv={}, qty={}, before={}, after={}",
                inventory.getInventoryNo(),
                qty,
                beforeQty,
                beforeQty.subtract(qty));
        return inventoryMapper.selectById(inventory.getId());
    }

    // ============================================================

    // 3.1 核销预占扣减（出库发运）- 总量减少、预占核销、可用不动（预占时已减过可用）
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public Inventory deductAllocatedInventory(
            String warehouseCode,
            String locationCode,
            String skuCode,
            String batchNo,
            String ownerCode,
            BigDecimal qty,
            String refType,
            String refNo,
            String operator) {
        Inventory inventory =
                inventoryMapper.selectByUniqueKey(
                        warehouseCode, locationCode, skuCode, batchNo, ownerCode);
        if (inventory == null) {
            throw new RuntimeException(
                    "库存不存在: " + warehouseCode + "/" + locationCode + "/" + skuCode);
        }
        if (inventory.getAllocatedQty().compareTo(qty) < 0) {
            throw new RuntimeException(
                    "预占不足，不能核销扣减: 需要" + qty + ", 预占" + inventory.getAllocatedQty());
        }

        BigDecimal beforeAllocated = inventory.getAllocatedQty();

        // 乐观锁重试核销扣减
        boolean success = false;
        for (int i = 0; i < MAX_RETRY; i++) {
            int rows =
                    inventoryMapper.deductAllocatedInventory(
                            inventory.getId(), qty, inventory.getVersion());
            if (rows > 0) {
                success = true;
                break;
            }
            inventory = inventoryMapper.selectById(inventory.getId());
        }
        if (!success) {
            throw new RuntimeException("核销扣减失败，乐观锁冲突: " + inventory.getInventoryNo());
        }

        log.info(
                "核销预占扣减: inv={}, qty={}, allocated_before={}, allocated_after={}",
                inventory.getInventoryNo(),
                qty,
                beforeAllocated,
                beforeAllocated.subtract(qty));
        return inventoryMapper.selectById(inventory.getId());
    }

    // ============================================================

    // 4. 库存预占（分配）- Oracle原子SQL+乐观锁
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public Inventory allocateInventory(
            String warehouseCode,
            String locationCode,
            String skuCode,
            String batchNo,
            String ownerCode,
            BigDecimal qty,
            String refType,
            String refNo,
            String operator) {
        Inventory inventory =
                inventoryMapper.selectByUniqueKey(
                        warehouseCode, locationCode, skuCode, batchNo, ownerCode);
        if (inventory == null) {
            throw new RuntimeException("库存不存在");
        }
        if (inventory.getAvailableQty().compareTo(qty) < 0) {
            throw new RuntimeException("可用库存不足: 需要" + qty + ", 可用" + inventory.getAvailableQty());
        }

        BigDecimal beforeAvailable = inventory.getAvailableQty();

        boolean success = false;
        for (int i = 0; i < MAX_RETRY; i++) {
            int rows =
                    inventoryMapper.allocateInventory(
                            inventory.getId(), qty, inventory.getVersion());
            if (rows > 0) {
                success = true;
                break;
            }
            inventory = inventoryMapper.selectById(inventory.getId());
        }
        if (!success) {
            throw new RuntimeException("库存预占失败，乐观锁冲突");
        }

        log.info(
                "库存预占: inv={}, qty={}, available={}->{}",
                inventory.getInventoryNo(),
                qty,
                beforeAvailable,
                beforeAvailable.subtract(qty));
        return inventoryMapper.selectById(inventory.getId());
    }

    // ============================================================

    // 5. 释放预占
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public Inventory releaseAllocation(
            String warehouseCode,
            String locationCode,
            String skuCode,
            String batchNo,
            String ownerCode,
            BigDecimal qty,
            String refType,
            String refNo,
            String operator) {
        Inventory inventory =
                inventoryMapper.selectByUniqueKey(
                        warehouseCode, locationCode, skuCode, batchNo, ownerCode);
        if (inventory == null) {
            throw new RuntimeException("库存不存在");
        }

        BigDecimal beforeAvailable = inventory.getAvailableQty();

        boolean success = false;
        for (int i = 0; i < MAX_RETRY; i++) {
            int rows =
                    inventoryMapper.releaseAllocation(
                            inventory.getId(), qty, inventory.getVersion());
            if (rows > 0) {
                success = true;
                break;
            }
            inventory = inventoryMapper.selectById(inventory.getId());
        }
        if (!success) {
            throw new RuntimeException("释放预占失败，乐观锁冲突");
        }

        log.info("释放预占: inv={}, qty={}", inventory.getInventoryNo(), qty);
        return inventoryMapper.selectById(inventory.getId());
    }

    // ============================================================

    // 工具方法
    // ============================================================

    private String generateInventoryNo() {
        return "INV"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }
}
