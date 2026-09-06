package com.xwms.core.pool.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.common.core.Result;
import com.xwms.core.pool.entity.*;
import com.xwms.core.pool.service.InventoryPoolService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/** 库存共享/分配池管理 Controller */
@Tag(name = "库存共享/分配池管理", description = "共享规则/分配池/池化库存/池化分配")
@RestController
@RequestMapping("/api/pool")
@RequiredArgsConstructor
public class InventoryPoolController {

    private final InventoryPoolService poolService;

    // ============================================================

    // 共享规则
    // ============================================================

    @Operation(summary = "创建共享规则")
    @PostMapping("/share-rule")
    public Result<ShareRule> createShareRule(@RequestBody ShareRule rule) {
        return Result.success(poolService.createShareRule(rule));
    }

    @Operation(summary = "按编码查询共享规则")
    @GetMapping("/share-rule/{ruleCode}")
    public Result<ShareRule> getShareRuleByCode(@PathVariable String ruleCode) {
        return Result.success(poolService.getShareRuleByCode(ruleCode));
    }

    @Operation(summary = "按池查询共享规则")
    @GetMapping("/share-rule/pool/{poolCode}")
    public Result<List<ShareRule>> getShareRulesByPool(@PathVariable String poolCode) {
        return Result.success(poolService.getShareRulesByPool(poolCode));
    }

    @Operation(summary = "按仓库和类型查询共享规则")
    @GetMapping("/share-rule/warehouse")
    public Result<List<ShareRule>> getShareRulesByWarehouseAndType(
            @RequestParam String warehouseCode, @RequestParam String shareType) {
        return Result.success(
                poolService.getShareRulesByWarehouseAndType(warehouseCode, shareType));
    }

    @Operation(summary = "按货主对查询共享规则")
    @GetMapping("/share-rule/owner")
    public Result<List<ShareRule>> getShareRulesByOwnerPair(
            @RequestParam String sourceOwner, @RequestParam String targetOwner) {
        return Result.success(poolService.getShareRulesByOwnerPair(sourceOwner, targetOwner));
    }

    @Operation(summary = "分页查询共享规则")
    @GetMapping("/share-rule/list")
    public Result<Page<ShareRule>> pageShareRules(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String warehouseCode,
            @RequestParam(required = false) String shareType) {
        return Result.success(
                poolService.pageShareRules(new Page<>(page, size), warehouseCode, shareType));
    }

    // ============================================================

    // 分配池
    // ============================================================

    @Operation(summary = "创建分配池")
    @PostMapping("/pool")
    public Result<AllocationPool> createPool(@RequestBody AllocationPool pool) {
        return Result.success(poolService.createPool(pool));
    }

    @Operation(summary = "按编码查询分配池")
    @GetMapping("/pool/{poolCode}")
    public Result<AllocationPool> getPoolByCode(@PathVariable String poolCode) {
        return Result.success(poolService.getPoolByCode(poolCode));
    }

    @Operation(summary = "按仓库查询分配池")
    @GetMapping("/pool/warehouse/{warehouseCode}")
    public Result<List<AllocationPool>> getPoolsByWarehouse(@PathVariable String warehouseCode) {
        return Result.success(poolService.getPoolsByWarehouse(warehouseCode));
    }

    @Operation(summary = "按仓库和类型查询分配池")
    @GetMapping("/pool/warehouse-type")
    public Result<List<AllocationPool>> getPoolsByWarehouseAndType(
            @RequestParam String warehouseCode, @RequestParam String poolType) {
        return Result.success(poolService.getPoolsByWarehouseAndType(warehouseCode, poolType));
    }

    @Operation(summary = "按货主查询分配池")
    @GetMapping("/pool/owner/{ownerCode}")
    public Result<List<AllocationPool>> getPoolsByOwner(@PathVariable String ownerCode) {
        return Result.success(poolService.getPoolsByOwner(ownerCode));
    }

    @Operation(summary = "分页查询分配池")
    @GetMapping("/pool/list")
    public Result<Page<AllocationPool>> pagePools(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String warehouseCode,
            @RequestParam(required = false) String poolType) {
        return Result.success(
                poolService.pagePools(new Page<>(page, size), warehouseCode, poolType));
    }

    // ============================================================

    // 池化库存
    // ============================================================

    @Operation(summary = "添加池化库存")
    @PostMapping("/inventory")
    public Result<PoolInventory> addPoolInventory(@RequestBody PoolInventory inventory) {
        return Result.success(poolService.addPoolInventory(inventory));
    }

