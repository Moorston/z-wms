package com.xwms.core.multiwms.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.common.exception.BizException;
import com.xwms.core.multiwms.entity.*;
import com.xwms.core.multiwms.enums.AllocationStatus;
import com.xwms.core.multiwms.enums.TransferStatus;
import com.xwms.core.multiwms.mapper.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 多仓协同核心服务 包含: 仓库间调拨/多仓库存查询/订单智能分配 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MultiWarehouseService {

    private final TransferOrderMapper transferOrderMapper;
    private final TransferDetailMapper transferDetailMapper;
    private final MultiWarehouseStockMapper stockMapper;
    private final OrderAllocationMapper allocationMapper;

    private static final AtomicInteger TRANSFER_SEQ = new AtomicInteger(0);
    private static final AtomicInteger ALLOC_SEQ = new AtomicInteger(0);
    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    // ============================================================

    // 1. 仓库间调拨管理
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public TransferOrder createTransfer(TransferOrder order, List<TransferDetail> details) {
        order.setTransferNo(generateTransferNo());
        order.setStatus(TransferStatus.DRAFT.getCode());
        order.setShippedQty(BigDecimal.ZERO);
        order.setReceivedQty(BigDecimal.ZERO);
        order.setTotalSku(details.size());
        order.setTotalQty(
                details.stream()
                        .map(d -> d.getPlannedQty() != null ? d.getPlannedQty() : BigDecimal.ZERO)
                        .reduce(BigDecimal.ZERO, BigDecimal::add));
        transferOrderMapper.insert(order);

        for (TransferDetail detail : details) {
            detail.setTransferId(order.getId());
            detail.setTransferNo(order.getTransferNo());
            detail.setShippedQty(BigDecimal.ZERO);
            detail.setReceivedQty(BigDecimal.ZERO);
            detail.setDifferenceQty(BigDecimal.ZERO);
            detail.setOwnerCodeCol(order.getOwnerCodeCol());
            transferDetailMapper.insert(detail);
        }

        log.info(
                "创建调拨单: {}, {} -> {}",
                order.getTransferNo(),
                order.getFromWarehouse(),
                order.getToWarehouse());
        return order;
    }

    public TransferOrder getTransfer(Long id) {
        TransferOrder order = transferOrderMapper.selectById(id);
        if (order == null) throw new BizException("调拨单不存在: " + id);
        return order;
    }

    public Page<TransferOrder> pageTransfers(
            Page<TransferOrder> page, String fromWarehouse, String toWarehouse, String status) {
        LambdaQueryWrapper<TransferOrder> wrapper = new LambdaQueryWrapper<>();
        if (fromWarehouse != null) wrapper.eq(TransferOrder::getFromWarehouse, fromWarehouse);
        if (toWarehouse != null) wrapper.eq(TransferOrder::getToWarehouse, toWarehouse);
        if (status != null) wrapper.eq(TransferOrder::getStatus, status);
        wrapper.orderByDesc(TransferOrder::getCreatedTime);
        return transferOrderMapper.selectPage(page, wrapper);
    }

    public List<TransferDetail> getTransferDetails(Long transferId) {
        return transferDetailMapper.selectByTransferId(transferId);
    }

    /** 确认调拨单 (发出仓确认, 库存预占) */
    @Transactional(rollbackFor = Exception.class)
    public TransferOrder confirmTransfer(Long transferId, String confirmedBy) {
        TransferOrder order = getTransfer(transferId);
        if (!TransferStatus.DRAFT.getCode().equals(order.getStatus())) {
            throw new BizException("调拨单状态不允许确认: " + order.getStatus());
        }
        order.setStatus(TransferStatus.CONFIRMED.getCode());
        order.setConfirmedBy(confirmedBy);
        order.setConfirmedTime(LocalDateTime.now());
        transferOrderMapper.updateById(order);
        log.info("确认调拨单: {}", order.getTransferNo());
        return order;
    }

    /** 调拨出库 (发出仓发货) */
    @Transactional(rollbackFor = Exception.class)
    public TransferOrder shipTransfer(Long transferId, List<TransferDetail> shippedDetails) {
        TransferOrder order = getTransfer(transferId);
        if (!TransferStatus.CONFIRMED.getCode().equals(order.getStatus())) {
            throw new BizException("调拨单状态不允许发货: " + order.getStatus());
        }

        BigDecimal totalShipped = BigDecimal.ZERO;
        for (TransferDetail shipped : shippedDetails) {
            TransferDetail detail = transferDetailMapper.selectById(shipped.getId());
            if (detail == null) continue;
            detail.setShippedQty(shipped.getShippedQty());
            transferDetailMapper.updateById(detail);
            totalShipped =
                    totalShipped.add(
                            shipped.getShippedQty() != null
                                    ? shipped.getShippedQty()
                                    : BigDecimal.ZERO);
        }

        order.setShippedQty(totalShipped);
        order.setActualShipDate(LocalDate.now());
        order.setStatus(TransferStatus.IN_TRANSIT.getCode());
        transferOrderMapper.updateById(order);

        log.info("调拨出库: {}, 发货数量={}", order.getTransferNo(), totalShipped);
        return order;
    }

    /** 调拨入库 (目的仓收货) */
    @Transactional(rollbackFor = Exception.class)
    public TransferOrder receiveTransfer(Long transferId, List<TransferDetail> receivedDetails) {
        TransferOrder order = getTransfer(transferId);
        if (!TransferStatus.IN_TRANSIT.getCode().equals(order.getStatus())) {
            throw new BizException("调拨单状态不允许收货: " + order.getStatus());
        }

        BigDecimal totalReceived = BigDecimal.ZERO;
        for (TransferDetail received : receivedDetails) {
            TransferDetail detail = transferDetailMapper.selectById(received.getId());
            if (detail == null) continue;
            detail.setReceivedQty(received.getReceivedQty());
            // 计算差异
            BigDecimal diff =
                    detail.getShippedQty()
                            .subtract(
                                    received.getReceivedQty() != null
                                            ? received.getReceivedQty()
                                            : BigDecimal.ZERO);
            detail.setDifferenceQty(diff);
            transferDetailMapper.updateById(detail);
            totalReceived =
                    totalReceived.add(
                            received.getReceivedQty() != null
                                    ? received.getReceivedQty()
                                    : BigDecimal.ZERO);
        }

        order.setReceivedQty(totalReceived);
        order.setActualArrivalDate(LocalDate.now());
        order.setStatus(TransferStatus.RECEIVED.getCode());
        transferOrderMapper.updateById(order);

        log.info("调拨入库: {}, 收货数量={}", order.getTransferNo(), totalReceived);
        return order;
    }

    /** 完成调拨单 */
    @Transactional(rollbackFor = Exception.class)
    public TransferOrder completeTransfer(Long transferId) {
        TransferOrder order = getTransfer(transferId);
        if (!TransferStatus.RECEIVED.getCode().equals(order.getStatus())) {
            throw new BizException("调拨单状态不允许完成: " + order.getStatus());
        }
        order.setStatus(TransferStatus.COMPLETED.getCode());
        transferOrderMapper.updateById(order);
        log.info("完成调拨单: {}", order.getTransferNo());
        return order;
    }

    /** 取消调拨单 */
    @Transactional(rollbackFor = Exception.class)
    public TransferOrder cancelTransfer(Long transferId, String remark) {
        TransferOrder order = getTransfer(transferId);
        if (!TransferStatus.DRAFT.getCode().equals(order.getStatus())
                && !TransferStatus.CONFIRMED.getCode().equals(order.getStatus())) {
            throw new BizException("调拨单状态不允许取消: " + order.getStatus());
        }
        order.setStatus(TransferStatus.CANCELLED.getCode());
        order.setRemark(remark);
        transferOrderMapper.updateById(order);
        log.info("取消调拨单: {}", order.getTransferNo());
        return order;
    }

    // ============================================================

    // 2. 多仓库存查询
    // ============================================================

    /** 查询SKU在所有仓库的库存 */
    public List<MultiWarehouseStock> getStockBySku(String sku) {
        return stockMapper.selectBySku(sku);
    }

    /** 查询仓库库存 */
    public List<MultiWarehouseStock> getStockByWarehouse(String warehouse) {
        return stockMapper.selectByWarehouse(warehouse);
    }

    /** 查询SKU可用库存总量(所有仓) */
    public BigDecimal getTotalAvailableQty(String sku) {
        List<MultiWarehouseStock> stocks = stockMapper.selectBySku(sku);
        return stocks.stream()
                .map(s -> s.getAvailableQty() != null ? s.getAvailableQty() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** 同步库存快照 (PowerJob定时触发) */
    @Transactional(rollbackFor = Exception.class)
    public void syncStockSnapshot(
            String warehouse,
            String sku,
            BigDecimal available,
            BigDecimal allocated,
            BigDecimal picking,
            BigDecimal inTransit,
            BigDecimal frozen,
            BigDecimal safetyStock) {
        MultiWarehouseStock stock =
                stockMapper.selectOne(
                        new LambdaQueryWrapper<MultiWarehouseStock>()
                                .eq(MultiWarehouseStock::getWarehouseCode, warehouse)
                                .eq(MultiWarehouseStock::getSku, sku));

        BigDecimal total = available.add(allocated).add(picking).add(frozen);

        if (stock == null) {
            stock = new MultiWarehouseStock();
            stock.setWarehouseCode(warehouse);
            stock.setSku(sku);
            stock.setAvailableQty(available);
            stock.setAllocatedQty(allocated);
            stock.setPickingQty(picking);
            stock.setInTransitQty(inTransit);
            stock.setFrozenQty(frozen);
            stock.setTotalQty(total);
            stock.setSafetyStock(safetyStock);
            stock.setLastSyncTime(LocalDateTime.now());
            stockMapper.insert(stock);
        } else {
            stock.setAvailableQty(available);
            stock.setAllocatedQty(allocated);
            stock.setPickingQty(picking);
            stock.setInTransitQty(inTransit);
            stock.setFrozenQty(frozen);
            stock.setTotalQty(total);
            stock.setSafetyStock(safetyStock);
            stock.setLastSyncTime(LocalDateTime.now());
            stockMapper.updateById(stock);
        }
    }

    // ============================================================

    // 3. 订单智能分配 (核心)
    // ============================================================

    /** 分配订单到仓库 策略: 1.就近分配(收货地址) 2.库存充足优先 3.配送成本最低 4.优先级高的仓优先 */
    @Transactional(rollbackFor = Exception.class)
    public OrderAllocation allocateOrder(
            String orderNo,
            String orderType,
            String sku,
            String productName,
            BigDecimal requiredQty,
            String customerAddress,
            String priority,
            String ownerCode) {
        // 1. 查询该SKU所有仓库库存
        List<MultiWarehouseStock> stocks = stockMapper.selectBySku(sku);
        if (stocks.isEmpty()) {
            return createFailedAllocation(
                    orderNo, orderType, sku, productName, requiredQty, "所有仓库无库存", ownerCode);
        }

        // 2. 筛选可用库存充足的仓库
        List<MultiWarehouseStock> available =
                stocks.stream()
                        .filter(
                                s ->
                                        s.getAvailableQty() != null
                                                && s.getAvailableQty().compareTo(requiredQty) >= 0)
                        .sorted(
                                Comparator.comparing(MultiWarehouseStock::getAvailableQty)
                                        .reversed())
                        .toList();

        if (available.isEmpty()) {
            // 3. 部分分配: 找库存最多的仓
            MultiWarehouseStock best =
                    stocks.stream()
                            .max(
                                    Comparator.comparing(
                                            s ->
                                                    s.getAvailableQty() != null
                                                            ? s.getAvailableQty()
                                                            : BigDecimal.ZERO))
                            .orElse(null);
            if (best == null || best.getAvailableQty().compareTo(BigDecimal.ZERO) <= 0) {
                return createFailedAllocation(
                        orderNo, orderType, sku, productName, requiredQty, "所有仓库无可用库存", ownerCode);
            }
            return createPartialAllocation(
                    orderNo,
                    orderType,
                    sku,
                    productName,
                    requiredQty,
                    best.getAvailableQty(),
                    best.getWarehouseCode(),
                    "库存不足,部分分配",
                    ownerCode);
        }

        // 4. 选择最优仓库(简化: 库存最多的仓, 实际应结合地址距离、配送成本)
        MultiWarehouseStock best = available.get(0);

        OrderAllocation allocation = new OrderAllocation();
        allocation.setAllocationNo(generateAllocNo());
        allocation.setOrderNo(orderNo);
        allocation.setOrderType(orderType);
        allocation.setSku(sku);
        allocation.setProductName(productName);
        allocation.setRequiredQty(requiredQty);
        allocation.setAllocatedQty(requiredQty);
        allocation.setWarehouseCode(best.getWarehouseCode());
        allocation.setAllocationRule("STOCK_PRIORITY");
        allocation.setAllocationReason("库存充足仓库优先");
        allocation.setStatus(AllocationStatus.ALLOCATED.getCode());
        allocation.setPriority(priority != null ? priority : "NORMAL");
        allocation.setCustomerAddress(customerAddress);
        allocation.setOwnerCodeCol(ownerCode);
        allocationMapper.insert(allocation);

        log.info(
                "订单分配: {}, SKU={}, 数量={}, 仓库={}",
                orderNo,
                sku,
                requiredQty,
                best.getWarehouseCode());
        return allocation;
    }

    private OrderAllocation createFailedAllocation(
            String orderNo,
            String orderType,
            String sku,
            String productName,
            BigDecimal requiredQty,
            String reason,
            String ownerCode) {
        OrderAllocation allocation = new OrderAllocation();
        allocation.setAllocationNo(generateAllocNo());
        allocation.setOrderNo(orderNo);
        allocation.setOrderType(orderType);
        allocation.setSku(sku);
        allocation.setProductName(productName);
        allocation.setRequiredQty(requiredQty);
        allocation.setAllocatedQty(BigDecimal.ZERO);
        allocation.setAllocationReason(reason);
        allocation.setStatus(AllocationStatus.FAILED.getCode());
        allocation.setOwnerCodeCol(ownerCode);
        allocationMapper.insert(allocation);
        log.warn("订单分配失败: {}, SKU={}, 原因={}", orderNo, sku, reason);
        return allocation;
    }

    private OrderAllocation createPartialAllocation(
            String orderNo,
            String orderType,
            String sku,
            String productName,
            BigDecimal requiredQty,
            BigDecimal allocatedQty,
            String warehouse,
            String reason,
            String ownerCode) {
        OrderAllocation allocation = new OrderAllocation();
        allocation.setAllocationNo(generateAllocNo());
        allocation.setOrderNo(orderNo);
        allocation.setOrderType(orderType);
        allocation.setSku(sku);
        allocation.setProductName(productName);
        allocation.setRequiredQty(requiredQty);
        allocation.setAllocatedQty(allocatedQty);
        allocation.setWarehouseCode(warehouse);
        allocation.setAllocationRule("PARTIAL");
        allocation.setAllocationReason(reason);
        allocation.setStatus(AllocationStatus.PARTIAL.getCode());
        allocation.setOwnerCodeCol(ownerCode);
        allocationMapper.insert(allocation);
        log.info(
                "订单部分分配: {}, SKU={}, 需求={}, 分配={}, 仓库={}",
                orderNo,
                sku,
                requiredQty,
                allocatedQty,
                warehouse);
        return allocation;
    }

    public List<OrderAllocation> getOrderAllocations(String orderNo) {
        return allocationMapper.selectByOrderNo(orderNo);
    }

    public List<OrderAllocation> getPendingAllocations(String warehouse) {
        return allocationMapper.selectPendingByWarehouse(warehouse);
    }

    @Transactional(rollbackFor = Exception.class)
    public void markAllocationShipped(Long allocationId) {
        OrderAllocation allocation = allocationMapper.selectById(allocationId);
        if (allocation == null) throw new BizException("分配记录不存在");
        allocation.setStatus(AllocationStatus.SHIPPED.getCode());
        allocationMapper.updateById(allocation);
    }

    // ============================================================

    // 工具方法
    // ============================================================

    private String generateTransferNo() {
        return "TR"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", TRANSFER_SEQ.incrementAndGet() % 1000);
    }

    private String generateAllocNo() {
        return "AL"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", ALLOC_SEQ.incrementAndGet() % 1000);
    }
}
