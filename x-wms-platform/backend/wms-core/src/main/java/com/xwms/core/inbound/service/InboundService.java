package com.xwms.core.inbound.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.core.inbound.entity.*;
import com.xwms.core.inbound.enums.InboundStatus;
import com.xwms.core.inbound.mapper.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 入库管理核心服务 包含: ASN管理/入库单管理/收货管理/上架管理 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InboundService {

    private final AsnMapper asnMapper;
    private final InboundOrderMapper inboundOrderMapper;
    private final InboundDetailMapper inboundDetailMapper;
    private final ReceiveRecordMapper receiveRecordMapper;

    private static final AtomicInteger SEQ = new AtomicInteger(0);
    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    // ============================================================

    // 1. ASN管理
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public Asn createAsn(Asn asn) {
        if (asn.getAsnNo() == null) {
            asn.setAsnNo(generateAsnNo());
        }
        if (asn.getStatus() == null) asn.setStatus("CREATED");
        if (asn.getReceivedQty() == null) asn.setReceivedQty(BigDecimal.ZERO);
        asnMapper.insert(asn);
        log.info("创建ASN: {}={}", asn.getAsnNo(), asn.getAsnType());
        return asn;
    }

    @Transactional(rollbackFor = Exception.class)
    public Asn updateAsn(Asn asn) {
        asnMapper.updateById(asn);
        return asn;
    }

    public Page<Asn> pageAsns(
            Page<Asn> page,
            String asnType,
            String status,
            String supplierCode,
            String warehouseCode) {
        LambdaQueryWrapper<Asn> wrapper = new LambdaQueryWrapper<>();
        if (asnType != null) wrapper.eq(Asn::getAsnType, asnType);
        if (status != null) wrapper.eq(Asn::getStatus, status);
        if (supplierCode != null) wrapper.eq(Asn::getSupplierCode, supplierCode);
        if (warehouseCode != null) wrapper.eq(Asn::getWarehouseCode, warehouseCode);
        wrapper.orderByDesc(Asn::getCreatedTime);
        return asnMapper.selectPage(page, wrapper);
    }

    public Asn getAsnByNo(String asnNo) {
        return asnMapper.selectByAsnNo(asnNo);
    }

    /** ASN到货确认 */
    @Transactional(rollbackFor = Exception.class)
    public Asn confirmAsnArrival(String asnNo) {
        Asn asn = asnMapper.selectByAsnNo(asnNo);
        if (asn == null) throw new RuntimeException("ASN不存在: " + asnNo);
        asn.setStatus("RECEIVING");
        asn.setActualDate(LocalDateTime.now());
        asnMapper.updateById(asn);
        log.info("ASN到货确认: {}", asnNo);
        return asn;
    }

    // ============================================================

    // 2. 入库单管理
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public InboundOrder createInboundOrder(InboundOrder order, List<InboundDetail> details) {
        if (order.getInboundNo() == null) {
            order.setInboundNo(generateInboundNo());
        }
        if (order.getStatus() == null) order.setStatus(InboundStatus.CREATED.getCode());
        if (order.getReceivedQty() == null) order.setReceivedQty(BigDecimal.ZERO);
        if (order.getPutawayQty() == null) order.setPutawayQty(BigDecimal.ZERO);
        inboundOrderMapper.insert(order);

        // 保存明细
        if (details != null) {
            for (int i = 0; i < details.size(); i++) {
                InboundDetail detail = details.get(i);
                detail.setInboundNo(order.getInboundNo());
                detail.setLineNo(i + 1);
                detail.setDetailNo(generateDetailNo());
                if (detail.getReceivedQty() == null) detail.setReceivedQty(BigDecimal.ZERO);
                if (detail.getPutawayQty() == null) detail.setPutawayQty(BigDecimal.ZERO);
                if (detail.getStatus() == null) detail.setStatus("CREATED");
                inboundDetailMapper.insert(detail);
            }
        }
        log.info(
                "创建入库单: {}={}, 明细{}行",
                order.getInboundNo(),
                order.getInboundType(),
                details != null ? details.size() : 0);
        return order;
    }

    @Transactional(rollbackFor = Exception.class)
    public InboundOrder updateInboundOrder(InboundOrder order) {
        inboundOrderMapper.updateById(order);
        return order;
    }

    public Page<InboundOrder> pageInboundOrders(
            Page<InboundOrder> page,
            String inboundType,
            String status,
            String warehouseCode,
            String supplierCode) {
        LambdaQueryWrapper<InboundOrder> wrapper = new LambdaQueryWrapper<>();
        if (inboundType != null) wrapper.eq(InboundOrder::getInboundType, inboundType);
        if (status != null) wrapper.eq(InboundOrder::getStatus, status);
        if (warehouseCode != null) wrapper.eq(InboundOrder::getWarehouseCode, warehouseCode);
        if (supplierCode != null) wrapper.eq(InboundOrder::getSupplierCode, supplierCode);
        wrapper.orderByDesc(InboundOrder::getCreatedTime);
        return inboundOrderMapper.selectPage(page, wrapper);
    }

    public InboundOrder getInboundOrderByNo(String inboundNo) {
        return inboundOrderMapper.selectByInboundNo(inboundNo);
    }

    public List<InboundDetail> getInboundDetails(String inboundNo) {
        return inboundDetailMapper.selectByInboundNo(inboundNo);
    }

    // ============================================================

    // 3. 收货管理
    // ============================================================

    /** 执行收货 */
    @Transactional(rollbackFor = Exception.class)
    public ReceiveRecord receive(
            String inboundNo,
            String detailNo,
            String skuCode,
            String batchNo,
            BigDecimal receiveQty,
            String receiveLocation,
            String receiveType,
            String operator) {
        InboundOrder order = inboundOrderMapper.selectByInboundNo(inboundNo);
        if (order == null) throw new RuntimeException("入库单不存在: " + inboundNo);

        // 更新入库单状态
        if (InboundStatus.CREATED.getCode().equals(order.getStatus())) {
            order.setStatus(InboundStatus.RECEIVING.getCode());
            order.setReceiveTime(LocalDateTime.now());
        }
        order.setReceivedQty(order.getReceivedQty().add(receiveQty));
        inboundOrderMapper.updateById(order);

        // 更新明细
        InboundDetail detail = inboundDetailMapper.selectByDetailNo(detailNo);
        if (detail != null) {
            detail.setReceivedQty(detail.getReceivedQty().add(receiveQty));
            detail.setStatus("RECEIVED");
            inboundDetailMapper.updateById(detail);
        }

        // 保存收货记录
        ReceiveRecord record = new ReceiveRecord();
        record.setRecordNo(generateReceiveNo());
        record.setInboundNo(inboundNo);
        record.setDetailNo(detailNo);
        record.setSkuCode(skuCode);
        record.setBatchNo(batchNo);
        record.setReceiveQty(receiveQty);
        record.setReceiveLocation(receiveLocation);
        record.setReceiveType(receiveType != null ? receiveType : "NORMAL");
        record.setDifferenceQty(BigDecimal.ZERO);
        record.setOperator(operator);
        record.setReceiveTime(LocalDateTime.now());
        receiveRecordMapper.insert(record);

        log.info("收货完成: inbound={}, detail={}, qty={}", inboundNo, detailNo, receiveQty);
        return record;
    }

    /** 收货完成 */
    @Transactional(rollbackFor = Exception.class)
    public InboundOrder completeReceive(String inboundNo) {
        InboundOrder order = inboundOrderMapper.selectByInboundNo(inboundNo);
        if (order == null) throw new RuntimeException("入库单不存在: " + inboundNo);
        order.setStatus(InboundStatus.RECEIVED.getCode());
        inboundOrderMapper.updateById(order);

        // 更新ASN状态
        if (order.getAsnNo() != null) {
            Asn asn = asnMapper.selectByAsnNo(order.getAsnNo());
            if (asn != null) {
                asn.setStatus("RECEIVED");
                asn.setReceivedQty(order.getReceivedQty());
                asnMapper.updateById(asn);
            }
        }
        log.info("收货完成: {}", inboundNo);
        return order;
    }

    public List<ReceiveRecord> getReceiveRecords(String inboundNo) {
        return receiveRecordMapper.selectByInboundNo(inboundNo);
    }

    // ============================================================

    // 4. 上架管理
    // ============================================================

    /** 执行上架 */
    @Transactional(rollbackFor = Exception.class)
    public InboundDetail putaway(
            String inboundNo,
            String detailNo,
            String targetLocation,
            BigDecimal putawayQty,
            String operator) {
        InboundOrder order = inboundOrderMapper.selectByInboundNo(inboundNo);
        if (order == null) throw new RuntimeException("入库单不存在: " + inboundNo);

        if (InboundStatus.RECEIVED.getCode().equals(order.getStatus())) {
            order.setStatus(InboundStatus.PUTAWAYING.getCode());
            order.setPutawayTime(LocalDateTime.now());
        }
        order.setPutawayQty(order.getPutawayQty().add(putawayQty));
        inboundOrderMapper.updateById(order);

        InboundDetail detail = inboundDetailMapper.selectByDetailNo(detailNo);
        if (detail != null) {
            detail.setPutawayQty(detail.getPutawayQty().add(putawayQty));
            // 判断是否全部上架完成
            if (detail.getPutawayQty().compareTo(detail.getReceivedQty()) >= 0) {
                detail.setStatus("DONE");
            } else {
                detail.setStatus("PUTAWAYING");
            }
            inboundDetailMapper.updateById(detail);
        }

        // 检查整单是否完成
        checkInboundComplete(inboundNo);

        log.info(
                "上架完成: inbound={}, detail={}, location={}, qty={}",
                inboundNo,
                detailNo,
                targetLocation,
                putawayQty);
        return detail;
    }

    /** 检查入库单是否全部完成 */
    private void checkInboundComplete(String inboundNo) {
        List<InboundDetail> details = inboundDetailMapper.selectByInboundNo(inboundNo);
        boolean allDone = details.stream().allMatch(d -> "DONE".equals(d.getStatus()));
        if (allDone) {
            InboundOrder order = inboundOrderMapper.selectByInboundNo(inboundNo);
            order.setStatus(InboundStatus.DONE.getCode());
            order.setDoneTime(LocalDateTime.now());
            inboundOrderMapper.updateById(order);
            log.info("入库单完成: {}", inboundNo);
        }
    }

    // ============================================================

    // 工具方法
    // ============================================================

    private String generateAsnNo() {
        return "ASN"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }

    private String generateInboundNo() {
        return "IN"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }

    private String generateDetailNo() {
        return "IND"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }

    private String generateReceiveNo() {
        return "RCV"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }
}
