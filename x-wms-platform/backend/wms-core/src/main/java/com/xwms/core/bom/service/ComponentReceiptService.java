package com.xwms.core.bom.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import com.xwms.core.bom.entity.*;
import com.xwms.core.bom.mapper.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 组件扫描收货服务 核心能力: BOM组件维护/组件扫描收货/子件→父件组装/异常处理 业务场景: 单一SKU整箱入库时，根据组件维护的BOM信息，扫描同时整理为配比箱 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ComponentReceiptService {

    private final ProductBomMapper productBomMapper;
    private final ProductBomDetailMapper productBomDetailMapper;
    private final ComponentReceiptMapper componentReceiptMapper;
    private final ComponentReceiptDetailMapper componentReceiptDetailMapper;

    private static final AtomicInteger SEQ = new AtomicInteger(0);
    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    // ============================================================

    // 1. BOM组件维护
    // ============================================================

    /** 创建BOM */
    @Transactional(rollbackFor = Exception.class)
    public ProductBom createBom(ProductBom bom, List<ProductBomDetail> details, String creator) {
        log.info("创建BOM: bomCode={}, parentSku={}", bom.getBomCode(), bom.getParentSkuCode());

        if (bom.getBomCode() == null) {
            bom.setBomCode(generateBomCode());
        }
        bom.setStatus("ENABLED");
        bom.setCreatedBy(creator);
        bom.setCreatedTime(LocalDateTime.now());
        productBomMapper.insert(bom);

        // 保存BOM明细
        int lineNo = 1;
        for (ProductBomDetail detail : details) {
            detail.setBomCode(bom.getBomCode());
            detail.setLineNo(lineNo++);
            if (detail.getIsKey() == null) detail.setIsKey("N");
            if (detail.getIsOptional() == null) detail.setIsOptional("N");
        }
        productBomDetailMapper.batchInsert(details);

        log.info("BOM创建完成: bomCode={}, 子件数={}", bom.getBomCode(), details.size());
        return bom;
    }

    /** 根据父件查询默认BOM */
    public ProductBom getDefaultBomByParentSku(String parentSkuCode) {
        return productBomMapper.selectDefaultByParentSku(parentSkuCode);
    }

    /** 查询BOM明细 */
    public List<ProductBomDetail> getBomDetails(String bomCode) {
        return productBomDetailMapper.selectByBomCode(bomCode);
    }

    /** 查询所有BOM */
    public List<ProductBom> getAllBoms() {
        return productBomMapper.selectList(
                new LambdaQueryWrapper<ProductBom>().orderByDesc(ProductBom::getCreatedTime));
    }

    // ============================================================

    // 2. 组件扫描收货（子件→父件组装）
    // ============================================================

    /** 开始组件扫描收货 选择ASN中的父件行，系统显示父件对应的BOM组合 */
    @Transactional(rollbackFor = Exception.class)
    public ComponentReceipt startComponentReceipt(
            String bomCode,
            String asnNo,
            String inboundNo,
            String ownerCode,
            String warehouseCode,
            String batchNo,
            String locationCode,
            String operator) {
        log.info("开始组件扫描收货: bomCode={}, asnNo={}", bomCode, asnNo);

        ProductBom bom = productBomMapper.selectByBomCode(bomCode);
        if (bom == null) {
            throw new RuntimeException("BOM不存在: " + bomCode);
        }
        if (!"ENABLED".equals(bom.getStatus())) {
            throw new RuntimeException("BOM未启用: " + bomCode);
        }

        // 查询BOM明细
        List<ProductBomDetail> bomDetails = productBomDetailMapper.selectByBomCode(bomCode);
        if (bomDetails == null || bomDetails.isEmpty()) {
            throw new RuntimeException("BOM明细为空: " + bomCode);
        }

        // 创建组件收货单
        String receiptNo = generateReceiptNo();
        ComponentReceipt receipt = new ComponentReceipt();
        receipt.setReceiptNo(receiptNo);
        receipt.setBomCode(bomCode);
        receipt.setParentSkuCode(bom.getParentSkuCode());
        receipt.setParentSkuName(bom.getParentSkuName());
        receipt.setAsnNo(asnNo);
        receipt.setInboundNo(inboundNo);
        receipt.setOwnerCode(ownerCode);
        receipt.setWarehouseCode(warehouseCode);
        receipt.setBatchNo(batchNo);
        receipt.setLocationCode(locationCode);
        receipt.setParentQty(BigDecimal.ZERO);
        receipt.setScannedChildCount(0);
        receipt.setRequiredChildCount(
                (int) bomDetails.stream().filter(d -> "N".equals(d.getIsOptional())).count());
        receipt.setStatus("SCANNING");
        receipt.setBomMatched("N");
        receipt.setOperator(operator);
        receipt.setStartTime(LocalDateTime.now());
        receipt.setCreatedBy(operator);
        receipt.setCreatedTime(LocalDateTime.now());
        componentReceiptMapper.insert(receipt);

        // 创建组件收货明细（初始化扫描数量为0）
        List<ComponentReceiptDetail> details = new ArrayList<>();
        for (ProductBomDetail bomDetail : bomDetails) {
            ComponentReceiptDetail detail = new ComponentReceiptDetail();
            detail.setReceiptNo(receiptNo);
            detail.setBomCode(bomCode);
            detail.setChildSkuCode(bomDetail.getChildSkuCode());
            detail.setChildSkuName(bomDetail.getChildSkuName());
            detail.setRequiredQty(bomDetail.getChildQty());
            detail.setScannedQty(BigDecimal.ZERO);
            detail.setSatisfied("N");
            detail.setIsOptional(bomDetail.getIsOptional());
            detail.setCreatedBy(operator);
            detail.setCreatedTime(LocalDateTime.now());
            details.add(detail);
        }
        componentReceiptDetailMapper.batchInsert(details);

        log.info(
                "组件扫描收货开始: receiptNo={}, 父件={}, 子件数={}",
                receiptNo,
                bom.getParentSkuCode(),
                details.size());
        return receipt;
    }

    /** 扫描子件SKU 当产品、数量满足BOM组合时，弹出提示提醒确认收货 */
    @Transactional(rollbackFor = Exception.class)
    public ComponentReceipt scanChildSku(
            String receiptNo, String childSkuCode, BigDecimal scanQty, String operator) {
        log.info("扫描子件: receiptNo={}, childSku={}, qty={}", receiptNo, childSkuCode, scanQty);

        ComponentReceipt receipt = componentReceiptMapper.selectByReceiptNo(receiptNo);
        if (receipt == null) {
            throw new RuntimeException("组件收货单不存在: " + receiptNo);
        }
        if (!"SCANNING".equals(receipt.getStatus())) {
            throw new RuntimeException("组件收货单状态不允许扫描: " + receipt.getStatus());
        }

        // 查询子件明细
        ComponentReceiptDetail detail =
                componentReceiptDetailMapper.selectByReceiptNoAndChildSku(receiptNo, childSkuCode);
        if (detail == null) {
            throw new RuntimeException("子件不在BOM中: " + childSkuCode);
        }

        // 检查子件数量是否超过BOM要求
        BigDecimal newScannedQty = detail.getScannedQty().add(scanQty);
        if ("N".equals(detail.getIsOptional())
                && newScannedQty.compareTo(detail.getRequiredQty()) > 0) {
            throw new RuntimeException(
                    "子件数量超过BOM要求: "
                            + childSkuCode
                            + ", 要求="
                            + detail.getRequiredQty()
                            + ", 已扫描="
                            + detail.getScannedQty()
                            + ", 本次="
                            + scanQty);
        }

        // 更新子件扫描数量
        detail.setScannedQty(newScannedQty);
        detail.setScanTime(LocalDateTime.now());
        if (newScannedQty.compareTo(detail.getRequiredQty()) >= 0) {
            detail.setSatisfied("Y");
        }
        detail.setUpdatedTime(LocalDateTime.now());
        componentReceiptDetailMapper.updateById(detail);

        // 更新收货单已扫描子件种类数
        List<ComponentReceiptDetail> allDetails =
                componentReceiptDetailMapper.selectByReceiptNo(receiptNo);
        long satisfiedCount = allDetails.stream().filter(d -> "Y".equals(d.getSatisfied())).count();
        receipt.setScannedChildCount((int) satisfiedCount);

        // 检查是否满足BOM组合（所有必选件都满足）
        boolean allRequiredSatisfied =
                allDetails.stream()
                        .filter(d -> "N".equals(d.getIsOptional()))
                        .allMatch(d -> "Y".equals(d.getSatisfied()));

        if (allRequiredSatisfied) {
            receipt.setBomMatched("Y");
            // 计算父件数量（取所有子件能组成的最小套数）
            BigDecimal minSets =
                    allDetails.stream()
                            .filter(d -> "N".equals(d.getIsOptional()))
                            .map(
                                    d ->
                                            d.getScannedQty()
                                                    .divide(
                                                            d.getRequiredQty(),
                                                            4,
                                                            BigDecimal.ROUND_DOWN))
                            .min(BigDecimal::compareTo)
                            .orElse(BigDecimal.ZERO);
            receipt.setParentQty(minSets);
        }

        receipt.setUpdatedTime(LocalDateTime.now());
        componentReceiptMapper.updateById(receipt);

        log.info(
                "子件扫描完成: receiptNo={}, childSku={}, 已满足={}/{}, BOM匹配={}",
                receiptNo,
                childSkuCode,
                satisfiedCount,
                receipt.getRequiredChildCount(),
                receipt.getBomMatched());
        return receipt;
    }

    /** 确认组件收货（BOM组合满足后，父件库存增加，触发标签打印） */
    @Transactional(rollbackFor = Exception.class)
    public ComponentReceipt confirmComponentReceipt(String receiptNo, String operator) {
        log.info("确认组件收货: receiptNo={}", receiptNo);

        ComponentReceipt receipt = componentReceiptMapper.selectByReceiptNo(receiptNo);
        if (receipt == null) {
            throw new RuntimeException("组件收货单不存在: " + receiptNo);
        }
        if (!"SCANNING".equals(receipt.getStatus())) {
            throw new RuntimeException("组件收货单状态不允许确认: " + receipt.getStatus());
        }
        if (!"Y".equals(receipt.getBomMatched())) {
            throw new RuntimeException("BOM组合未满足，无法确认收货");
        }

        // 更新收货单状态
        receipt.setStatus("COMPLETED");
        receipt.setFinishTime(LocalDateTime.now());
        receipt.setUpdatedTime(LocalDateTime.now());
        componentReceiptMapper.updateById(receipt);

        // TODO: 调用库存模块接口，增加父件库存
        // TODO: 调用标签打印模块，触发父件标签打印

        log.info(
                "组件收货确认完成: receiptNo={}, 父件={}, 数量={}",
                receiptNo,
                receipt.getParentSkuCode(),
                receipt.getParentQty());
        return receipt;
    }

    /** 取消组件收货 */
    @Transactional(rollbackFor = Exception.class)
    public void cancelComponentReceipt(String receiptNo, String operator) {
        log.info("取消组件收货: receiptNo={}", receiptNo);

        ComponentReceipt receipt = componentReceiptMapper.selectByReceiptNo(receiptNo);
        if (receipt == null) {
            throw new RuntimeException("组件收货单不存在: " + receiptNo);
        }
        if ("COMPLETED".equals(receipt.getStatus())) {
            throw new RuntimeException("已完成的组件收货单不允许取消");
        }

        receipt.setStatus("CANCELLED");
        receipt.setUpdatedTime(LocalDateTime.now());
        componentReceiptMapper.updateById(receipt);

        log.info("组件收货已取消: receiptNo={}", receiptNo);
    }

    // ============================================================

    // 3. 查询方法
    // ============================================================

    /** 查询组件收货单详情 */
    public ComponentReceipt getComponentReceipt(String receiptNo) {
        return componentReceiptMapper.selectByReceiptNo(receiptNo);
    }

    /** 查询组件收货明细 */
    public List<ComponentReceiptDetail> getComponentReceiptDetails(String receiptNo) {
        return componentReceiptDetailMapper.selectByReceiptNo(receiptNo);
    }

    /** 根据ASN查询组件收货单 */
    public List<ComponentReceipt> getComponentReceiptsByAsn(String asnNo) {
        return componentReceiptMapper.selectByAsnNo(asnNo);
    }

    // ============================================================

    // 工具方法
    // ============================================================

    private String generateBomCode() {
        return "BOM"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }

    private String generateReceiptNo() {
        return "CR"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }
}
