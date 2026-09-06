package com.xwms.core.receipt.controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.common.core.Result;
import com.xwms.core.inbound.entity.Asn;
import com.xwms.core.receipt.entity.*;
import com.xwms.core.receipt.service.ReceiptService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/** 收货管理控制器 支持12种收货方式 */
@Tag(name = "收货管理", description = "多种收货方式/扫描收货/盲收/取消收货")
@RestController
@RequestMapping("/api/receipt")
@RequiredArgsConstructor
public class ReceiptController {

    private final ReceiptService receiptService;

    // ============================================================

    // 1. 收货任务管理
    // ============================================================

    @Operation(summary = "从ASN生成收货任务")
    @PostMapping("/task/from-asn/{asnNo}")
    public Result<ReceiptTask> createTaskFromAsn(
            @PathVariable String asnNo,
            @RequestParam(required = false) String receiptType,
            @RequestParam(required = false) String scanMode,
            @RequestParam String operator) {
        return Result.success(
                receiptService.createTaskFromAsn(asnNo, receiptType, scanMode, operator));
    }

    @Operation(summary = "取消收货任务")
    @PutMapping("/task/{taskNo}/cancel")
    public Result<ReceiptTask> cancelTask(
            @PathVariable String taskNo,
            @RequestParam String cancelReason,
            @RequestParam String operator) {
        return Result.success(receiptService.cancelTask(taskNo, cancelReason, operator));
    }

    @Operation(summary = "查询收货任务")
    @GetMapping("/task/{taskNo}")
    public Result<ReceiptTask> getTaskByNo(@PathVariable String taskNo) {
        return Result.success(receiptService.getTaskByNo(taskNo));
    }

    @Operation(summary = "查询收货任务明细")
    @GetMapping("/task/{taskNo}/details")
    public Result<List<ReceiptTaskDetail>> getTaskDetails(@PathVariable String taskNo) {
        return Result.success(receiptService.getTaskDetails(taskNo));
    }

    @Operation(summary = "分页查询收货任务")
    @GetMapping("/task")
    public Result<Page<ReceiptTask>> pageTasks(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String receiptType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String warehouseCode,
            @RequestParam(required = false) String asnNo) {
        return Result.success(
                receiptService.pageTasks(
                        new Page<>(page, size), receiptType, status, warehouseCode, asnNo));
    }

    // ============================================================

    // 2. B1 按ASN整单收货
    // ============================================================

    @Operation(summary = "按ASN整单收货")
    @PostMapping("/asn/{taskNo}")
    public Result<ReceiptTask> receiveByAsn(
            @PathVariable String taskNo,
            @RequestParam(required = false) String receiveLocation,
            @RequestParam String operator) {
        return Result.success(receiptService.receiveByAsn(taskNo, receiveLocation, operator));
    }

    // ============================================================

    // 3. B2 部分收货
    // ============================================================

    @Operation(summary = "部分收货")
    @PostMapping("/partial")
    public Result<ReceiptRecord> partialReceive(
            @RequestParam String taskNo,
            @RequestParam String detailNo,
            @RequestParam BigDecimal receiveQty,
            @RequestParam(required = false) String batchNo,
            @RequestParam(required = false) String receiveLocation,
            @RequestParam String operator) {
        return Result.success(
                receiptService.partialReceive(
                        taskNo, detailNo, receiveQty, batchNo, receiveLocation, operator));
    }

    // ============================================================

    // 4. B4 扫描收货
    // ============================================================

    @Operation(summary = "扫描收货")
    @PostMapping("/scan")
    public Result<ReceiptService.ScanResult> scanReceive(
            @RequestParam String taskNo,
            @RequestParam String scanContent,
            @RequestParam(defaultValue = "BARCODE") String scanType,
            @RequestParam(defaultValue = "BATCH") String scanMode,
            @RequestParam(required = false) String receiveLocation,
            @RequestParam String operator) {
        return Result.success(
                receiptService.scanReceive(
                        taskNo, scanContent, scanType, scanMode, receiveLocation, operator));
    }

    // ============================================================

    // 5. B5 按箱收货
    // ============================================================

