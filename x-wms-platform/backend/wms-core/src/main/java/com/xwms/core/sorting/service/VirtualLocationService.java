package com.xwms.core.sorting.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import com.xwms.base.location.entity.Location;
import com.xwms.base.location.enums.VirtualLocationType;
import com.xwms.base.location.mapper.LocationMapper;
import com.xwms.core.inventory.entity.Inventory;
import com.xwms.core.inventory.mapper.InventoryMapper;
import com.xwms.core.inventory.service.InventoryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 虚拟库位管理服务 核心能力： 1. 虚拟库位创建（分拣中/格口/差异/暂存/在途/系统） 2. 虚拟库位库存转移（实库位→虚拟库位→实库位） 3. 虚拟库位库存查询与对账 4. 虚拟库位自动释放
 *
 * <p>设计原则： - 虚拟库位纳入统一库位表（is_virtual=Y），复用库存表 - 虚拟库位库存计入总量，但分拣中/格口/差异库存不可被新订单分配 -
 * 虚拟库位与业务单据（波次/订单/调拨单）绑定
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VirtualLocationService {

    /**
     * 虚拟库位库存转移链路的占位货主编码。 transfer 调用方（分拣/差异处理）当前均不传 ownerCode，而 InventoryService 写操作 的 9 参签名强制要求
     * ownerCode 做唯一键定位。此处用统一占位常量使链路可编译运行， 与多货主库存隔离；后续若需真实货主维度，需改造 transfer 签名及调用方。
     */
    private static final String DEFAULT_OWNER = "DEFAULT_OWNER";

    private final LocationMapper locationMapper;
    private final InventoryMapper inventoryMapper;
    private final InventoryService inventoryService;

    /**
     * 创建虚拟库位
     *
     * @param type 虚拟库位类型
     * @param bizNo 业务单号（波次号/订单号/调拨单号等）
     * @param warehouseCode 仓库
     * @param areaCode 库区
     */
    @Transactional(rollbackFor = Exception.class)
    public Location createVirtualLocation(
            VirtualLocationType type, String bizNo, String warehouseCode, String areaCode) {
        String locationCode = type.generateCode(bizNo);

        // 检查是否已存在
        Location existing =
                locationMapper.selectOne(
                        new LambdaQueryWrapper<Location>()
                                .eq(Location::getLocationCode, locationCode));
        if (existing != null) {
            return existing;
        }

        Location loc = new Location();
        loc.setLocationCode(locationCode);
        loc.setLocationName(type.getName() + "-" + bizNo);
        loc.setWarehouseCode(warehouseCode);
        loc.setAreaCode(areaCode != null ? areaCode : "VIRTUAL");
        loc.setLocationType("VIRTUAL");
        loc.setStatus("EMPTY");
        loc.setIsVirtual("Y");
        loc.setVirtualType(type.getCode());
        loc.setRefBizNo(bizNo);
        loc.setAutoRelease("Y");
        loc.setCapacity(BigDecimal.ZERO);
        loc.setUsedCapacity(BigDecimal.ZERO);
        locationMapper.insert(loc);

        log.info("创建虚拟库位: code={}, type={}, bizNo={}", locationCode, type.getCode(), bizNo);
        return loc;
    }

    /**
     * 库存转移：源库位 → 目标库位 支持实库位→虚拟库位、虚拟库位→实库位、虚拟库位→虚拟库位
     *
     * @param fromLocation 源库位编码
     * @param toLocation 目标库位编码
     * @param sku 商品
     * @param batchNo 批次
     * @param qty 数量
     * @param refBizNo 关联业务单号
     */
    @Transactional(rollbackFor = Exception.class)
    public void transfer(
            String fromLocation,
            String toLocation,
            String sku,
            String batchNo,
            BigDecimal qty,
            String warehouse,
            String refBizNo) {
        // 1. 源库位扣减（库存不足/乐观锁冲突时抛 RuntimeException 即转移失败）
        inventoryService.deductInventory(
                warehouse,
                fromLocation,
                sku,
                batchNo,
                DEFAULT_OWNER,
                qty,
                "TRANSFER",
                refBizNo,
                "system");

        // 2. 目标库位增加
        inventoryService.addInventory(
                warehouse,
                toLocation,
                sku,
                batchNo,
                DEFAULT_OWNER,
                qty,
                "TRANSFER",
                refBizNo,
                "system");

        // 3. 更新库位已用容量
        updateUsedCapacity(fromLocation, qty.negate());
        updateUsedCapacity(toLocation, qty);

        log.info(
                "库存转移: {} → {}, sku={}, batch={}, qty={}, ref={}",
                fromLocation,
                toLocation,
                sku,
                batchNo,
                qty,
                refBizNo);
    }

    /** 第一次拣货完成：实库位 → 波次分拣虚拟库位 库存状态：可用/预占 → 分拣中 */
    @Transactional(rollbackFor = Exception.class)
    public void onFirstPickComplete(String waveNo, String warehouse, List<PickItem> pickItems) {
        String waveVirtualLoc = VirtualLocationType.SORTING.generateCode(waveNo);

        // 确保虚拟库位存在
        createVirtualLocation(VirtualLocationType.SORTING, waveNo, warehouse, "SORTING_AREA");

        for (PickItem item : pickItems) {
            transfer(
                    item.getFromLocation(),
                    waveVirtualLoc,
                    item.getSku(),
                    item.getBatchNo(),
                    item.getQty(),
                    warehouse,
                    waveNo);
        }

        log.info("波次第一次拣货完成，库存转入分拣虚拟库位: wave={}, items={}", waveNo, pickItems.size());
    }

    /** 二次分拣完成：格口虚拟库位 → 发货暂存虚拟库位 */
    @Transactional(rollbackFor = Exception.class)
    public void onSortingComplete(
            String orderNo,
            String warehouse,
            String gridLocationCode,
            BigDecimal qty,
            String sku,
            String batchNo) {
        String shippingLoc = VirtualLocationType.STAGING.generateCode("SHIP-" + orderNo);
        createVirtualLocation(
                VirtualLocationType.STAGING, "SHIP-" + orderNo, warehouse, "SHIPPING_AREA");

        transfer(gridLocationCode, shippingLoc, sku, batchNo, qty, warehouse, orderNo);
    }

    /** 查询虚拟库位库存 */
    public List<Inventory> queryVirtualInventory(VirtualLocationType type, String bizNo) {
        String locationPattern = type.getCodePrefix() + bizNo;
        return inventoryMapper.selectList(
                new LambdaQueryWrapper<Inventory>()
                        .likeRight(Inventory::getLocationCode, locationPattern)
                        .gt(Inventory::getQuantity, BigDecimal.ZERO));
    }

    /**
     * 虚拟库位对账：校验波次分拣虚拟库位库存是否与波次进度一致
     *
     * @return 差异数量（>0表示虚拟库位多了，<0表示少了）
     */
    public BigDecimal reconcileWaveSorting(
            String waveNo, String warehouse, BigDecimal totalPickedQty, BigDecimal sortedQty) {
        String waveVirtualLoc = VirtualLocationType.SORTING.generateCode(waveNo);
        BigDecimal virtualQty =
                inventoryMapper
                        .selectList(
                                new LambdaQueryWrapper<Inventory>()
                                        .eq(Inventory::getLocationCode, waveVirtualLoc)
                                        .eq(Inventory::getWarehouseCode, warehouse))
                        .stream()
                        .map(Inventory::getQuantity)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal expected = totalPickedQty.subtract(sortedQty);
        BigDecimal diff = virtualQty.subtract(expected);

        if (diff.compareTo(BigDecimal.ZERO) != 0) {
            log.warn(
                    "虚拟库位对账异常: wave={}, 虚拟库位={}, 理论={}, 差异={}", waveNo, virtualQty, expected, diff);
        }
        return diff;
    }

    /** 释放虚拟库位（库存为0且自动释放标记为Y时） */
    @Transactional(rollbackFor = Exception.class)
    public void releaseIfEmpty(String locationCode) {
        Location loc =
                locationMapper.selectOne(
                        new LambdaQueryWrapper<Location>()
                                .eq(Location::getLocationCode, locationCode));
        if (loc == null || !"Y".equals(loc.getIsVirtual()) || !"Y".equals(loc.getAutoRelease())) {
            return;
        }

        BigDecimal qty =
                inventoryMapper
                        .selectList(
                                new LambdaQueryWrapper<Inventory>()
                                        .eq(Inventory::getLocationCode, locationCode))
                        .stream()
                        .map(Inventory::getQuantity)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (qty.compareTo(BigDecimal.ZERO) == 0) {
            loc.setStatus("EMPTY");
            loc.setUsedCapacity(BigDecimal.ZERO);
            locationMapper.updateById(loc);
            log.info("虚拟库位已释放: {}", locationCode);
        }
    }

    private void updateUsedCapacity(String locationCode, BigDecimal delta) {
        Location loc =
                locationMapper.selectOne(
                        new LambdaQueryWrapper<Location>()
                                .eq(Location::getLocationCode, locationCode));
        if (loc != null) {
            BigDecimal current =
                    loc.getUsedCapacity() != null ? loc.getUsedCapacity() : BigDecimal.ZERO;
            loc.setUsedCapacity(current.add(delta));
            if (loc.getUsedCapacity().compareTo(BigDecimal.ZERO) <= 0
                    && "Y".equals(loc.getIsVirtual())) {
                loc.setStatus("EMPTY");
            } else if ("Y".equals(loc.getIsVirtual())) {
                loc.setStatus("OCCUPIED");
            }
            locationMapper.updateById(loc);
        }
    }

    /** 拣货明细（内部DTO） */
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class PickItem {
        private String fromLocation;
        private String sku;
        private String batchNo;
        private BigDecimal qty;
    }
}
