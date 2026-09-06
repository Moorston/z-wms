package com.xwms.core.receipt.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.core.inbound.entity.Asn;
import com.xwms.core.inbound.entity.InboundDetail;
import com.xwms.core.inbound.entity.InboundOrder;
import com.xwms.core.inbound.entity.ReceiveRecord;
import com.xwms.core.inbound.mapper.AsnMapper;
import com.xwms.core.inbound.mapper.InboundDetailMapper;
import com.xwms.core.inbound.mapper.InboundOrderMapper;
import com.xwms.core.inbound.mapper.ReceiveRecordMapper;
import com.xwms.core.po.service.PurchaseOrderService;
import com.xwms.core.receipt.entity.*;
import com.xwms.core.receipt.enums.ReceiptStatus;
import com.xwms.core.receipt.enums.ReceiptType;
import com.xwms.core.receipt.enums.ScanMode;
import com.xwms.core.receipt.mapper.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 收货管理核心服务 支持12种收货方式： B1 按ASN整单收货 / B2 部分收货 / B3 码盘收货 / B4 扫描收货 B5 按箱收货 / B6 快捷收货 / B7 可视化收货 / B8
 * 混ASN混PO扫描 B9 组件扫描收货 / B10 整理收货 / B11 预收货 / A3 盲收
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReceiptService {

    private final ReceiptTaskMapper taskMapper;
    private final ReceiptTaskDetailMapper taskDetailMapper;
    private final ReceiptRecordMapper recordMapper;
    private final ReceiptScanLogMapper scanLogMapper;
    private final BlindReceiptMapper blindReceiptMapper;
    private final AsnMapper asnMapper;
    private final InboundOrderMapper inboundOrderMapper;
    private final InboundDetailMapper inboundDetailMapper;
    private final ReceiveRecordMapper receiveRecordMapper;
    private final PurchaseOrderService purchaseOrderService;

    private static final AtomicInteger SEQ = new AtomicInteger(0);
    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    // ============================================================

    // 1. 收货任务管理
    // ============================================================

    /** 从ASN生成收货任务 */
    @Transactional(rollbackFor = Exception.class)
    public ReceiptTask createTaskFromAsn(
            String asnNo, String receiptType, String scanMode, String operator) {
        log.info("从ASN生成收货任务: asnNo={}, receiptType={}", asnNo, receiptType);

        Asn asn = asnMapper.selectByAsnNo(asnNo);
        if (asn == null) throw new RuntimeException("ASN不存在: " + asnNo);

        // 检查是否已有收货任务
        ReceiptTask existing = taskMapper.selectByAsnNo(asnNo);
        if (existing != null && !ReceiptStatus.CANCELLED.getCode().equals(existing.getStatus())) {
            log.warn("ASN已有收货任务: asnNo={}, taskNo={}", asnNo, existing.getTaskNo());
            return existing;
        }

        // 创建收货任务
        ReceiptTask task = new ReceiptTask();
        task.setTaskNo(generateTaskNo());
        task.setAsnNo(asnNo);
        task.setInboundNo(asn.getInboundNo());
        task.setPoNo(asn.getPoNo());
        task.setReceiptType(receiptType != null ? receiptType : ReceiptType.ASN.getCode());
        task.setScanMode(scanMode != null ? scanMode : ScanMode.BATCH.getCode());
        task.setSupplierCode(asn.getSupplierCode());
        task.setOwnerCode(asn.getOwnerCode());
        task.setWarehouseCode(asn.getWarehouseCode());
        task.setReceiveArea(asn.getReceiveArea());
        task.setReceiveLocation(asn.getReceiveLocation());
        task.setExpectedQty(asn.getExpectedQty() != null ? asn.getExpectedQty() : BigDecimal.ZERO);
        task.setReceivedQty(BigDecimal.ZERO);
        task.setDifferenceQty(BigDecimal.ZERO);
        task.setStatus(ReceiptStatus.PENDING.getCode());
        task.setQcRequired(asn.getQcRequired());
        task.setDirectPutaway("N");
        task.setSource("ASN");
        task.setCreatedBy(operator);
        taskMapper.insert(task);

        // 从入库单明细生成收货任务明细
        List<InboundDetail> inboundDetails =
                inboundDetailMapper.selectByInboundNo(asn.getInboundNo());
        int lineNo = 1;
        for (InboundDetail inboundDetail : inboundDetails) {
            ReceiptTaskDetail detail = new ReceiptTaskDetail();
            detail.setDetailNo(generateTaskDetailNo());
            detail.setTaskNo(task.getTaskNo());
            detail.setLineNo(lineNo++);
            detail.setAsnLineNo(inboundDetail.getLineNo());
            detail.setInboundDetailNo(inboundDetail.getDetailNo());
            detail.setSkuCode(inboundDetail.getSkuCode());
            detail.setSkuName(inboundDetail.getSkuName());
            detail.setBarcode(inboundDetail.getBarcode());
            detail.setUnit(inboundDetail.getUnit());
            detail.setExpectedQty(
                    inboundDetail.getExpectedQty() != null
                            ? inboundDetail.getExpectedQty()
                            : BigDecimal.ZERO);
            detail.setReceivedQty(BigDecimal.ZERO);
            detail.setDifferenceQty(BigDecimal.ZERO);
            detail.setStatus(ReceiptStatus.PENDING.getCode());
            taskDetailMapper.insert(detail);
        }

        // 更新ASN状态
        asn.setStatus("RECEIVING");
        asnMapper.updateById(asn);

        log.info("从ASN生成收货任务完成: taskNo={}, 明细{}行", task.getTaskNo(), inboundDetails.size());
        return task;
    }

    /** 取消收货任务 */
    @Transactional(rollbackFor = Exception.class)
    public ReceiptTask cancelTask(String taskNo, String cancelReason, String operator) {
        ReceiptTask task = taskMapper.selectByTaskNo(taskNo);
        if (task == null) throw new RuntimeException("收货任务不存在: " + taskNo);

        // 已收货的任务不允许取消
        if (task.getReceivedQty() != null && task.getReceivedQty().compareTo(BigDecimal.ZERO) > 0) {
            throw new RuntimeException("已收货的任务不允许取消，请使用取消收货功能: " + taskNo);
        }

        task.setStatus(ReceiptStatus.CANCELLED.getCode());
        task.setRemark(cancelReason);
        task.setUpdatedBy(operator);
        task.setUpdatedTime(LocalDateTime.now());
        taskMapper.updateById(task);

        // 更新明细状态
        List<ReceiptTaskDetail> details = taskDetailMapper.selectByTaskNo(taskNo);
        for (ReceiptTaskDetail detail : details) {
            detail.setStatus(ReceiptStatus.CANCELLED.getCode());
            taskDetailMapper.updateById(detail);
        }

        // 恢复ASN状态
        if (task.getAsnNo() != null) {
            Asn asn = asnMapper.selectByAsnNo(task.getAsnNo());
            if (asn != null) {
                asn.setStatus("CREATED");
                asnMapper.updateById(asn);
            }
        }

        log.info("取消收货任务: taskNo={}, 原因={}", taskNo, cancelReason);
        return task;
    }

    public ReceiptTask getTaskByNo(String taskNo) {
        return taskMapper.selectByTaskNo(taskNo);
    }

    public List<ReceiptTaskDetail> getTaskDetails(String taskNo) {
        return taskDetailMapper.selectByTaskNo(taskNo);
    }

    public Page<ReceiptTask> pageTasks(
            Page<ReceiptTask> page,
            String receiptType,
            String status,
            String warehouseCode,
            String asnNo) {
        LambdaQueryWrapper<ReceiptTask> wrapper = new LambdaQueryWrapper<>();
        if (receiptType != null) wrapper.eq(ReceiptTask::getReceiptType, receiptType);
        if (status != null) wrapper.eq(ReceiptTask::getStatus, status);
        if (warehouseCode != null) wrapper.eq(ReceiptTask::getWarehouseCode, warehouseCode);
        if (asnNo != null) wrapper.eq(ReceiptTask::getAsnNo, asnNo);
        wrapper.orderByDesc(ReceiptTask::getCreatedTime);
        return taskMapper.selectPage(page, wrapper);
    }

    // ============================================================

    // 2. B1 按ASN整单收货
    // ============================================================

    /** 按ASN整单收货 */
    @Transactional(rollbackFor = Exception.class)
    public ReceiptTask receiveByAsn(String taskNo, String receiveLocation, String operator) {
        log.info("按ASN整单收货: taskNo={}", taskNo);

        ReceiptTask task = taskMapper.selectByTaskNo(taskNo);
        if (task == null) throw new RuntimeException("收货任务不存在: " + taskNo);

        List<ReceiptTaskDetail> details = taskDetailMapper.selectByTaskNo(taskNo);
        BigDecimal totalReceived = BigDecimal.ZERO;

        for (ReceiptTaskDetail detail : details) {
            // 按预期数量整单收货
            BigDecimal receiveQty = detail.getExpectedQty();
            totalReceived = totalReceived.add(receiveQty);

            // 创建收货记录
            createReceiptRecord(
                    task, detail, receiveQty, receiveLocation, ReceiptType.ASN.getCode(), operator);

            // 更新任务明细
            detail.setReceivedQty(receiveQty);
            detail.setStatus(ReceiptStatus.COMPLETED.getCode());
            taskDetailMapper.updateById(detail);
        }

        // 更新任务
        task.setReceivedQty(totalReceived);
        task.setDifferenceQty(BigDecimal.ZERO);
        task.setStatus(ReceiptStatus.COMPLETED.getCode());
        task.setCompleteTime(LocalDateTime.now());
        task.setReceiver(operator);
        taskMapper.updateById(task);

        // 更新入库单和ASN
        updateInboundAfterReceive(task, totalReceived);

        log.info("按ASN整单收货完成: taskNo={}, 总数量={}", taskNo, totalReceived);
        return task;
    }

    // ============================================================

    // 3. B2 部分收货/多次收货
    // ============================================================

    /** 部分收货 */
    @Transactional(rollbackFor = Exception.class)
    public ReceiptRecord partialReceive(
            String taskNo,
            String detailNo,
            BigDecimal receiveQty,
            String batchNo,
            String receiveLocation,
            String operator) {
        log.info("部分收货: taskNo={}, detailNo={}, qty={}", taskNo, detailNo, receiveQty);

        ReceiptTask task = taskMapper.selectByTaskNo(taskNo);
        if (task == null) throw new RuntimeException("收货任务不存在: " + taskNo);

        ReceiptTaskDetail detail = taskDetailMapper.selectByDetailNo(detailNo);
        if (detail == null) throw new RuntimeException("收货任务明细不存在: " + detailNo);

        // 校验收货数量不能超过预期数量（允许部分收货）
        BigDecimal remainingQty = detail.getExpectedQty().subtract(detail.getReceivedQty());
        if (receiveQty.compareTo(remainingQty) > 0) {
            throw new RuntimeException(
                    String.format(
                            "收货数量超过剩余数量: 明细=%s, 剩余=%s, 请求=%s", detailNo, remainingQty, receiveQty));
        }

        // 创建收货记录
        ReceiptRecord record =
                createReceiptRecord(
                        task,
                        detail,
                        receiveQty,
                        receiveLocation,
                        ReceiptType.PARTIAL.getCode(),
                        operator);
        record.setBatchNo(batchNo);
        recordMapper.updateById(record);

        // 更新任务明细
        detail.setReceivedQty(detail.getReceivedQty().add(receiveQty));
        detail.setBatchNo(batchNo);
        if (detail.getReceivedQty().compareTo(detail.getExpectedQty()) >= 0) {
            detail.setStatus(ReceiptStatus.COMPLETED.getCode());
        } else {
            detail.setStatus(ReceiptStatus.PARTIAL.getCode());
        }
        taskDetailMapper.updateById(detail);

        // 更新任务
        task.setReceivedQty(task.getReceivedQty().add(receiveQty));
        task.setDifferenceQty(task.getExpectedQty().subtract(task.getReceivedQty()));
        if (task.getReceivedQty().compareTo(BigDecimal.ZERO) == 0) {
            task.setStatus(ReceiptStatus.PENDING.getCode());
        } else if (task.getReceivedQty().compareTo(task.getExpectedQty()) >= 0) {
            task.setStatus(ReceiptStatus.COMPLETED.getCode());
            task.setCompleteTime(LocalDateTime.now());
        } else {
            task.setStatus(ReceiptStatus.PARTIAL.getCode());
        }
        task.setReceiver(operator);
        taskMapper.updateById(task);

        // 更新入库单
        updateInboundAfterReceive(task, receiveQty);

        log.info("部分收货完成: taskNo={}, detailNo={}, qty={}", taskNo, detailNo, receiveQty);
        return record;
    }

    // ============================================================

    // 4. B4 扫描收货（核心功能）
    // ============================================================

    /** 扫描收货 支持批量/逐件/逐箱/序列号四种扫描模式 */
    @Transactional(rollbackFor = Exception.class)
    public ScanResult scanReceive(
            String taskNo,
            String scanContent,
            String scanType,
            String scanMode,
            String receiveLocation,
            String operator) {
        log.info(
                "扫描收货: taskNo={}, scanContent={}, scanType={}, scanMode={}",
                taskNo,
                scanContent,
                scanType,
                scanMode);

        ReceiptTask task = taskMapper.selectByTaskNo(taskNo);
        if (task == null) throw new RuntimeException("收货任务不存在: " + taskNo);

        ScanResult result = new ScanResult();
        result.setScanContent(scanContent);
        result.setScanType(scanType);

        // 根据扫描类型匹配商品
        ReceiptTaskDetail matchedDetail = matchDetailByScan(task, scanContent, scanType);

        if (matchedDetail == null) {
            // 扫描失败
            result.setScanResult("FAIL");
            result.setFailReason("未找到匹配的商品: " + scanContent);
            saveScanLog(task, null, scanContent, scanType, scanMode, "FAIL", "未找到匹配的商品", operator);
            return result;
        }

        // 序列号模式校验
        if ("SERIAL".equals(scanMode)) {
            int dupCount = scanLogMapper.countDuplicateSerial(scanContent, taskNo);
            if (dupCount > 0) {
                result.setScanResult("DUPLICATE");
                result.setFailReason("序列号重复: " + scanContent);
                saveScanLog(
                        task,
                        matchedDetail,
                        scanContent,
                        scanType,
                        scanMode,
                        "DUPLICATE",
                        "序列号重复",
                        operator);
                return result;
            }
        }

        // 计算扫描数量
        BigDecimal scanQty = calculateScanQty(scanMode, matchedDetail);

        // 校验剩余数量
        BigDecimal remainingQty =
                matchedDetail.getExpectedQty().subtract(matchedDetail.getReceivedQty());
        if (scanQty.compareTo(remainingQty) > 0) {
            result.setScanResult("FAIL");
            result.setFailReason(String.format("扫描数量超过剩余数量: 剩余=%s, 请求=%s", remainingQty, scanQty));
            saveScanLog(
                    task,
                    matchedDetail,
                    scanContent,
                    scanType,
                    scanMode,
                    "FAIL",
                    result.getFailReason(),
                    operator);
            return result;
        }

        // 创建收货记录
        ReceiptRecord record =
                createReceiptRecord(
                        task,
                        matchedDetail,
                        scanQty,
                        receiveLocation,
                        ReceiptType.SCAN.getCode(),
                        operator);
        record.setScanMode(scanMode);
        if ("SERIAL".equals(scanType)) {
            record.setSerialNo(scanContent);
        }
        recordMapper.updateById(record);

        // 保存扫描日志
        saveScanLog(
                task, matchedDetail, scanContent, scanType, scanMode, "SUCCESS", null, operator);

        // 更新任务明细
        matchedDetail.setReceivedQty(matchedDetail.getReceivedQty().add(scanQty));
        if ("SERIAL".equals(scanType)) {
            matchedDetail.setSerialNo(scanContent);
        }
        if (matchedDetail.getReceivedQty().compareTo(matchedDetail.getExpectedQty()) >= 0) {
            matchedDetail.setStatus(ReceiptStatus.COMPLETED.getCode());
        } else {
            matchedDetail.setStatus(ReceiptStatus.RECEIVING.getCode());
        }
        taskDetailMapper.updateById(matchedDetail);

        // 更新任务
        task.setReceivedQty(task.getReceivedQty().add(scanQty));
        task.setDifferenceQty(task.getExpectedQty().subtract(task.getReceivedQty()));
        if (task.getReceivedQty().compareTo(task.getExpectedQty()) >= 0) {
            task.setStatus(ReceiptStatus.COMPLETED.getCode());
            task.setCompleteTime(LocalDateTime.now());
        } else {
            task.setStatus(ReceiptStatus.RECEIVING.getCode());
        }
        task.setReceiver(operator);
        taskMapper.updateById(task);

        // 更新入库单
        updateInboundAfterReceive(task, scanQty);

        // 满箱提醒
        if (matchedDetail.getPackageQty() != null
                && matchedDetail.getPackageQty().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal currentBoxQty =
                    matchedDetail.getReceivedQty().remainder(matchedDetail.getPackageQty());
            if (currentBoxQty.compareTo(BigDecimal.ZERO) == 0) {
                result.setFullBoxAlert(true);
            }
        }

        result.setScanResult("SUCCESS");
        result.setSkuCode(matchedDetail.getSkuCode());
        result.setSkuName(matchedDetail.getSkuName());
        result.setScanQty(scanQty);
        result.setReceivedQty(matchedDetail.getReceivedQty());
        result.setExpectedQty(matchedDetail.getExpectedQty());
        result.setRemainingQty(
                matchedDetail.getExpectedQty().subtract(matchedDetail.getReceivedQty()));

        log.info("扫描收货成功: taskNo={}, sku={}, qty={}", taskNo, matchedDetail.getSkuCode(), scanQty);
        return result;
    }

    /** 根据扫描内容匹配任务明细 */
    private ReceiptTaskDetail matchDetailByScan(
            ReceiptTask task, String scanContent, String scanType) {
        List<ReceiptTaskDetail> details = taskDetailMapper.selectByTaskNo(task.getTaskNo());
        for (ReceiptTaskDetail detail : details) {
            if (ReceiptStatus.COMPLETED.getCode().equals(detail.getStatus())) continue;
            switch (scanType) {
                case "BARCODE":
                    if (scanContent.equals(detail.getBarcode())
                            || scanContent.equals(detail.getSkuCode())) {
                        return detail;
                    }
                    break;
                case "SERIAL":
                    // 序列号模式下，序列号不预存在明细中，通过商品匹配
                    if (scanContent.startsWith(detail.getSkuCode())) {
                        return detail;
                    }
                    break;
                case "LPN":
                    // 箱号模式暂通过商品匹配
                    break;
                default:
                    // 未知扫描类型，不匹配
                    break;
            }
        }
        return null;
    }

    /** 计算扫描数量 */
    private BigDecimal calculateScanQty(String scanMode, ReceiptTaskDetail detail) {
        switch (scanMode) {
            case "PIECE":
                return BigDecimal.ONE;
            case "BOX":
                return detail.getPackageQty() != null ? detail.getPackageQty() : BigDecimal.ONE;
            case "SERIAL":
                return BigDecimal.ONE;
            case "BATCH":
            default:
                // 批量模式默认返回1，实际数量由用户输入
                return BigDecimal.ONE;
        }
    }

    /** 保存扫描日志 */
    private void saveScanLog(
            ReceiptTask task,
            ReceiptTaskDetail detail,
            String scanContent,
            String scanType,
            String scanMode,
            String scanResult,
            String failReason,
            String operator) {
        ReceiptScanLog scanLog = new ReceiptScanLog();
        scanLog.setScanNo(generateScanNo());
        scanLog.setTaskNo(task.getTaskNo());
        scanLog.setScanType(scanType);
        scanLog.setScanContent(scanContent);
        scanLog.setScanMode(scanMode);
        scanLog.setScanResult(scanResult);
        scanLog.setFailReason(failReason);
        if (detail != null) {
            scanLog.setSkuCode(detail.getSkuCode());
            scanLog.setSkuName(detail.getSkuName());
        }
        scanLog.setOperator(operator);
        scanLog.setScanTime(LocalDateTime.now());
        scanLogMapper.insert(scanLog);
    }

    // ============================================================

    // 5. B5 按箱收货
    // ============================================================

    /** 按箱收货（预打包不拆箱） */
    @Transactional(rollbackFor = Exception.class)
    public ReceiptRecord receiveByBox(
            String taskNo,
            String detailNo,
            String lpnNo,
            BigDecimal boxQty,
            String receiveLocation,
            String operator) {
        log.info(
                "按箱收货: taskNo={}, detailNo={}, lpn={}, boxQty={}", taskNo, detailNo, lpnNo, boxQty);

        ReceiptTask task = taskMapper.selectByTaskNo(taskNo);
        if (task == null) throw new RuntimeException("收货任务不存在: " + taskNo);

        ReceiptTaskDetail detail = taskDetailMapper.selectByDetailNo(detailNo);
        if (detail == null) throw new RuntimeException("收货任务明细不存在: " + detailNo);

        // 箱号校验（检查箱号是否在预期范围内）
        if (lpnNo == null || lpnNo.isEmpty()) {
            throw new RuntimeException("箱号不能为空");
        }

        // 创建收货记录
        ReceiptRecord record =
                createReceiptRecord(
                        task, detail, boxQty, receiveLocation, ReceiptType.BOX.getCode(), operator);
        record.setLpnNo(lpnNo);
        recordMapper.updateById(record);

        // 更新任务明细
        detail.setReceivedQty(detail.getReceivedQty().add(boxQty));
        if (detail.getReceivedQty().compareTo(detail.getExpectedQty()) >= 0) {
            detail.setStatus(ReceiptStatus.COMPLETED.getCode());
        } else {
            detail.setStatus(ReceiptStatus.PARTIAL.getCode());
        }
        taskDetailMapper.updateById(detail);

        // 更新任务
        task.setReceivedQty(task.getReceivedQty().add(boxQty));
        if (task.getReceivedQty().compareTo(task.getExpectedQty()) >= 0) {
            task.setStatus(ReceiptStatus.COMPLETED.getCode());
            task.setCompleteTime(LocalDateTime.now());
        } else {
            task.setStatus(ReceiptStatus.PARTIAL.getCode());
        }
        taskMapper.updateById(task);

        updateInboundAfterReceive(task, boxQty);

        log.info("按箱收货完成: taskNo={}, lpn={}, qty={}", taskNo, lpnNo, boxQty);
        return record;
    }

    // ============================================================

    // 6. B6 快捷收货
    // ============================================================

    /** 快捷收货（简化版，信息集中展示） */
    @Transactional(rollbackFor = Exception.class)
    public ReceiptTask quickReceive(String taskNo, List<QuickReceiveItem> items, String operator) {
        log.info("快捷收货: taskNo={}, 商品{}种", taskNo, items != null ? items.size() : 0);

        ReceiptTask task = taskMapper.selectByTaskNo(taskNo);
        if (task == null) throw new RuntimeException("收货任务不存在: " + taskNo);

        BigDecimal totalReceived = BigDecimal.ZERO;
        for (QuickReceiveItem item : items) {
            ReceiptTaskDetail detail =
                    taskDetailMapper.selectByTaskNoAndSku(taskNo, item.getSkuCode());
            if (detail == null) {
                log.warn("快捷收货未找到商品: taskNo={}, sku={}", taskNo, item.getSkuCode());
                continue;
            }

            // 创建收货记录
            createReceiptRecord(
                    task,
                    detail,
                    item.getReceiveQty(),
                    task.getReceiveLocation(),
                    ReceiptType.QUICK.getCode(),
                    operator);

            // 更新明细
            detail.setReceivedQty(detail.getReceivedQty().add(item.getReceiveQty()));
            if (detail.getReceivedQty().compareTo(detail.getExpectedQty()) >= 0) {
                detail.setStatus(ReceiptStatus.COMPLETED.getCode());
            } else {
                detail.setStatus(ReceiptStatus.PARTIAL.getCode());
            }
            taskDetailMapper.updateById(detail);

            totalReceived = totalReceived.add(item.getReceiveQty());
        }

        // 更新任务
        task.setReceivedQty(task.getReceivedQty().add(totalReceived));
        if (task.getReceivedQty().compareTo(task.getExpectedQty()) >= 0) {
            task.setStatus(ReceiptStatus.COMPLETED.getCode());
            task.setCompleteTime(LocalDateTime.now());
        } else {
            task.setStatus(ReceiptStatus.PARTIAL.getCode());
        }
        task.setReceiver(operator);
        taskMapper.updateById(task);

        updateInboundAfterReceive(task, totalReceived);

        log.info("快捷收货完成: taskNo={}, 总数量={}", taskNo, totalReceived);
        return task;
    }

    // ============================================================

    // 7. B8 混ASN/混PO扫描收货
    // ============================================================

    /** 混ASN/混PO扫描收货 扫描时自动匹配到对应的ASN/PO */
    @Transactional(rollbackFor = Exception.class)
    public ScanResult mixScanReceive(
            String warehouseCode,
            String scanContent,
            String scanType,
            String receiveLocation,
            String operator) {
        log.info("混ASN扫描收货: warehouse={}, scanContent={}", warehouseCode, scanContent);

        // 查找所有待收货的任务
        List<ReceiptTask> pendingTasks = taskMapper.selectPendingTasks(warehouseCode);
        if (pendingTasks == null || pendingTasks.isEmpty()) {
            ScanResult result = new ScanResult();
            result.setScanResult("FAIL");
            result.setFailReason("没有待收货的任务");
            return result;
        }

        // 遍历所有任务，找到匹配的商品
        for (ReceiptTask task : pendingTasks) {
            ReceiptTaskDetail matchedDetail = matchDetailByScan(task, scanContent, scanType);
            if (matchedDetail != null) {
                log.info("混ASN扫描匹配到任务: taskNo={}, asnNo={}", task.getTaskNo(), task.getAsnNo());
                return scanReceive(
                        task.getTaskNo(),
                        scanContent,
                        scanType,
                        "BATCH",
                        receiveLocation,
                        operator);
            }
        }

        ScanResult result = new ScanResult();
        result.setScanResult("FAIL");
        result.setFailReason("未在任何待收货任务中找到匹配商品: " + scanContent);
        return result;
    }

    // ============================================================

    // 8. B11 预收货
    // ============================================================

    /** 预收货（先验后收，不增加库存） */
    @Transactional(rollbackFor = Exception.class)
    public ReceiptRecord preReceive(
            String taskNo,
            String detailNo,
            BigDecimal preReceiveQty,
            String receiveLocation,
            String operator) {
        log.info("预收货: taskNo={}, detailNo={}, qty={}", taskNo, detailNo, preReceiveQty);

        ReceiptTask task = taskMapper.selectByTaskNo(taskNo);
        if (task == null) throw new RuntimeException("收货任务不存在: " + taskNo);

        ReceiptTaskDetail detail = taskDetailMapper.selectByDetailNo(detailNo);
        if (detail == null) throw new RuntimeException("收货任务明细不存在: " + detailNo);

        // 预收货不增加库存，只记录
        ReceiptRecord record = new ReceiptRecord();
        record.setRecordNo(generateRecordNo());
        record.setTaskNo(taskNo);
        record.setAsnNo(task.getAsnNo());
        record.setInboundNo(task.getInboundNo());
        record.setTaskDetailNo(detail.getDetailNo());
        record.setSkuCode(detail.getSkuCode());
        record.setSkuName(detail.getSkuName());
        record.setReceiveQty(preReceiveQty);
        record.setReceiveLocation(receiveLocation);
        record.setReceiptType(ReceiptType.PRE.getCode());
        record.setOperator(operator);
        record.setReceiveTime(LocalDateTime.now());
        record.setRemark("预收货-不增加库存");
        recordMapper.insert(record);

        log.info("预收货完成: taskNo={}, qty={}（不增加库存）", taskNo, preReceiveQty);
        return record;
    }

    // ============================================================

    // 9. A3 盲收
    // ============================================================

    /** 盲收（无单据收货） */
    @Transactional(rollbackFor = Exception.class)
    public BlindReceipt blindReceive(
            String warehouseCode,
            String ownerCode,
            String skuCode,
            BigDecimal blindQty,
            String batchNo,
            String receiveLocation,
            String blindMode,
            String operator) {
        log.info(
                "盲收: warehouse={}, sku={}, qty={}, mode={}",
                warehouseCode,
                skuCode,
                blindQty,
                blindMode);

        BlindReceipt blindReceipt = new BlindReceipt();
        blindReceipt.setBlindNo(generateBlindNo());
        blindReceipt.setBlindMode(blindMode != null ? blindMode : "NORMAL");
        blindReceipt.setOwnerCode(ownerCode);
        blindReceipt.setWarehouseCode(warehouseCode);
        blindReceipt.setReceiveLocation(receiveLocation);
        blindReceipt.setSkuCode(skuCode);
        blindReceipt.setBlindQty(blindQty);
        blindReceipt.setBatchNo(batchNo);
        blindReceipt.setStatus("BLIND_RECEIVED");
        blindReceipt.setOperator(operator);
        blindReceipt.setReceiveTime(LocalDateTime.now());
        blindReceipt.setSource("BLIND");
        blindReceiptMapper.insert(blindReceipt);

        log.info("盲收完成: blindNo={}, sku={}, qty={}", blindReceipt.getBlindNo(), skuCode, blindQty);
        return blindReceipt;
    }

    /** 盲收匹配（匹配到PO/ASN） */
    @Transactional(rollbackFor = Exception.class)
    public BlindReceipt matchBlindReceipt(
            String blindNo, String poNo, String asnNo, String inboundNo, String operator) {
        log.info("盲收匹配: blindNo={}, poNo={}, asnNo={}", blindNo, poNo, asnNo);

        BlindReceipt blindReceipt = blindReceiptMapper.selectByBlindNo(blindNo);
        if (blindReceipt == null) throw new RuntimeException("盲收记录不存在: " + blindNo);

        blindReceipt.setMatchedPoNo(poNo);
        blindReceipt.setMatchedAsnNo(asnNo);
        blindReceipt.setMatchedInboundNo(inboundNo);
        blindReceipt.setStatus("MATCHED");
        blindReceipt.setMatchTime(LocalDateTime.now());
        blindReceipt.setMatchedBy(operator);
        blindReceiptMapper.updateById(blindReceipt);

        log.info("盲收匹配完成: blindNo={}, asnNo={}", blindNo, asnNo);
        return blindReceipt;
    }

    /** 盲收生成ASN（反向生成ASN明细） */
    @Transactional(rollbackFor = Exception.class)
    public Asn createAsnFromBlind(String blindNo, String operator) {
        log.info("盲收生成ASN: blindNo={}", blindNo);

        BlindReceipt blindReceipt = blindReceiptMapper.selectByBlindNo(blindNo);
        if (blindReceipt == null) throw new RuntimeException("盲收记录不存在: " + blindNo);

        // 创建ASN
        Asn asn = new Asn();
        asn.setAsnNo(generateAsnNo());
        asn.setWarehouseCode(blindReceipt.getWarehouseCode());
        asn.setOwnerCode(blindReceipt.getOwnerCode());
        asn.setSupplierCode(blindReceipt.getSupplierCode());
        asn.setExpectedQty(blindReceipt.getBlindQty());
        asn.setReceivedQty(blindReceipt.getBlindQty());
        asn.setStatus("RECEIVED");
        asn.setActualDate(LocalDateTime.now());
        asnMapper.insert(asn);

        // 更新盲收记录
        blindReceipt.setMatchedAsnNo(asn.getAsnNo());
        blindReceipt.setStatus("ASN_CREATED");
        blindReceipt.setAsnCreateTime(LocalDateTime.now());
        blindReceiptMapper.updateById(blindReceipt);

        log.info("盲收生成ASN完成: blindNo={}, asnNo={}", blindNo, asn.getAsnNo());
        return asn;
    }

    // ============================================================

    // 10. E5 取消收货
    // ============================================================

    /** 取消收货（仅收货未上架可取消） */
    @Transactional(rollbackFor = Exception.class)
    public ReceiptRecord cancelReceive(String recordNo, String cancelReason, String operator) {
        log.info("取消收货: recordNo={}", recordNo);

        ReceiptRecord record = recordMapper.selectByRecordNo(recordNo);
        if (record == null) throw new RuntimeException("收货记录不存在: " + recordNo);

        // 检查是否已上架
        // TODO: 检查库存是否已上架，如果已上架则不允许取消

        // 扣减任务收货数量
        ReceiptTask task = taskMapper.selectByTaskNo(record.getTaskNo());
        if (task != null) {
            task.setReceivedQty(task.getReceivedQty().subtract(record.getReceiveQty()));
            if (task.getReceivedQty().compareTo(BigDecimal.ZERO) <= 0) {
                task.setStatus(ReceiptStatus.PENDING.getCode());
                task.setReceivedQty(BigDecimal.ZERO);
            } else {
                task.setStatus(ReceiptStatus.PARTIAL.getCode());
            }
            taskMapper.updateById(task);

            // 扣减明细收货数量
            ReceiptTaskDetail detail = taskDetailMapper.selectByDetailNo(record.getTaskDetailNo());
            if (detail != null) {
                detail.setReceivedQty(detail.getReceivedQty().subtract(record.getReceiveQty()));
                if (detail.getReceivedQty().compareTo(BigDecimal.ZERO) <= 0) {
                    detail.setStatus(ReceiptStatus.PENDING.getCode());
                    detail.setReceivedQty(BigDecimal.ZERO);
                } else {
                    detail.setStatus(ReceiptStatus.PARTIAL.getCode());
                }
                taskDetailMapper.updateById(detail);
            }
        }

        // 删除收货记录（或标记取消）
        record.setRemark("已取消: " + cancelReason);
        record.setReceiveQty(BigDecimal.ZERO);
        recordMapper.updateById(record);

        log.info("取消收货完成: recordNo={}, 原因={}", recordNo, cancelReason);
        return record;
    }

    // ============================================================

    // 11. 收货完成
    // ============================================================

    /** 标记收货完成 */
    @Transactional(rollbackFor = Exception.class)
    public ReceiptTask completeReceive(String taskNo, String operator) {
        log.info("标记收货完成: taskNo={}", taskNo);

        ReceiptTask task = taskMapper.selectByTaskNo(taskNo);
        if (task == null) throw new RuntimeException("收货任务不存在: " + taskNo);

        task.setStatus(ReceiptStatus.COMPLETED.getCode());
        task.setCompleteTime(LocalDateTime.now());
        task.setReceiver(operator);
        taskMapper.updateById(task);

        // 更新所有明细状态
        List<ReceiptTaskDetail> details = taskDetailMapper.selectByTaskNo(taskNo);
        for (ReceiptTaskDetail detail : details) {
            if (!ReceiptStatus.COMPLETED.getCode().equals(detail.getStatus())) {
                detail.setStatus(ReceiptStatus.COMPLETED.getCode());
                taskDetailMapper.updateById(detail);
            }
        }

        // 更新ASN状态
        if (task.getAsnNo() != null) {
            Asn asn = asnMapper.selectByAsnNo(task.getAsnNo());
            if (asn != null) {
                asn.setStatus("RECEIVED");
                asn.setReceivedQty(task.getReceivedQty());
                asnMapper.updateById(asn);
            }
        }

        log.info("标记收货完成: taskNo={}", taskNo);
        return task;
    }

    // ============================================================

    // 12. 公共方法
    // ============================================================

    /** 创建收货记录 */
    private ReceiptRecord createReceiptRecord(
            ReceiptTask task,
            ReceiptTaskDetail detail,
            BigDecimal receiveQty,
            String receiveLocation,
            String receiptType,
            String operator) {
        ReceiptRecord record = new ReceiptRecord();
        record.setRecordNo(generateRecordNo());
        record.setTaskNo(task.getTaskNo());
        record.setAsnNo(task.getAsnNo());
        record.setInboundNo(task.getInboundNo());
        record.setTaskDetailNo(detail.getDetailNo());
        record.setInboundDetailNo(detail.getInboundDetailNo());
        record.setSkuCode(detail.getSkuCode());
        record.setSkuName(detail.getSkuName());
        record.setBarcode(detail.getBarcode());
        record.setReceiveQty(receiveQty);
        record.setUnit(detail.getUnit());
        record.setReceiveLocation(
                receiveLocation != null ? receiveLocation : task.getReceiveLocation());
        record.setReceiptType(receiptType);
        record.setDifferenceQty(receiveQty.subtract(detail.getExpectedQty()));
        record.setDifferenceType(
                receiveQty.compareTo(detail.getExpectedQty()) > 0
                        ? "OVER"
                        : receiveQty.compareTo(detail.getExpectedQty()) < 0 ? "SHORT" : "NONE");
        record.setOperator(operator);
        record.setReceiveTime(LocalDateTime.now());
        recordMapper.insert(record);

        // 同时写入inbound模块的ReceiveRecord（保持兼容）
        ReceiveRecord inboundRecord = new ReceiveRecord();
        inboundRecord.setRecordNo(record.getRecordNo());
        inboundRecord.setInboundNo(task.getInboundNo());
        inboundRecord.setDetailNo(detail.getInboundDetailNo());
        inboundRecord.setSkuCode(detail.getSkuCode());
        inboundRecord.setReceiveQty(receiveQty);
        inboundRecord.setReceiveLocation(receiveLocation);
        inboundRecord.setReceiveType(receiptType);
        inboundRecord.setOperator(operator);
        inboundRecord.setReceiveTime(LocalDateTime.now());
        receiveRecordMapper.insert(inboundRecord);

        return record;
    }

    /** 收货后更新入库单 */
    private void updateInboundAfterReceive(ReceiptTask task, BigDecimal receivedQty) {
        if (task.getInboundNo() == null) return;

        InboundOrder inboundOrder = inboundOrderMapper.selectByInboundNo(task.getInboundNo());
        if (inboundOrder == null) return;

        // 更新入库单收货数量
        inboundOrder.setReceivedQty(inboundOrder.getReceivedQty().add(receivedQty));
        if (inboundOrder.getReceivedQty().compareTo(BigDecimal.ZERO) > 0) {
            inboundOrder.setStatus("RECEIVING");
        }
        inboundOrderMapper.updateById(inboundOrder);

        // 更新PO收货数量（如果关联了PO）
        if (task.getAsnNo() != null) {
            // 通过ASN找到PO关联，更新PO收货数量
            // TODO: 这里简化处理，实际应通过PO-ASN关联表找到对应PO明细
        }
    }

    // ============================================================

    // 13. 数据模型
    // ============================================================

    @lombok.Data
    public static class ScanResult {
        private String scanContent;
        private String scanType;
        private String scanResult; // SUCCESS/FAIL/DUPLICATE
        private String failReason;
        private String skuCode;
        private String skuName;
        private BigDecimal scanQty;
        private BigDecimal receivedQty;
        private BigDecimal expectedQty;
        private BigDecimal remainingQty;
        private boolean fullBoxAlert;
    }

    @lombok.Data
    public static class QuickReceiveItem {
        private String skuCode;
        private BigDecimal receiveQty;
    }

    // ============================================================

    // 14. 编号生成
    // ============================================================

    private String generateTaskNo() {
        return "RCT"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }

    private String generateTaskDetailNo() {
        return "RTD"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }

    private String generateRecordNo() {
        return "RCR"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }

    private String generateScanNo() {
        return "SCN"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }

    private String generateBlindNo() {
        return "BLD"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }

    private String generateAsnNo() {
        return "ASN"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }
}
