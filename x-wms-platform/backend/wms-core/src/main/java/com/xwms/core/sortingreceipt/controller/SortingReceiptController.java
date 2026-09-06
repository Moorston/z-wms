package com.xwms.core.sortingreceipt.controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.xwms.core.sortingreceipt.entity.*;
import com.xwms.core.sortingreceipt.service.SortingReceiptService;

import lombok.RequiredArgsConstructor;

/** 整理收货Controller 服装行业配比箱拆箱整理 */
@RestController
@RequestMapping("/api/sorting-receipt")
@RequiredArgsConstructor
public class SortingReceiptController {

    private final SortingReceiptService sortingReceiptService;

    // ==================== 整理收货单管理 ====================

    /** 创建整理收货单 */
    @PostMapping("/create")
    public SortingReceipt createSortingReceipt(
            @RequestParam(required = false) String asnNo,
            @RequestParam(required = false) String inboundNo,
            @RequestParam String ownerCode,
            @RequestParam String warehouseCode,
            @RequestParam(required = false) String groupNo,
            @RequestParam String styleCode,
            @RequestParam String colorCode,
            @RequestParam Integer boxCount,
            @RequestParam(required = false) String locationCode,
            @RequestBody List<SortingReceiptDetail> details,
            @RequestParam(required = false, defaultValue = "system") String operator) {
        return sortingReceiptService.createSortingReceipt(
                asnNo,
                inboundNo,
                ownerCode,
                warehouseCode,
                groupNo,
                styleCode,
                colorCode,
                boxCount,
                locationCode,
                details,
                operator);
    }

    /** 扫描SKU条码 */
    @PostMapping("/{receiptNo}/scan")
    public SortingReceiptDetail scanSku(
            @PathVariable String receiptNo,
            @RequestParam String skuCode,
            @RequestParam BigDecimal scanQty,
            @RequestParam(required = false, defaultValue = "system") String operator) {
        return sortingReceiptService.scanSku(receiptNo, skuCode, scanQty, operator);
    }

    /** 满箱收货 */
    @PostMapping("/{receiptNo}/full-box-receive")
    public SortingReceiptBox fullBoxReceive(
            @PathVariable String receiptNo,
            @RequestParam String skuCode,
            @RequestParam(required = false, defaultValue = "system") String operator) {
        return sortingReceiptService.fullBoxReceive(receiptNo, skuCode, operator);
    }

    /** 手工确认收货（未满箱） */
    @PostMapping("/{receiptNo}/manual-receive")
    public SortingReceiptBox manualReceive(
            @PathVariable String receiptNo,
            @RequestParam String skuCode,
            @RequestParam BigDecimal receiveQty,
            @RequestParam(required = false, defaultValue = "system") String operator) {
        return sortingReceiptService.manualReceive(receiptNo, skuCode, receiveQty, operator);
    }

    /** 完成整理收货 */
    @PostMapping("/{receiptNo}/complete")
    public SortingReceipt completeSortingReceipt(
            @PathVariable String receiptNo,
            @RequestParam(required = false, defaultValue = "system") String operator) {
        return sortingReceiptService.completeSortingReceipt(receiptNo, operator);
    }

    // ==================== 查询方法 ====================

    /** 查询整理收货单详情 */
    @GetMapping("/{receiptNo}")
    public SortingReceipt getSortingReceipt(@PathVariable String receiptNo) {
        return sortingReceiptService.getSortingReceipt(receiptNo);
    }

    /** 查询整理收货明细 */
    @GetMapping("/{receiptNo}/details")
    public List<SortingReceiptDetail> getSortingReceiptDetails(@PathVariable String receiptNo) {
        return sortingReceiptService.getSortingReceiptDetails(receiptNo);
    }

    /** 查询整理收货箱 */
    @GetMapping("/{receiptNo}/boxes")
    public List<SortingReceiptBox> getSortingReceiptBoxes(@PathVariable String receiptNo) {
        return sortingReceiptService.getSortingReceiptBoxes(receiptNo);
    }

    /** 根据ASN查询整理收货单 */
    @GetMapping("/asn/{asnNo}")
    public List<SortingReceipt> getSortingReceiptsByAsn(@PathVariable String asnNo) {
        return sortingReceiptService.getSortingReceiptsByAsn(asnNo);
    }

    /** 查询所有整理收货单 */
    @GetMapping("/list")
    public List<SortingReceipt> getAllSortingReceipts() {
        return sortingReceiptService.getAllSortingReceipts();
    }
}
