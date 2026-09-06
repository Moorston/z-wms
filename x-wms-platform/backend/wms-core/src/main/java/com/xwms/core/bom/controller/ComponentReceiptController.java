package com.xwms.core.bom.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.*;

import com.xwms.core.bom.entity.*;
import com.xwms.core.bom.service.ComponentReceiptService;

import lombok.RequiredArgsConstructor;

/** 组件扫描收货Controller */
@RestController
@RequestMapping("/api/bom")
@RequiredArgsConstructor
public class ComponentReceiptController {

    private final ComponentReceiptService componentReceiptService;

    // ==================== BOM组件维护 ====================

    /** 创建BOM */
    @PostMapping("/create")
    public ProductBom createBom(
            @RequestBody ProductBom bom,
            @RequestParam(required = false) List<ProductBomDetail> details,
            @RequestParam(required = false, defaultValue = "system") String creator) {
        return componentReceiptService.createBom(bom, details, creator);
    }

    /** 根据父件查询默认BOM */
    @GetMapping("/parent/{parentSkuCode}")
    public ProductBom getDefaultBomByParentSku(@PathVariable String parentSkuCode) {
        return componentReceiptService.getDefaultBomByParentSku(parentSkuCode);
    }

    /** 查询BOM明细 */
    @GetMapping("/{bomCode}/details")
    public List<ProductBomDetail> getBomDetails(@PathVariable String bomCode) {
        return componentReceiptService.getBomDetails(bomCode);
    }

    /** 查询所有BOM */
    @GetMapping("/list")
    public List<ProductBom> getAllBoms() {
        return componentReceiptService.getAllBoms();
    }

    // ==================== 组件扫描收货 ====================

    /** 开始组件扫描收货 */
    @PostMapping("/receipt/start")
    public ComponentReceipt startComponentReceipt(
            @RequestParam String bomCode,
            @RequestParam(required = false) String asnNo,
            @RequestParam(required = false) String inboundNo,
            @RequestParam(required = false) String ownerCode,
            @RequestParam(required = false) String warehouseCode,
            @RequestParam(required = false) String batchNo,
            @RequestParam(required = false) String locationCode,
            @RequestParam(required = false, defaultValue = "system") String operator) {
        return componentReceiptService.startComponentReceipt(
                bomCode,
                asnNo,
                inboundNo,
                ownerCode,
                warehouseCode,
                batchNo,
                locationCode,
                operator);
    }

    /** 扫描子件SKU */
    @PostMapping("/receipt/{receiptNo}/scan")
    public ComponentReceipt scanChildSku(
            @PathVariable String receiptNo,
            @RequestParam String childSkuCode,
            @RequestParam BigDecimal scanQty,
            @RequestParam(required = false, defaultValue = "system") String operator) {
        return componentReceiptService.scanChildSku(receiptNo, childSkuCode, scanQty, operator);
    }

    /** 确认组件收货 */
    @PostMapping("/receipt/{receiptNo}/confirm")
    public ComponentReceipt confirmComponentReceipt(
            @PathVariable String receiptNo,
            @RequestParam(required = false, defaultValue = "system") String operator) {
        return componentReceiptService.confirmComponentReceipt(receiptNo, operator);
    }

    /** 取消组件收货 */
    @PostMapping("/receipt/{receiptNo}/cancel")
    public Map<String, Object> cancelComponentReceipt(
            @PathVariable String receiptNo,
            @RequestParam(required = false, defaultValue = "system") String operator) {
        componentReceiptService.cancelComponentReceipt(receiptNo, operator);
        return Map.of("success", true, "message", "组件收货已取消");
    }

    // ==================== 查询方法 ====================

    /** 查询组件收货单详情 */
    @GetMapping("/receipt/{receiptNo}")
    public ComponentReceipt getComponentReceipt(@PathVariable String receiptNo) {
        return componentReceiptService.getComponentReceipt(receiptNo);
    }

    /** 查询组件收货明细 */
    @GetMapping("/receipt/{receiptNo}/details")
    public List<ComponentReceiptDetail> getComponentReceiptDetails(@PathVariable String receiptNo) {
        return componentReceiptService.getComponentReceiptDetails(receiptNo);
    }

    /** 根据ASN查询组件收货单 */
    @GetMapping("/receipt/asn/{asnNo}")
    public List<ComponentReceipt> getComponentReceiptsByAsn(@PathVariable String asnNo) {
        return componentReceiptService.getComponentReceiptsByAsn(asnNo);
    }
}
