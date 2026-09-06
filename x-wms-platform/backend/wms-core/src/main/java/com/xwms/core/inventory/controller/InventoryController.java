package com.xwms.core.inventory.controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.common.core.Result;
import com.xwms.core.inventory.entity.Inventory;
import com.xwms.core.inventory.service.InventoryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * 库存管理 Controller 职责边界: 只负责库存核心操作(查询/增减/预占/释放预占) 库存冻结 -> freeze模块 库存调整 -> adjust模块 库存流水 ->
 * transaction模块
 */
@Tag(name = "库存管理", description = "库存查询/增减/预占/释放预占")
@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    // ============================================================

    // 库存查询
    // ============================================================

    @Operation(summary = "分页查询库存")
    @GetMapping
    public Result<Page<Inventory>> pageInventory(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String warehouseCode,
            @RequestParam(required = false) String locationCode,
            @RequestParam(required = false) String skuCode,
            @RequestParam(required = false) String batchNo,
            @RequestParam(required = false) String ownerCode) {
        return Result.success(
                inventoryService.pageInventory(
                        new Page<>(page, size),
                        warehouseCode,
                        locationCode,
                        skuCode,
                        batchNo,
                        ownerCode));
    }

    @Operation(summary = "按唯一键查询库存")
    @GetMapping("/detail")
    public Result<Inventory> getInventory(
            @RequestParam String warehouseCode,
            @RequestParam String locationCode,
            @RequestParam String skuCode,
            @RequestParam(required = false) String batchNo,
            @RequestParam String ownerCode) {
        return Result.success(
                inventoryService.getInventory(
                        warehouseCode, locationCode, skuCode, batchNo, ownerCode));
    }

    @Operation(summary = "按SKU查询库存")
    @GetMapping("/sku/{skuCode}")
    public Result<List<Inventory>> getInventoryBySku(
            @PathVariable String skuCode, @RequestParam String ownerCode) {
        return Result.success(inventoryService.getInventoryBySku(skuCode, ownerCode));
    }

    @Operation(summary = "按仓库查询库存")
    @GetMapping("/warehouse/{warehouseCode}")
    public Result<List<Inventory>> getInventoryByWarehouse(
            @PathVariable String warehouseCode, @RequestParam String ownerCode) {
        return Result.success(inventoryService.getInventoryByWarehouse(warehouseCode, ownerCode));
    }

    // ============================================================

    // 库存增减
    // ============================================================

    @Operation(summary = "库存增加（入库）")
    @PostMapping("/add")
    public Result<Inventory> addInventory(
            @RequestParam String warehouseCode,
            @RequestParam String locationCode,
            @RequestParam String skuCode,
            @RequestParam(required = false) String batchNo,
            @RequestParam String ownerCode,
            @RequestParam BigDecimal qty,
            @RequestParam(required = false) String refType,
            @RequestParam(required = false) String refNo,
            @RequestParam(required = false) String operator) {
        return Result.success(
                inventoryService.addInventory(
                        warehouseCode,
                        locationCode,
                        skuCode,
                        batchNo,
                        ownerCode,
                        qty,
                        refType,
                        refNo,
                        operator));
    }

    @Operation(summary = "库存扣减（出库）")
    @PostMapping("/deduct")
    public Result<Inventory> deductInventory(
            @RequestParam String warehouseCode,
            @RequestParam String locationCode,
            @RequestParam String skuCode,
            @RequestParam(required = false) String batchNo,
            @RequestParam String ownerCode,
            @RequestParam BigDecimal qty,
            @RequestParam(required = false) String refType,
            @RequestParam(required = false) String refNo,
            @RequestParam(required = false) String operator) {
        return Result.success(
                inventoryService.deductInventory(
                        warehouseCode,
                        locationCode,
                        skuCode,
                        batchNo,
                        ownerCode,
                        qty,
                        refType,
                        refNo,
                        operator));
    }

    // ============================================================

    // 库存预占
    // ============================================================

    @Operation(summary = "库存预占（分配）")
    @PostMapping("/allocate")
    public Result<Inventory> allocateInventory(
            @RequestParam String warehouseCode,
            @RequestParam String locationCode,
            @RequestParam String skuCode,
            @RequestParam(required = false) String batchNo,
            @RequestParam String ownerCode,
            @RequestParam BigDecimal qty,
            @RequestParam(required = false) String refType,
            @RequestParam(required = false) String refNo,
            @RequestParam(required = false) String operator) {
        return Result.success(
                inventoryService.allocateInventory(
                        warehouseCode,
                        locationCode,
                        skuCode,
                        batchNo,
                        ownerCode,
                        qty,
                        refType,
                        refNo,
                        operator));
    }

    @Operation(summary = "释放预占")
    @PostMapping("/release")
    public Result<Inventory> releaseAllocation(
            @RequestParam String warehouseCode,
            @RequestParam String locationCode,
            @RequestParam String skuCode,
            @RequestParam(required = false) String batchNo,
            @RequestParam String ownerCode,
            @RequestParam BigDecimal qty,
            @RequestParam(required = false) String refType,
            @RequestParam(required = false) String refNo,
            @RequestParam(required = false) String operator) {
        return Result.success(
                inventoryService.releaseAllocation(
                        warehouseCode,
                        locationCode,
                        skuCode,
                        batchNo,
                        ownerCode,
                        qty,
                        refType,
                        refNo,
                        operator));
    }
}
