package com.xwms.core.transfer.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.core.transfer.entity.*;
import com.xwms.core.transfer.mapper.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 调拨管理核心服务 核心能力: 调拨创建/审批/发运/在途库存/收货 调拨特点: 多仓之间库存转移，在途库存独立管理 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransferService {

    private final TransferMapper transferMapper;
    private final TransferDetailMapper detailMapper;
    private final TransferInTransitMapper inTransitMapper;
    private final TransferTaskMapper taskMapper;

    private static final AtomicInteger SEQ = new AtomicInteger(0);
    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    // ============================================================

    // 1. 调拨单创建
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public Transfer createTransfer(
            Transfer transfer, List<TransferDetail> details, String operator) {
        if (transfer.getTransferNo() == null) {
            transfer.setTransferNo(generateTransferNo());
        }
        if (transfer.getStatus() == null) transfer.setStatus("CREATED");
        if (transfer.getTotalQty() == null) transfer.setTotalQty(BigDecimal.ZERO);
        if (transfer.getShippedQty() == null) transfer.setShippedQty(BigDecimal.ZERO);
        if (transfer.getReceivedQty() == null) transfer.setReceivedQty(BigDecimal.ZERO);
        if (transfer.getDifferenceQty() == null) transfer.setDifferenceQty(BigDecimal.ZERO);
        transfer.setCreatedBy(operator);
        transferMapper.insert(transfer);

        // 保存明细
        if (details != null) {
            for (int i = 0; i < details.size(); i++) {
                TransferDetail detail = details.get(i);
                detail.setTransferNo(transfer.getTransferNo());
                detail.setLineNo(i + 1);
                if (detail.getExpectedQty() == null) detail.setExpectedQty(BigDecimal.ZERO);
                if (detail.getShippedQty() == null) detail.setShippedQty(BigDecimal.ZERO);
                if (detail.getReceivedQty() == null) detail.setReceivedQty(BigDecimal.ZERO);
                if (detail.getDifferenceQty() == null) detail.setDifferenceQty(BigDecimal.ZERO);
                if (detail.getStatus() == null) detail.setStatus("PENDING");
                detailMapper.insert(detail);
            }
            transfer.setTotalQty(
                    details.stream()
                            .map(TransferDetail::getExpectedQty)
                            .reduce(BigDecimal.ZERO, BigDecimal::add));
            transferMapper.updateById(transfer);
        }

        log.info(
                "创建调拨单: {}={}, {}->{}, 明细{}行",
                transfer.getTransferNo(),
                transfer.getTransferType(),
                transfer.getFromWarehouse(),
                transfer.getToWarehouse(),
                details != null ? details.size() : 0);
        return transfer;
    }

    // ============================================================

    // 2. 调拨审批
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public Transfer approveTransfer(String transferNo, String approver) {
        Transfer transfer = transferMapper.selectByTransferNo(transferNo);
        if (transfer == null) throw new RuntimeException("调拨单不存在: " + transferNo);
        if (!"CREATED".equals(transfer.getStatus())) {
            throw new RuntimeException("调拨单状态不正确: " + transfer.getStatus());
        }

        transfer.setStatus("APPROVED");
        transfer.setApprover(approver);
        transfer.setApproveTime(LocalDateTime.now());
        transferMapper.updateById(transfer);

        log.info("审批调拨单: {}, 审批人={}", transferNo, approver);
        return transfer;
    }

    // ============================================================

    // 3. 调拨发运（调出仓库扣减库存，生成在途库存）
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public TransferTask shipTransfer(
            String transferNo,
            String skuCode,
            String batchNo,
            String fromLocation,
            BigDecimal shipQty,
            String carrier,
            String trackingNo,
            String operator) {
        Transfer transfer = transferMapper.selectByTransferNo(transferNo);
        if (transfer == null) throw new RuntimeException("调拨单不存在: " + transferNo);
        if (!"APPROVED".equals(transfer.getStatus()) && !"SHIPPED".equals(transfer.getStatus())) {
            throw new RuntimeException("调拨单状态不正确: " + transfer.getStatus());
        }

        // 更新调拨单发运数量
        transfer.setShippedQty(transfer.getShippedQty().add(shipQty));
        transfer.setCarrier(carrier);
        transfer.setTrackingNo(trackingNo);
        transfer.setShipTime(LocalDateTime.now());
        if ("APPROVED".equals(transfer.getStatus())) {
            transfer.setStatus("SHIPPED");
        }
        transferMapper.updateById(transfer);

        // 更新明细发运数量
        List<TransferDetail> details = detailMapper.selectByTransferNo(transferNo);
        for (TransferDetail detail : details) {
            if (skuCode.equals(detail.getSkuCode())) {
                detail.setShippedQty(detail.getShippedQty().add(shipQty));
                detail.setStatus("SHIPPED");
                detailMapper.updateById(detail);
                break;
            }
        }

        // 生成在途库存
        TransferInTransit inTransit = new TransferInTransit();
        inTransit.setTransferNo(transferNo);
        inTransit.setSkuCode(skuCode);
        inTransit.setBatchNo(batchNo);
        inTransit.setFromWarehouse(transfer.getFromWarehouse());
        inTransit.setToWarehouse(transfer.getToWarehouse());
        inTransit.setInTransitQty(shipQty);
        inTransit.setReceivedQty(BigDecimal.ZERO);
        inTransit.setStatus("IN_TRANSIT");
        inTransit.setShipTime(LocalDateTime.now());
        inTransit.setExpectedArrival(transfer.getExpectedArrival());
        inTransitMapper.insert(inTransit);

        // 创建发运作业记录
        TransferTask task = new TransferTask();
        task.setTaskNo(generateTaskNo());
        task.setTransferNo(transferNo);
        task.setTaskType("SHIP");
        task.setSkuCode(skuCode);
        task.setBatchNo(batchNo);
        task.setFromLocation(fromLocation);
        task.setToLocation("IN_TRANSIT");
        task.setTaskQty(shipQty);
        task.setDoneQty(shipQty);
        task.setOperator(operator);
        task.setStatus("DONE");
        task.setStartTime(LocalDateTime.now());
        task.setEndTime(LocalDateTime.now());
        taskMapper.insert(task);

        // 检查是否全部发运
        checkShipComplete(transferNo);

        log.info("调拨发运: {}, sku={}, qty={}, carrier={}", transferNo, skuCode, shipQty, carrier);
        return task;
    }

    /** 检查是否全部发运 */
    private void checkShipComplete(String transferNo) {
        Transfer transfer = transferMapper.selectByTransferNo(transferNo);
        if (transfer.getShippedQty().compareTo(transfer.getTotalQty()) >= 0) {
            transfer.setStatus("IN_TRANSIT");
            transferMapper.updateById(transfer);
            log.info("调拨全部发运，进入在途: {}", transferNo);
        }
    }

    // ============================================================

    // 4. 在途库存管理
    // ============================================================

    public Page<TransferInTransit> pageInTransit(
            Page<TransferInTransit> page,
            String skuCode,
            String fromWarehouse,
            String toWarehouse,
            String status) {
        LambdaQueryWrapper<TransferInTransit> wrapper = new LambdaQueryWrapper<>();
        if (skuCode != null) wrapper.eq(TransferInTransit::getSkuCode, skuCode);
        if (fromWarehouse != null) wrapper.eq(TransferInTransit::getFromWarehouse, fromWarehouse);
        if (toWarehouse != null) wrapper.eq(TransferInTransit::getToWarehouse, toWarehouse);
        if (status != null) wrapper.eq(TransferInTransit::getStatus, status);
        wrapper.orderByDesc(TransferInTransit::getShipTime);
        return inTransitMapper.selectPage(page, wrapper);
    }

    public List<TransferInTransit> getInTransitByTransfer(String transferNo) {
        return inTransitMapper.selectByTransferNo(transferNo);
    }

    public List<TransferInTransit> getIncomingByWarehouse(String warehouseCode) {
        return inTransitMapper.selectIncomingByWarehouse(warehouseCode);
    }

    // ============================================================

    // 5. 调拨收货（调入仓库增加库存，扣减在途库存）
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public TransferTask receiveTransfer(
            String transferNo,
            String skuCode,
            String batchNo,
            String toLocation,
            BigDecimal receivedQty,
            BigDecimal differenceQty,
            String operator) {
        Transfer transfer = transferMapper.selectByTransferNo(transferNo);
        if (transfer == null) throw new RuntimeException("调拨单不存在: " + transferNo);
        if (!"IN_TRANSIT".equals(transfer.getStatus())
                && !"RECEIVED".equals(transfer.getStatus())) {
            throw new RuntimeException("调拨单状态不正确: " + transfer.getStatus());
        }

        // 更新调拨单收货数量
        transfer.setReceivedQty(transfer.getReceivedQty().add(receivedQty));
        transfer.setDifferenceQty(
                transfer.getDifferenceQty()
                        .add(differenceQty != null ? differenceQty : BigDecimal.ZERO));
        transfer.setReceiveTime(LocalDateTime.now());
        if ("IN_TRANSIT".equals(transfer.getStatus())) {
            transfer.setStatus("RECEIVED");
        }
        transferMapper.updateById(transfer);

        // 更新明细收货数量
        List<TransferDetail> details = detailMapper.selectByTransferNo(transferNo);
        for (TransferDetail detail : details) {
            if (skuCode.equals(detail.getSkuCode())) {
                detail.setReceivedQty(detail.getReceivedQty().add(receivedQty));
                detail.setDifferenceQty(
                        detail.getDifferenceQty()
                                .add(differenceQty != null ? differenceQty : BigDecimal.ZERO));
                detail.setStatus("RECEIVED");
                detailMapper.updateById(detail);
                break;
            }
        }

        // 扣减在途库存
        List<TransferInTransit> inTransits = inTransitMapper.selectByTransferNo(transferNo);
        for (TransferInTransit inTransit : inTransits) {
            if (skuCode.equals(inTransit.getSkuCode())
                    && "IN_TRANSIT".equals(inTransit.getStatus())) {
                inTransit.setReceivedQty(inTransit.getReceivedQty().add(receivedQty));
                if (inTransit.getReceivedQty().compareTo(inTransit.getInTransitQty()) >= 0) {
                    inTransit.setStatus("RECEIVED");
                    inTransit.setReceiveTime(LocalDateTime.now());
                } else {
                    inTransit.setStatus("PARTIAL_RECEIVED");
                }
                inTransitMapper.updateById(inTransit);
                break;
            }
        }

        // 创建收货作业记录
        TransferTask task = new TransferTask();
        task.setTaskNo(generateTaskNo());
        task.setTransferNo(transferNo);
        task.setTaskType("RECEIVE");
        task.setSkuCode(skuCode);
        task.setBatchNo(batchNo);
        task.setFromLocation("IN_TRANSIT");
        task.setToLocation(toLocation);
        task.setTaskQty(receivedQty);
        task.setDoneQty(receivedQty);
        task.setOperator(operator);
        task.setStatus("DONE");
        task.setStartTime(LocalDateTime.now());
        task.setEndTime(LocalDateTime.now());
        taskMapper.insert(task);

        // 检查是否全部收货
        checkReceiveComplete(transferNo);

        log.info(
                "调拨收货: {}, sku={}, qty={}, diff={}",
                transferNo,
                skuCode,
                receivedQty,
                differenceQty);
        return task;
    }

    /** 检查是否全部收货 */
    private void checkReceiveComplete(String transferNo) {
        Transfer transfer = transferMapper.selectByTransferNo(transferNo);
        if (transfer.getReceivedQty().compareTo(transfer.getShippedQty()) >= 0) {
            transfer.setStatus("DONE");
            transferMapper.updateById(transfer);
            log.info("调拨全部收货完成: {}", transferNo);
        }
    }

    // ============================================================

    // 6. 查询
    // ============================================================

    public Page<Transfer> pageTransfers(
            Page<Transfer> page,
            String transferType,
            String status,
            String fromWarehouse,
            String toWarehouse) {
        LambdaQueryWrapper<Transfer> wrapper = new LambdaQueryWrapper<>();
        if (transferType != null) wrapper.eq(Transfer::getTransferType, transferType);
        if (status != null) wrapper.eq(Transfer::getStatus, status);
        if (fromWarehouse != null) wrapper.eq(Transfer::getFromWarehouse, fromWarehouse);
        if (toWarehouse != null) wrapper.eq(Transfer::getToWarehouse, toWarehouse);
        wrapper.orderByDesc(Transfer::getCreatedTime);
        return transferMapper.selectPage(page, wrapper);
    }

    public Transfer getTransferByNo(String transferNo) {
        return transferMapper.selectByTransferNo(transferNo);
    }

    public List<TransferDetail> getDetails(String transferNo) {
        return detailMapper.selectByTransferNo(transferNo);
    }

    public List<TransferTask> getTasks(String transferNo) {
        return taskMapper.selectByTransferNo(transferNo);
    }

    // ============================================================

    // 工具方法
    // ============================================================

    private String generateTransferNo() {
        return "TR"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }

    private String generateTaskNo() {
        return "TRT"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }
}
