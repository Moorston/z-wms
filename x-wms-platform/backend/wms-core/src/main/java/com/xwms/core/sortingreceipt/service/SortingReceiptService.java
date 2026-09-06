package com.xwms.core.sortingreceipt.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import com.xwms.core.sortingreceipt.entity.*;
import com.xwms.core.sortingreceipt.mapper.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 整理收货服务 核心能力: 配比箱拆箱整理/扫描SKU累加/满箱收货/整理收货箱标签打印 业务场景:
 * 服装行业配比箱（箱内混SKU，多为同款不同尺码，尺码比例固定）拆箱入库，整理为独色独码的单一SKU箱
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SortingReceiptService {

    private final SortingReceiptMapper sortingReceiptMapper;
    private final SortingReceiptDetailMapper sortingReceiptDetailMapper;
    private final SortingReceiptBoxMapper sortingReceiptBoxMapper;

    private static final AtomicInteger SEQ = new AtomicInteger(0);
    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    // ============================================================

    // 1. 创建整理收货单
    // ============================================================

    /** 创建整理收货单 开启参数RCV_AND_SRT，选择扫描收货，自动进入整理收货模式 */
    @Transactional(rollbackFor = Exception.class)
    public SortingReceipt createSortingReceipt(
            String asnNo,
            String inboundNo,
            String ownerCode,
            String warehouseCode,
            String groupNo,
            String styleCode,
            String colorCode,
            Integer boxCount,
            String locationCode,
            List<SortingReceiptDetail> details,
            String operator) {
        log.info("创建整理收货单: asnNo={}, 款式={}, 颜色={}, 配比箱数={}", asnNo, styleCode, colorCode, boxCount);

        String receiptNo = generateReceiptNo();
        SortingReceipt receipt = new SortingReceipt();
        receipt.setReceiptNo(receiptNo);
        receipt.setAsnNo(asnNo);
        receipt.setInboundNo(inboundNo);
        receipt.setOwnerCode(ownerCode);
        receipt.setWarehouseCode(warehouseCode);
        receipt.setGroupNo(groupNo);
        receipt.setStyleCode(styleCode);
        receipt.setColorCode(colorCode);
        receipt.setBoxCount(boxCount);
        receipt.setUnpackedBoxCount(0);
        receipt.setTotalQty(BigDecimal.ZERO);
        receipt.setSortedQty(BigDecimal.ZERO);
        receipt.setStatus("PENDING");
        receipt.setLocationCode(locationCode);
        receipt.setOperator(operator);
        receipt.setCreatedBy(operator);
        receipt.setCreatedTime(LocalDateTime.now());
        sortingReceiptMapper.insert(receipt);

        // 保存明细（每个尺码一个SKU）
        BigDecimal totalQty = BigDecimal.ZERO;
        int lineNo = 1;
        for (SortingReceiptDetail detail : details) {
            detail.setReceiptNo(receiptNo);
            detail.setLineNo(lineNo++);
            detail.setExpectedQty(detail.getRatioQty().multiply(new BigDecimal(boxCount)));
            detail.setScannedQty(BigDecimal.ZERO);
            detail.setReceivedQty(BigDecimal.ZERO);
            detail.setStatus("PENDING");
            detail.setIsFullBox("N");
            detail.setCreatedBy(operator);
            detail.setCreatedTime(LocalDateTime.now());
            totalQty = totalQty.add(detail.getExpectedQty());
        }
        sortingReceiptDetailMapper.batchInsert(details);

        // 更新总数量
        receipt.setTotalQty(totalQty);
        sortingReceiptMapper.updateById(receipt);

        log.info("整理收货单创建完成: receiptNo={}, SKU数={}, 总数量={}", receiptNo, details.size(), totalQty);
        return receipt;
    }

    // ============================================================

    // 2. 扫描产品SKU条码（可混序扫描，按SKU分别累加）
    // ============================================================

    /** 扫描SKU条码 可混序扫描，按SKU分别累加，某一SKU扫描数量累加满箱时系统提示满箱 */
    @Transactional(rollbackFor = Exception.class)
    public SortingReceiptDetail scanSku(
            String receiptNo, String skuCode, BigDecimal scanQty, String operator) {
        log.info("扫描SKU: receiptNo={}, sku={}, qty={}", receiptNo, skuCode, scanQty);

        SortingReceipt receipt = sortingReceiptMapper.selectByReceiptNo(receiptNo);
        if (receipt == null) {
            throw new RuntimeException("整理收货单不存在: " + receiptNo);
        }
        if (!"PENDING".equals(receipt.getStatus()) && !"SORTING".equals(receipt.getStatus())) {
            throw new RuntimeException("整理收货单状态不允许扫描: " + receipt.getStatus());
        }

        // 更新收货单状态
        if ("PENDING".equals(receipt.getStatus())) {
            receipt.setStatus("SORTING");
            receipt.setStartTime(LocalDateTime.now());
            sortingReceiptMapper.updateById(receipt);
        }

        // 查询SKU明细
        SortingReceiptDetail detail =
                sortingReceiptDetailMapper.selectByReceiptNoAndSku(receiptNo, skuCode);
        if (detail == null) {
            throw new RuntimeException("SKU不在配比箱明细中: " + skuCode);
        }

        // 累加扫描数量
        BigDecimal newScannedQty = detail.getScannedQty().add(scanQty);
        detail.setScannedQty(newScannedQty);
        detail.setStatus("SCANNING");

        // 检查是否满箱（根据包装箱数量判断）
        if (detail.getFullBoxQty() != null
                && detail.getFullBoxQty().compareTo(BigDecimal.ZERO) > 0) {
            if (newScannedQty.compareTo(detail.getFullBoxQty()) >= 0) {
                detail.setIsFullBox("Y");
                log.info(
                        "SKU满箱提示: sku={}, 已扫描={}, 满箱数量={}",
                        skuCode,
                        newScannedQty,
                        detail.getFullBoxQty());
            }
        }

        detail.setUpdatedTime(LocalDateTime.now());
        sortingReceiptDetailMapper.updateById(detail);

        // 更新收货单已整理数量
        receipt.setSortedQty(receipt.getSortedQty().add(scanQty));
        sortingReceiptMapper.updateById(receipt);

        log.info("SKU扫描完成: sku={}, 已扫描={}, 满箱={}", skuCode, newScannedQty, detail.getIsFullBox());
        return detail;
    }

    // ============================================================

    // 3. 满箱收货（系统提示满箱，点击满箱收货按钮确认此SKU收货）
    // ============================================================

    /** 满箱收货 系统提示满箱，点击"满箱收货"按钮确认此SKU收货，自动触发整理收货箱标签打印 */
    @Transactional(rollbackFor = Exception.class)
    public SortingReceiptBox fullBoxReceive(String receiptNo, String skuCode, String operator) {
        log.info("满箱收货: receiptNo={}, sku={}", receiptNo, skuCode);

        SortingReceipt receipt = sortingReceiptMapper.selectByReceiptNo(receiptNo);
        if (receipt == null) {
            throw new RuntimeException("整理收货单不存在: " + receiptNo);
        }

        SortingReceiptDetail detail =
                sortingReceiptDetailMapper.selectByReceiptNoAndSku(receiptNo, skuCode);
        if (detail == null) {
            throw new RuntimeException("SKU明细不存在: " + skuCode);
        }
        if (!"Y".equals(detail.getIsFullBox())) {
            throw new RuntimeException("SKU未满箱，不能执行满箱收货: " + skuCode);
        }

        // 生成整理收货箱号
        String boxNo = generateBoxNo();

        // 创建整理收货箱
        SortingReceiptBox box = new SortingReceiptBox();
        box.setBoxNo(boxNo);
        box.setReceiptNo(receiptNo);
        box.setSkuCode(skuCode);
        box.setSkuName(detail.getSkuName());
        box.setStyleCode(detail.getStyleCode());
        box.setColorCode(detail.getColorCode());
        box.setSizeCode(detail.getSizeCode());
        box.setBoxQty(detail.getFullBoxQty());
        box.setFullBoxQty(detail.getFullBoxQty());
        box.setIsFull("Y");
        box.setLocationCode(receipt.getLocationCode());
        box.setStatus("RECEIVED");
        box.setOperator(operator);
        box.setCreatedTime(LocalDateTime.now());
        box.setReceivedTime(LocalDateTime.now());
        box.setCreatedBy(operator);
        sortingReceiptBoxMapper.insert(box);

        // 更新明细已收货数量
        detail.setReceivedQty(detail.getReceivedQty().add(detail.getFullBoxQty()));
        detail.setScannedQty(detail.getScannedQty().subtract(detail.getFullBoxQty()));
        if (detail.getScannedQty().compareTo(detail.getFullBoxQty()) < 0) {
            detail.setIsFullBox("N");
        }
        if (detail.getReceivedQty().compareTo(detail.getExpectedQty()) >= 0) {
            detail.setStatus("COMPLETED");
        }
        detail.setUpdatedTime(LocalDateTime.now());
        sortingReceiptDetailMapper.updateById(detail);

        // TODO: 调用标签打印模块，触发整理收货箱标签打印

        log.info("满箱收货完成: boxNo={}, sku={}, 数量={}", boxNo, skuCode, box.getBoxQty());
        return box;
    }

    // ============================================================

    // 4. 未满箱SKU手工收货
    // ============================================================

    /** 手工确认收货（未满箱SKU处理） 仍处于累加数量、待收货确认状态，可手工点击确认收货按钮对未满箱数量进行收货 */
    @Transactional(rollbackFor = Exception.class)
    public SortingReceiptBox manualReceive(
            String receiptNo, String skuCode, BigDecimal receiveQty, String operator) {
        log.info("手工收货: receiptNo={}, sku={}, qty={}", receiptNo, skuCode, receiveQty);

        SortingReceipt receipt = sortingReceiptMapper.selectByReceiptNo(receiptNo);
        if (receipt == null) {
            throw new RuntimeException("整理收货单不存在: " + receiptNo);
        }

        SortingReceiptDetail detail =
                sortingReceiptDetailMapper.selectByReceiptNoAndSku(receiptNo, skuCode);
        if (detail == null) {
            throw new RuntimeException("SKU明细不存在: " + skuCode);
        }
        if (receiveQty.compareTo(detail.getScannedQty()) > 0) {
            throw new RuntimeException(
                    "收货数量不能大于已扫描数量: " + receiveQty + " > " + detail.getScannedQty());
        }

        // 生成整理收货箱号
        String boxNo = generateBoxNo();

        // 创建整理收货箱（未满箱）
        SortingReceiptBox box = new SortingReceiptBox();
        box.setBoxNo(boxNo);
        box.setReceiptNo(receiptNo);
        box.setSkuCode(skuCode);
        box.setSkuName(detail.getSkuName());
        box.setStyleCode(detail.getStyleCode());
        box.setColorCode(detail.getColorCode());
        box.setSizeCode(detail.getSizeCode());
        box.setBoxQty(receiveQty);
        box.setFullBoxQty(detail.getFullBoxQty());
        box.setIsFull(receiveQty.compareTo(detail.getFullBoxQty()) >= 0 ? "Y" : "N");
        box.setLocationCode(receipt.getLocationCode());
        box.setStatus("RECEIVED");
        box.setOperator(operator);
        box.setCreatedTime(LocalDateTime.now());
        box.setReceivedTime(LocalDateTime.now());
        box.setCreatedBy(operator);
        sortingReceiptBoxMapper.insert(box);

        // 更新明细
        detail.setReceivedQty(detail.getReceivedQty().add(receiveQty));
        detail.setScannedQty(detail.getScannedQty().subtract(receiveQty));
        if (detail.getReceivedQty().compareTo(detail.getExpectedQty()) >= 0) {
            detail.setStatus("COMPLETED");
        }
        detail.setUpdatedTime(LocalDateTime.now());
        sortingReceiptDetailMapper.updateById(detail);

        log.info("手工收货完成: boxNo={}, sku={}, 数量={}", boxNo, skuCode, receiveQty);
        return box;
    }

    // ============================================================

    // 5. 完成整理收货
    // ============================================================

    /** 完成整理收货 */
    @Transactional(rollbackFor = Exception.class)
    public SortingReceipt completeSortingReceipt(String receiptNo, String operator) {
        log.info("完成整理收货: receiptNo={}", receiptNo);

        SortingReceipt receipt = sortingReceiptMapper.selectByReceiptNo(receiptNo);
        if (receipt == null) {
            throw new RuntimeException("整理收货单不存在: " + receiptNo);
        }

        // 检查所有明细是否完成
        List<SortingReceiptDetail> details =
                sortingReceiptDetailMapper.selectByReceiptNo(receiptNo);
        boolean allCompleted = details.stream().allMatch(d -> "COMPLETED".equals(d.getStatus()));
        if (!allCompleted) {
            long pendingCount =
                    details.stream().filter(d -> !"COMPLETED".equals(d.getStatus())).count();
            throw new RuntimeException("还有" + pendingCount + "个SKU未完成整理，不能完成收货");
        }

        // 更新收货单状态
        receipt.setStatus("COMPLETED");
        receipt.setFinishTime(LocalDateTime.now());
        receipt.setUpdatedTime(LocalDateTime.now());
        sortingReceiptMapper.updateById(receipt);

        // TODO: 调用库存模块接口，增加整理后SKU的库存
        // TODO: 调用上架模块，生成上架任务

        log.info("整理收货完成: receiptNo={}, 总数量={}", receiptNo, receipt.getTotalQty());
        return receipt;
    }

    // ============================================================

    // 6. 查询方法
    // ============================================================

    /** 查询整理收货单详情 */
    public SortingReceipt getSortingReceipt(String receiptNo) {
        return sortingReceiptMapper.selectByReceiptNo(receiptNo);
    }

    /** 查询整理收货明细 */
    public List<SortingReceiptDetail> getSortingReceiptDetails(String receiptNo) {
        return sortingReceiptDetailMapper.selectByReceiptNo(receiptNo);
    }

    /** 查询整理收货箱 */
    public List<SortingReceiptBox> getSortingReceiptBoxes(String receiptNo) {
        return sortingReceiptBoxMapper.selectByReceiptNo(receiptNo);
    }

    /** 根据ASN查询整理收货单 */
    public List<SortingReceipt> getSortingReceiptsByAsn(String asnNo) {
        return sortingReceiptMapper.selectByAsnNo(asnNo);
    }

    /** 查询所有整理收货单 */
    public List<SortingReceipt> getAllSortingReceipts() {
        return sortingReceiptMapper.selectList(
                new LambdaQueryWrapper<SortingReceipt>()
                        .orderByDesc(SortingReceipt::getCreatedTime));
    }

    // ============================================================

    // 工具方法
    // ============================================================

    private String generateReceiptNo() {
        return "SR"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }

    private String generateBoxNo() {
        return "BOX"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }
}
