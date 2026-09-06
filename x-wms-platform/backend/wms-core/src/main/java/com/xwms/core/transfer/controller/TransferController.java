package com.xwms.core.transfer.controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.common.core.Result;
import com.xwms.core.transfer.entity.*;
import com.xwms.core.transfer.service.TransferService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/** 调拨管理 Controller */
@Tag(name = "调拨管理", description = "调拨单/审批/发运/在途/收货")
@RestController
@RequestMapping("/api/transfer")
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;

    // ============================================================

    // 调拨单管理
    // ============================================================

    @Operation(summary = "创建调拨单")
    @PostMapping
    public Result<Transfer> createTransfer(
            @RequestBody Transfer transfer,
            @RequestParam(required = false) List<TransferDetail> details,
            @RequestParam(required = false) String operator) {
        return Result.success(transferService.createTransfer(transfer, details, operator));
    }

    @Operation(summary = "审批调拨单")
    @PutMapping("/{transferNo}/approve")
    public Result<Transfer> approveTransfer(
            @PathVariable String transferNo, @RequestParam String approver) {
        return Result.success(transferService.approveTransfer(transferNo, approver));
    }

    @Operation(summary = "分页查询调拨单")
    @GetMapping
    public Result<Page<Transfer>> pageTransfers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String transferType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String fromWarehouse,
            @RequestParam(required = false) String toWarehouse) {
        return Result.success(
                transferService.pageTransfers(
                        new Page<>(page, size), transferType, status, fromWarehouse, toWarehouse));
    }

    @Operation(summary = "按调拨单号查询")
    @GetMapping("/{transferNo}")
    public Result<Transfer> getTransferByNo(@PathVariable String transferNo) {
        return Result.success(transferService.getTransferByNo(transferNo));
    }

    @Operation(summary = "查询调拨明细")
    @GetMapping("/{transferNo}/details")
    public Result<List<TransferDetail>> getDetails(@PathVariable String transferNo) {
        return Result.success(transferService.getDetails(transferNo));
    }

    // ============================================================

    // 调拨发运
    // ============================================================

    @Operation(summary = "调拨发运")
    @PostMapping("/{transferNo}/ship")
    public Result<TransferTask> shipTransfer(
            @PathVariable String transferNo,
            @RequestParam String skuCode,
            @RequestParam(required = false) String batchNo,
            @RequestParam String fromLocation,
            @RequestParam BigDecimal shipQty,
            @RequestParam(required = false) String carrier,
            @RequestParam(required = false) String trackingNo,
            @RequestParam(required = false) String operator) {
        return Result.success(
                transferService.shipTransfer(
                        transferNo,
                        skuCode,
                        batchNo,
                        fromLocation,
                        shipQty,
                        carrier,
                        trackingNo,
                        operator));
    }

    // ============================================================

    // 在途库存
    // ============================================================

    @Operation(summary = "分页查询在途库存")
    @GetMapping("/in-transit")
    public Result<Page<TransferInTransit>> pageInTransit(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String skuCode,
            @RequestParam(required = false) String fromWarehouse,
            @RequestParam(required = false) String toWarehouse,
            @RequestParam(required = false) String status) {
        return Result.success(
                transferService.pageInTransit(
                        new Page<>(page, size), skuCode, fromWarehouse, toWarehouse, status));
    }

    @Operation(summary = "按调拨单查询在途库存")
    @GetMapping("/{transferNo}/in-transit")
    public Result<List<TransferInTransit>> getInTransitByTransfer(@PathVariable String transferNo) {
        return Result.success(transferService.getInTransitByTransfer(transferNo));
    }

    @Operation(summary = "查询仓库待收在途库存")
    @GetMapping("/in-transit/incoming/{warehouseCode}")
    public Result<List<TransferInTransit>> getIncomingByWarehouse(
            @PathVariable String warehouseCode) {
        return Result.success(transferService.getIncomingByWarehouse(warehouseCode));
    }

    // ============================================================

    // 调拨收货
    // ============================================================

    @Operation(summary = "调拨收货")
    @PostMapping("/{transferNo}/receive")
    public Result<TransferTask> receiveTransfer(
            @PathVariable String transferNo,
            @RequestParam String skuCode,
            @RequestParam(required = false) String batchNo,
            @RequestParam String toLocation,
            @RequestParam BigDecimal receivedQty,
            @RequestParam(required = false) BigDecimal differenceQty,
            @RequestParam(required = false) String operator) {
        return Result.success(
                transferService.receiveTransfer(
                        transferNo,
                        skuCode,
                        batchNo,
                        toLocation,
                        receivedQty,
                        differenceQty,
                        operator));
    }

    // ============================================================

    // 作业记录
    // ============================================================

    @Operation(summary = "查询作业记录")
    @GetMapping("/{transferNo}/tasks")
    public Result<List<TransferTask>> getTasks(@PathVariable String transferNo) {
        return Result.success(transferService.getTasks(transferNo));
    }
}
