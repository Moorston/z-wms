package com.xwms.core.outbound.service;

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
import com.xwms.core.inventory.service.InventoryService;
import com.xwms.core.outbound.entity.*;
import com.xwms.core.outbound.enums.OutboundStatus;
import com.xwms.core.outbound.mapper.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 出库管理核心服务 包含: 出库单管理/库存分配/拣货/复核打包/发运 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OutboundService {

    private final OutboundOrderMapper outboundOrderMapper;
    private final OutboundDetailMapper outboundDetailMapper;
    private final PickRecordMapper pickRecordMapper;
    private final ShipRecordMapper shipRecordMapper;
    private final InventoryService inventoryService;
    private final InventoryMapper inventoryMapper;

    private static final AtomicInteger SEQ = new AtomicInteger(0);
    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    // ============================================================

    // 1. 出库单管理
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public OutboundOrder createOutboundOrder(OutboundOrder order, List<OutboundDetail> details) {
        if (order.getOutboundNo() == null) {
            order.setOutboundNo(generateOutboundNo());
        }
        if (order.getStatus() == null) order.setStatus(OutboundStatus.CREATED.getCode());
        if (order.getAllocatedQty() == null) order.setAllocatedQty(BigDecimal.ZERO);
        if (order.getPickedQty() == null) order.setPickedQty(BigDecimal.ZERO);
        if (order.getPackedQty() == null) order.setPackedQty(BigDecimal.ZERO);
        if (order.getShippedQty() == null) order.setShippedQty(BigDecimal.ZERO);
        outboundOrderMapper.insert(order);

        // 保存明细
        if (details != null) {
            for (int i = 0; i < details.size(); i++) {
                OutboundDetail detail = details.get(i);
                detail.setOutboundNo(order.getOutboundNo());
                detail.setLineNo(i + 1);
                detail.setDetailNo(generateDetailNo());
                if (detail.getAllocatedQty() == null) detail.setAllocatedQty(BigDecimal.ZERO);
                if (detail.getPickedQty() == null) detail.setPickedQty(BigDecimal.ZERO);
                if (detail.getPackedQty() == null) detail.setPackedQty(BigDecimal.ZERO);
                if (detail.getShippedQty() == null) detail.setShippedQty(BigDecimal.ZERO);
                if (detail.getStatus() == null) detail.setStatus("CREATED");
                outboundDetailMapper.insert(detail);
            }
        }
        log.info(
                "创建出库单: {}={}, 明细{}行",
                order.getOutboundNo(),
                order.getOutboundType(),
                details != null ? details.size() : 0);
        return order;
    }

    @Transactional(rollbackFor = Exception.class)
    public OutboundOrder updateOutboundOrder(OutboundOrder order) {
        outboundOrderMapper.updateById(order);
        return order;
    }

    public Page<OutboundOrder> pageOutboundOrders(
            Page<OutboundOrder> page,
            String outboundType,
            String status,
            String warehouseCode,
            String customerCode,
            String waveNo) {
        LambdaQueryWrapper<OutboundOrder> wrapper = new LambdaQueryWrapper<>();
        if (outboundType != null) wrapper.eq(OutboundOrder::getOutboundType, outboundType);
        if (status != null) wrapper.eq(OutboundOrder::getStatus, status);
        if (warehouseCode != null) wrapper.eq(OutboundOrder::getWarehouseCode, warehouseCode);
        if (customerCode != null) wrapper.eq(OutboundOrder::getCustomerCode, customerCode);
        if (waveNo != null) wrapper.eq(OutboundOrder::getWaveNo, waveNo);
        wrapper.orderByDesc(OutboundOrder::getCreatedTime);
        return outboundOrderMapper.selectPage(page, wrapper);
    }

    public OutboundOrder getOutboundOrderByNo(String outboundNo) {
        return outboundOrderMapper.selectByOutboundNo(outboundNo);
    }

    public List<OutboundDetail> getOutboundDetails(String outboundNo) {
        return outboundDetailMapper.selectByOutboundNo(outboundNo);
    }

    // ============================================================

    // 2. 库存分配
    // ============================================================

    /** 执行库存分配 */
    @Transactional(rollbackFor = Exception.class)
    public OutboundOrder allocate(String outboundNo, String operator) {
        OutboundOrder order = outboundOrderMapper.selectByOutboundNo(outboundNo);
        if (order == null) throw new RuntimeException("出库单不存在: " + outboundNo);

        order.setStatus(OutboundStatus.ALLOCATING.getCode());
        outboundOrderMapper.updateById(order);

        // FIFO 库存预占：按 last_in_time 升序跨库位累加预占，回填明细的仓库/库位/批次
        String owner = order.getOwnerCodeCol();
        List<OutboundDetail> details = outboundDetailMapper.selectByOutboundNo(outboundNo);
        for (OutboundDetail detail : details) {
            BigDecimal remaining = detail.getExpectedQty();
            BigDecimal totalAllocated = BigDecimal.ZERO;
            String allocWh = null, allocLoc = null, allocBatch = null;

            List<Inventory> candidates =
                    inventoryMapper.selectAvailableFifo(detail.getSkuCode(), owner);
            for (Inventory row : candidates) {
                if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;
                BigDecimal takeQty = row.getAvailableQty().min(remaining);
                inventoryService.allocateInventory(
                        row.getWarehouseCode(),
                        row.getLocationCode(),
                        detail.getSkuCode(),
                        row.getBatchNo(),
                        owner,
                        takeQty,
                        "OUTBOUND",
                        outboundNo,
                        operator);
                remaining = remaining.subtract(takeQty);
                totalAllocated = totalAllocated.add(takeQty);
                // 记录首个命中行作为明细回填（单行命中即回填该行；跨行累加回填首行）
                if (allocWh == null) {
                    allocWh = row.getWarehouseCode();
                    allocLoc = row.getLocationCode();
                    allocBatch = row.getBatchNo();
                }
            }
            if (remaining.compareTo(BigDecimal.ZERO) > 0) {
                throw new RuntimeException(
                        "可用库存不足: sku="
                                + detail.getSkuCode()
                                + ", 需要"
                                + detail.getExpectedQty()
                                + ", 缺"
                                + remaining);
            }
            detail.setWarehouseCode(allocWh);
            detail.setLocationCode(allocLoc);
            detail.setBatchNo(allocBatch);
            detail.setAllocatedQty(totalAllocated);
            detail.setStatus("ALLOCATED");
            outboundDetailMapper.updateById(detail);
        }

        order.setAllocatedQty(order.getTotalQty());
        order.setStatus(OutboundStatus.ALLOCATED.getCode());
        outboundOrderMapper.updateById(order);

        log.info("库存分配完成: {}, 分配数量={}", outboundNo, order.getAllocatedQty());
        return order;
    }

    /** 取消分配 */
    @Transactional(rollbackFor = Exception.class)
    public OutboundOrder cancelAllocation(String outboundNo, String operator) {
        OutboundOrder order = outboundOrderMapper.selectByOutboundNo(outboundNo);
        if (order == null) throw new RuntimeException("出库单不存在: " + outboundNo);

        List<OutboundDetail> details = outboundDetailMapper.selectByOutboundNo(outboundNo);
        String owner = order.getOwnerCodeCol();
        for (OutboundDetail detail : details) {
            // 释放预占：allocated_qty -= qty, available_qty += qty（仅当存在预占时）
            BigDecimal allocated = detail.getAllocatedQty();
            if (allocated != null
                    && allocated.compareTo(BigDecimal.ZERO) > 0
                    && detail.getWarehouseCode() != null) {
                inventoryService.releaseAllocation(
                        detail.getWarehouseCode(),
                        detail.getLocationCode(),
                        detail.getSkuCode(),
                        detail.getBatchNo(),
                        owner,
                        allocated,
                        "OUTBOUND_CANCEL",
                        outboundNo,
                        operator);
            }
            detail.setAllocatedQty(BigDecimal.ZERO);
            detail.setStatus("CREATED");
            outboundDetailMapper.updateById(detail);
        }

        order.setAllocatedQty(BigDecimal.ZERO);
        order.setStatus(OutboundStatus.CREATED.getCode());
        outboundOrderMapper.updateById(order);

        log.info("取消分配: {}", outboundNo);
        return order;
    }

    // ============================================================

    // 3. 拣货管理
    // ============================================================

    /** 执行拣货 */
    @Transactional(rollbackFor = Exception.class)
    public PickRecord pick(
            String outboundNo,
            String detailNo,
            String skuCode,
            String batchNo,
            String fromLocation,
            BigDecimal pickQty,
            String pickType,
            String operator) {
        OutboundOrder order = outboundOrderMapper.selectByOutboundNo(outboundNo);
        if (order == null) throw new RuntimeException("出库单不存在: " + outboundNo);

        // 更新出库单状态
        if (OutboundStatus.ALLOCATED.getCode().equals(order.getStatus())) {
            order.setStatus(OutboundStatus.PICKING.getCode());
        }
        order.setPickedQty(order.getPickedQty().add(pickQty));
        outboundOrderMapper.updateById(order);

        // 更新明细
        OutboundDetail detail = outboundDetailMapper.selectByDetailNo(detailNo);
        if (detail != null) {
            detail.setPickedQty(detail.getPickedQty().add(pickQty));
            if (detail.getPickedQty().compareTo(detail.getAllocatedQty()) >= 0) {
                detail.setStatus("PICKED");
            } else {
                detail.setStatus("PICKING");
            }
            outboundDetailMapper.updateById(detail);
        }

        // 保存拣货记录
        PickRecord record = new PickRecord();
        record.setRecordNo(generatePickNo());
        record.setOutboundNo(outboundNo);
        record.setDetailNo(detailNo);
        record.setWaveNo(order.getWaveNo());
        record.setSkuCode(skuCode);
        record.setBatchNo(batchNo);
        record.setFromLocation(fromLocation);
        record.setPickQty(pickQty);
        record.setPickType(pickType != null ? pickType : "NORMAL");
        record.setDifferenceQty(BigDecimal.ZERO);
        record.setOperator(operator);
        record.setPickTime(LocalDateTime.now());
        pickRecordMapper.insert(record);

        // 检查整单拣货完成
        checkPickComplete(outboundNo);

        log.info(
                "拣货完成: outbound={}, detail={}, location={}, qty={}",
                outboundNo,
                detailNo,
                fromLocation,
                pickQty);
        return record;
    }

    /** 检查拣货是否完成 */
    private void checkPickComplete(String outboundNo) {
        List<OutboundDetail> details = outboundDetailMapper.selectByOutboundNo(outboundNo);
        boolean allPicked = details.stream().allMatch(d -> "PICKED".equals(d.getStatus()));
        if (allPicked) {
            OutboundOrder order = outboundOrderMapper.selectByOutboundNo(outboundNo);
            order.setStatus(OutboundStatus.PICKED.getCode());
            outboundOrderMapper.updateById(order);
            log.info("出库单拣货完成: {}", outboundNo);
        }
    }

    public List<PickRecord> getPickRecords(String outboundNo) {
        return pickRecordMapper.selectByOutboundNo(outboundNo);
    }

    // ============================================================

    // 4. 复核打包
    // ============================================================

    /** 执行打包 */
    @Transactional(rollbackFor = Exception.class)
    public OutboundOrder pack(
            String outboundNo,
            BigDecimal packQty,
            Integer packageCount,
            BigDecimal weight,
            BigDecimal volume,
            String operator) {
        OutboundOrder order = outboundOrderMapper.selectByOutboundNo(outboundNo);
        if (order == null) throw new RuntimeException("出库单不存在: " + outboundNo);

        if (OutboundStatus.PICKED.getCode().equals(order.getStatus())) {
            order.setStatus(OutboundStatus.PACKING.getCode());
        }
        order.setPackedQty(order.getPackedQty().add(packQty));
        outboundOrderMapper.updateById(order);

        // 更新明细
        List<OutboundDetail> details = outboundDetailMapper.selectByOutboundNo(outboundNo);
        for (OutboundDetail detail : details) {
            detail.setPackedQty(detail.getPickedQty());
            detail.setStatus("PACKED");
            outboundDetailMapper.updateById(detail);
        }

        // 检查整单打包完成
        if (order.getPackedQty().compareTo(order.getPickedQty()) >= 0) {
            order.setStatus(OutboundStatus.PACKED.getCode());
            outboundOrderMapper.updateById(order);
        }

        log.info("打包完成: outbound={}, packQty={}, packages={}", outboundNo, packQty, packageCount);
        return order;
    }

    // ============================================================

    // 5. 发运管理
    // ============================================================

    /** 执行发运 */
    @Transactional(rollbackFor = Exception.class)
    public ShipRecord ship(
            String outboundNo,
            String carrier,
            String trackingNo,
            BigDecimal shipQty,
            Integer packageCount,
            BigDecimal weight,
            BigDecimal volume,
            String operator) {
        OutboundOrder order = outboundOrderMapper.selectByOutboundNo(outboundNo);
        if (order == null) throw new RuntimeException("出库单不存在: " + outboundNo);

        if (OutboundStatus.PACKED.getCode().equals(order.getStatus())) {
            order.setStatus(OutboundStatus.SHIPPING.getCode());
        }
        order.setShippedQty(order.getShippedQty().add(shipQty));
        order.setCarrier(carrier);
        order.setTrackingNo(trackingNo);
        order.setActualShipTime(LocalDateTime.now());
        outboundOrderMapper.updateById(order);

        // 更新明细：核销预占扣减（总量减+预占核销，可用不动——预占时已减过可用）
        List<OutboundDetail> details = outboundDetailMapper.selectByOutboundNo(outboundNo);
        String shipOwner = order.getOwnerCodeCol();
        for (OutboundDetail detail : details) {
            BigDecimal allocated = detail.getAllocatedQty();
            if (allocated != null
                    && allocated.compareTo(BigDecimal.ZERO) > 0
                    && detail.getWarehouseCode() != null) {
                inventoryService.deductAllocatedInventory(
                        detail.getWarehouseCode(),
                        detail.getLocationCode(),
                        detail.getSkuCode(),
                        detail.getBatchNo(),
                        shipOwner,
                        allocated,
                        "OUTBOUND",
                        outboundNo,
                        operator);
            }
            detail.setShippedQty(detail.getPackedQty());
            detail.setStatus("SHIPPED");
            outboundDetailMapper.updateById(detail);
        }

        // 保存发运记录
        ShipRecord record = new ShipRecord();
        record.setRecordNo(generateShipNo());
        record.setOutboundNo(outboundNo);
        record.setCarrier(carrier);
        record.setTrackingNo(trackingNo);
        record.setShipQty(shipQty);
        record.setPackageCount(packageCount);
        record.setWeight(weight);
        record.setVolume(volume);
        record.setShipStatus("SHIPPED");
        record.setOperator(operator);
        record.setShipTime(LocalDateTime.now());
        shipRecordMapper.insert(record);

        // 检查整单发运完成
        if (order.getShippedQty().compareTo(order.getPackedQty()) >= 0) {
            order.setStatus(OutboundStatus.SHIPPED.getCode());
            outboundOrderMapper.updateById(order);
            log.info("出库单发运完成: {}", outboundNo);
        }

        log.info("发运完成: outbound={}, carrier={}, trackingNo={}", outboundNo, carrier, trackingNo);
        return record;
    }

    public List<ShipRecord> getShipRecords(String outboundNo) {
        return shipRecordMapper.selectByOutboundNo(outboundNo);
    }

    // ============================================================

    // 工具方法
    // ============================================================

    private String generateOutboundNo() {
        return "OUT"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }

    private String generateDetailNo() {
        return "OD"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }

    private String generatePickNo() {
        return "PCK"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }

    private String generateShipNo() {
        return "SHP"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }
}