    @Operation(summary = "按池查询池化库存")
    @GetMapping("/inventory/pool/{poolCode}")
    public Result<List<PoolInventory>> getPoolInventoryByPool(@PathVariable String poolCode) {
        return Result.success(poolService.getPoolInventoryByPool(poolCode));
    }

    @Operation(summary = "按池和SKU查询池化库存")
    @GetMapping("/inventory/pool-sku")
    public Result<List<PoolInventory>> getPoolInventoryByPoolAndSku(
            @RequestParam String poolCode, @RequestParam String skuCode) {
        return Result.success(poolService.getPoolInventoryByPoolAndSku(poolCode, skuCode));
    }

    @Operation(summary = "查询可用池化库存")
    @GetMapping("/inventory/available")
    public Result<List<PoolInventory>> getAvailableInventory(
            @RequestParam String warehouseCode, @RequestParam String skuCode) {
        return Result.success(
                poolService.getAvailableInventoryByWarehouseAndSku(warehouseCode, skuCode));
    }

    @Operation(summary = "分页查询池化库存")
    @GetMapping("/inventory/list")
    public Result<Page<PoolInventory>> pagePoolInventory(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String poolCode,
            @RequestParam(required = false) String warehouseCode,
            @RequestParam(required = false) String skuCode) {
        return Result.success(
                poolService.pagePoolInventory(
                        new Page<>(page, size), poolCode, warehouseCode, skuCode));
    }

    // ============================================================

    // 池化分配
    // ============================================================

    @Operation(summary = "从分配池分配库存")
    @PostMapping("/allocate")
    public Result<String> allocateFromPool(
            @RequestParam String orderNo,
            @RequestParam String warehouseCode,
            @RequestParam String targetOwner,
            @RequestParam String skuCode,
            @RequestParam BigDecimal needQty,
            @RequestParam String operator) {
        return Result.success(
                poolService.allocateFromPool(
                        orderNo, warehouseCode, targetOwner, skuCode, needQty, operator));
    }

    @Operation(summary = "释放池化分配")
    @PostMapping("/allocate/{allocationId}/release")
    public Result<Integer> releasePoolAllocation(
            @PathVariable String allocationId,
            @RequestParam String reason,
            @RequestParam String operator) {
        return Result.success(poolService.releasePoolAllocation(allocationId, reason, operator));
    }

    @Operation(summary = "池化分配拣货确认")
    @PostMapping("/allocate/{allocationId}/pick")
    public Result<PoolAllocation> confirmPoolPick(
            @PathVariable Long allocationId,
            @RequestParam BigDecimal pickedQty,
            @RequestParam String operator) {
        return Result.success(poolService.confirmPoolPick(allocationId, pickedQty, operator));
    }

    @Operation(summary = "按ID查询池化分配")
    @GetMapping("/allocate/{allocationId}")
    public Result<PoolAllocation> getPoolAllocationById(@PathVariable String allocationId) {
        return Result.success(poolService.getPoolAllocationById(allocationId));
    }

    @Operation(summary = "按订单查询池化分配")
    @GetMapping("/allocate/order/{orderNo}")
    public Result<List<PoolAllocation>> getPoolAllocationsByOrder(@PathVariable String orderNo) {
        return Result.success(poolService.getPoolAllocationsByOrder(orderNo));
    }

    @Operation(summary = "按池查询活跃分配")
    @GetMapping("/allocate/pool/{poolCode}")
    public Result<List<PoolAllocation>> getActiveAllocationsByPool(@PathVariable String poolCode) {
        return Result.success(poolService.getActiveAllocationsByPool(poolCode));
    }

    @Operation(summary = "按SKU查询活跃分配")
    @GetMapping("/allocate/sku/{skuCode}")
    public Result<List<PoolAllocation>> getActiveAllocationsBySku(@PathVariable String skuCode) {
        return Result.success(poolService.getActiveAllocationsBySku(skuCode));
    }

    @Operation(summary = "分页查询池化分配")
    @GetMapping("/allocate/list")
    public Result<Page<PoolAllocation>> pagePoolAllocations(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String poolCode,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) String skuCode,
            @RequestParam(required = false) String status) {
        return Result.success(
                poolService.pagePoolAllocations(
                        new Page<>(page, size), poolCode, orderNo, skuCode, status));
    }

    // ============================================================

    // 库存共享匹配
    // ============================================================

    @Operation(summary = "匹配可用共享库存")
    @GetMapping("/match-shared")
    public Result<List<Map<String, Object>>> matchSharedInventory(
            @RequestParam String warehouseCode,
            @RequestParam String targetOwner,
            @RequestParam String skuCode,
            @RequestParam BigDecimal needQty) {
        return Result.success(
                poolService.matchSharedInventory(warehouseCode, targetOwner, skuCode, needQty));
    }
}
