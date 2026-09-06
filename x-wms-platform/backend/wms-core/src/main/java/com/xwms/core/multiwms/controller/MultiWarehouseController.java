package com.xwms.core.multiwms.controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.common.core.Result;
import com.xwms.core.multiwms.entity.*;
import com.xwms.core.multiwms.service.MultiWarehouseService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/** 多仓协同 Controller */
@Tag(name = "多仓协同", description = "仓库间调拨/多仓库存查询/订单智能分配")
@RestController
@RequestMapping("/api/multi-warehouse")
@RequiredArgsConstructor
public class MultiWarehouseController {

    private final MultiWarehouseService multiWarehouseService;

    // ============================================================

    // 仓库间调拨
    // ============================================================

    @Operation(summary = "创建调拨单")
    @PostMapping("/transfers")
    public Result<TransferOrder> createTransfer(
            @RequestBody TransferOrder order, @RequestBody List<TransferDetail> details) {
        return Result.success(multiWarehouseService.createTransfer(order, details));
    }

    @Operation(summary = "查询调拨单详情")
    @GetMapping("/transfers/{id}")
    public Result<TransferOrder> getTransfer(@PathVariable Long id) {
        return Result.success(multiWarehouseService.getTransfer(id));
    }

    @Operation(summary = "分页查询调拨单")
    @GetMapping("/transfers")
    public Result<Page<TransferOrder>> pageTransfers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String fromWarehouse,
            @RequestParam(required = false) String toWarehouse,
            @RequestParam(required = false) String status) {
        return Result.success(
                multiWarehouseService.pageTransfers(
                        new Page<>(page, size), fromWarehouse, toWarehouse, status));
    }

    @Operation(summary = "查询调拨明细")
    @GetMapping("/transfers/{id}/details")
    public Result<List<TransferDetail>> getTransferDetails(@PathVariable Long id) {
        return Result.success(multiWarehouseService.getTransferDetails(id));
    }

    @Operation(summary = "确认调拨单")
    @PutMapping("/transfers/{id}/confirm")
    public Result<TransferOrder> confirmTransfer(
            @PathVariable Long id, @RequestParam String confirmedBy) {
        return Result.success(multiWarehouseService.confirmTransfer(id, confirmedBy));
    }

    @Operation(summary = "调拨出库")
    @PutMapping("/transfers/{id}/ship")
    public Result<TransferOrder> shipTransfer(
            @PathVariable Long id, @RequestBody List<TransferDetail> shippedDetails) {
        return Result.success(multiWarehouseService.shipTransfer(id, shippedDetails));
    }

    @Operation(summary = "调拨入库")
    @PutMapping("/transfers/{id}/receive")
    public Result<TransferOrder> receiveTransfer(
            @PathVariable Long id, @RequestBody List<TransferDetail> receivedDetails) {
        return Result.success(multiWarehouseService.receiveTransfer(id, receivedDetails));
    }

    @Operation(summary = "完成调拨单")
    @PutMapping("/transfers/{id}/complete")
    public Result<TransferOrder> completeTransfer(@PathVariable Long id) {
        return Result.success(multiWarehouseService.completeTransfer(id));
    }

    @Operation(summary = "取消调拨单")
    @PutMapping("/transfers/{id}/cancel")
    public Result<TransferOrder> cancelTransfer(
            @PathVariable Long id, @RequestParam(required = false) String remark) {
        return Result.success(multiWarehouseService.cancelTransfer(id, remark));
    }

    // ============================================================

    // 多仓库存查询
    // ============================================================

    @Operation(summary = "查询SKU多仓库存")
    @GetMapping("/stock/sku/{sku}")
    public Result<List<MultiWarehouseStock>> getStockBySku(@PathVariable String sku) {
        return Result.success(multiWarehouseService.getStockBySku(sku));
    }

    @Operation(summary = "查询仓库库存")
    @GetMapping("/stock/warehouse/{warehouse}")
    public Result<List<MultiWarehouseStock>> getStockByWarehouse(@PathVariable String warehouse) {
        return Result.success(multiWarehouseService.getStockByWarehouse(warehouse));
    }

    @Operation(summary = "查询SKU总可用库存")
    @GetMapping("/stock/total/{sku}")
    public Result<BigDecimal> getTotalAvailableQty(@PathVariable String sku) {
        return Result.success(multiWarehouseService.getTotalAvailableQty(sku));
    }

    @Operation(summary = "同步库存快照")
    @PostMapping("/stock/sync")
    public Result<Void> syncStockSnapshot(
            @RequestParam String warehouse,
            @RequestParam String sku,
            @RequestParam BigDecimal available,
            @RequestParam(required = false) BigDecimal allocated,
            @RequestParam(required = false) BigDecimal picking,
            @RequestParam(required = false) BigDecimal inTransit,
            @RequestParam(required = false) BigDecimal frozen,
            @RequestParam(required = false) BigDecimal safetyStock) {
        multiWarehouseService.syncStockSnapshot(
                warehouse,
                sku,
                available,
                allocated != null ? allocated : BigDecimal.ZERO,
                picking != null ? picking : BigDecimal.ZERO,
                inTransit != null ? inTransit : BigDecimal.ZERO,
                frozen != null ? frozen : BigDecimal.ZERO,
                safetyStock != null ? safetyStock : BigDecimal.ZERO);
        return Result.success();
    }

    // ============================================================

    // 订单智能分配
    // ============================================================

    @Operation(summary = "分配订单到仓库")
    @PostMapping("/allocate")
    public Result<OrderAllocation> allocateOrder(
            @RequestParam String orderNo,
            @RequestParam(required = false) String orderType,
            @RequestParam String sku,
            @RequestParam(required = false) String productName,
            @RequestParam BigDecimal requiredQty,
            @RequestParam(required = false) String customerAddress,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String ownerCode) {
        return Result.success(
                multiWarehouseService.allocateOrder(
                        orderNo,
                        orderType,
                        sku,
                        productName,
                        requiredQty,
                        customerAddress,
                        priority,
                        ownerCode));
    }

    @Operation(summary = "查询订单分配记录")
    @GetMapping("/allocate/order/{orderNo}")
    public Result<List<OrderAllocation>> getOrderAllocations(@PathVariable String orderNo) {
        return Result.success(multiWarehouseService.getOrderAllocations(orderNo));
    }

    @Operation(summary = "查询仓库待处理分配")
    @GetMapping("/allocate/pending/{warehouse}")
    public Result<List<OrderAllocation>> getPendingAllocations(@PathVariable String warehouse) {
        return Result.success(multiWarehouseService.getPendingAllocations(warehouse));
    }

    @Operation(summary = "标记分配已发货")
    @PutMapping("/allocate/{id}/shipped")
    public Result<Void> markAllocationShipped(@PathVariable Long id) {
        multiWarehouseService.markAllocationShipped(id);
        return Result.success();
    }
}