    @Operation(summary = "按箱收货")
    @PostMapping("/box")
    public Result<ReceiptRecord> receiveByBox(
            @RequestParam String taskNo,
            @RequestParam String detailNo,
            @RequestParam String lpnNo,
            @RequestParam BigDecimal boxQty,
            @RequestParam(required = false) String receiveLocation,
            @RequestParam String operator) {
        return Result.success(
                receiptService.receiveByBox(
                        taskNo, detailNo, lpnNo, boxQty, receiveLocation, operator));
    }

    // ============================================================

    // 6. B6 快捷收货
    // ============================================================

    @Operation(summary = "快捷收货")
    @PostMapping("/quick/{taskNo}")
    public Result<ReceiptTask> quickReceive(
            @PathVariable String taskNo,
            @RequestBody List<ReceiptService.QuickReceiveItem> items,
            @RequestParam String operator) {
        return Result.success(receiptService.quickReceive(taskNo, items, operator));
    }

    // ============================================================

    // 7. B8 混ASN扫描收货
    // ============================================================

    @Operation(summary = "混ASN/混PO扫描收货")
    @PostMapping("/mix-scan")
    public Result<ReceiptService.ScanResult> mixScanReceive(
            @RequestParam String warehouseCode,
            @RequestParam String scanContent,
            @RequestParam(defaultValue = "BARCODE") String scanType,
            @RequestParam(required = false) String receiveLocation,
            @RequestParam String operator) {
        return Result.success(
                receiptService.mixScanReceive(
                        warehouseCode, scanContent, scanType, receiveLocation, operator));
    }

    // ============================================================

    // 8. B11 预收货
    // ============================================================

    @Operation(summary = "预收货（不增加库存）")
    @PostMapping("/pre")
    public Result<ReceiptRecord> preReceive(
            @RequestParam String taskNo,
            @RequestParam String detailNo,
            @RequestParam BigDecimal preReceiveQty,
            @RequestParam(required = false) String receiveLocation,
            @RequestParam String operator) {
        return Result.success(
                receiptService.preReceive(
                        taskNo, detailNo, preReceiveQty, receiveLocation, operator));
    }

    // ============================================================

    // 9. A3 盲收
    // ============================================================

    @Operation(summary = "盲收（无单据收货）")
    @PostMapping("/blind")
    public Result<BlindReceipt> blindReceive(
            @RequestParam String warehouseCode,
            @RequestParam String ownerCode,
            @RequestParam String skuCode,
            @RequestParam BigDecimal blindQty,
            @RequestParam(required = false) String batchNo,
            @RequestParam(required = false) String receiveLocation,
            @RequestParam(required = false) String blindMode,
            @RequestParam String operator) {
        return Result.success(
                receiptService.blindReceive(
                        warehouseCode,
                        ownerCode,
                        skuCode,
                        blindQty,
                        batchNo,
                        receiveLocation,
                        blindMode,
                        operator));
    }

    @Operation(summary = "盲收匹配（匹配到PO/ASN）")
    @PutMapping("/blind/{blindNo}/match")
    public Result<BlindReceipt> matchBlindReceipt(
            @PathVariable String blindNo,
            @RequestParam(required = false) String poNo,
            @RequestParam(required = false) String asnNo,
            @RequestParam(required = false) String inboundNo,
            @RequestParam String operator) {
        return Result.success(
                receiptService.matchBlindReceipt(blindNo, poNo, asnNo, inboundNo, operator));
    }

    @Operation(summary = "盲收生成ASN（反向生成）")
    @PostMapping("/blind/{blindNo}/create-asn")
    public Result<Asn> createAsnFromBlind(
            @PathVariable String blindNo, @RequestParam String operator) {
        return Result.success(receiptService.createAsnFromBlind(blindNo, operator));
    }

    // ============================================================

    // 10. E5 取消收货
    // ============================================================

    @Operation(summary = "取消收货（仅收货未上架可取消）")
    @PutMapping("/record/{recordNo}/cancel")
    public Result<ReceiptRecord> cancelReceive(
            @PathVariable String recordNo,
            @RequestParam String cancelReason,
            @RequestParam String operator) {
        return Result.success(receiptService.cancelReceive(recordNo, cancelReason, operator));
    }

    // ============================================================

    // 11. 收货完成
    // ============================================================

    @Operation(summary = "标记收货完成")
    @PutMapping("/task/{taskNo}/complete")
    public Result<ReceiptTask> completeReceive(
            @PathVariable String taskNo, @RequestParam String operator) {
        return Result.success(receiptService.completeReceive(taskNo, operator));
    }
}
