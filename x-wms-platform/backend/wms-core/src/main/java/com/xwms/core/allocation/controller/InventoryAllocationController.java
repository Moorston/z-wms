package com.xwms.core.allocation.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.common.core.Result;
import com.xwms.core.allocation.entity.*;
import com.xwms.core.allocation.service.InventoryAllocationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/** 库存分配策略管理 Controller */
@Tag(name = "库存分配策略管理", description = "分配规则/分配策略/分配执行/分配释放")
@RestController
@RequestMapping("/api/allocation")
@RequiredArgsConstructor
public class InventoryAllocationController {

    private final InventoryAllocationService allocationService;

    // ============================================================

    // 分配规则
    // ============================================================

    @Operation(summary = "创建分配规则")
    @PostMapping("/rule")
    public Result<AllocationRule> createRule(@RequestBody Map<String, Object> request) {
        AllocationRule rule = new AllocationRule();
        rule.setRuleCode((String) request.get("ruleCode"));
        rule.setRuleName((String) request.get("ruleName"));
        rule.setWarehouseCode((String) request.get("warehouseCode"));
        rule.setOwnerCode((String) request.get("ownerCode"));
        rule.setCategoryCode((String) request.get("categoryCode"));
        rule.setSkuCode((String) request.get("skuCode"));
        rule.setOrderType((String) request.get("orderType"));
        if (request.get("priority") != null) {
            rule.setPriority(Integer.parseInt(request.get("priority").toString()));
        }
        rule.setRemark((String) request.get("remark"));

        AllocationStrategy strategy = null;
        if (request.get("strategy") != null) {
            @SuppressWarnings("unchecked")
            Map<String, Object> strategyData = (Map<String, Object>) request.get("strategy");
            strategy = new AllocationStrategy();
            strategy.setStrategyType((String) strategyData.get("strategyType"));
            strategy.setSortField((String) strategyData.get("sortField"));
            strategy.setSortOrder((String) strategyData.get("sortOrder"));
            strategy.setLocationPriority((String) strategyData.get("locationPriority"));
            strategy.setBatchPriority((String) strategyData.get("batchPriority"));
            strategy.setAllowSplit((String) strategyData.get("allowSplit"));
            if (strategyData.get("minAllocQty") != null) {
                strategy.setMinAllocQty(new BigDecimal(strategyData.get("minAllocQty").toString()));
            }
            if (strategyData.get("maxAllocQty") != null) {
                strategy.setMaxAllocQty(new BigDecimal(strategyData.get("maxAllocQty").toString()));
            }
            if (strategyData.get("reserveHours") != null) {
                strategy.setReserveHours(
                        Integer.parseInt(strategyData.get("reserveHours").toString()));
            }
            strategy.setAutoRelease((String) strategyData.get("autoRelease"));
        }

        return Result.success(allocationService.createRule(rule, strategy));
    }

    @Operation(summary = "按编码查询分配规则")
    @GetMapping("/rule/{ruleCode}")
    public Result<AllocationRule> getRuleByCode(@PathVariable String ruleCode) {
        return Result.success(allocationService.getRuleByCode(ruleCode));
    }

    @Operation(summary = "查询分配策略")
    @GetMapping("/rule/{ruleCode}/strategy")
    public Result<AllocationStrategy> getStrategyByRuleCode(@PathVariable String ruleCode) {
        return Result.success(allocationService.getStrategyByRuleCode(ruleCode));
    }

    @Operation(summary = "匹配分配规则")
    @GetMapping("/rule/match")
    public Result<AllocationRule> matchRule(
            @RequestParam String warehouseCode,
            @RequestParam(required = false) String ownerCode,
            @RequestParam(required = false) String categoryCode,
            @RequestParam(required = false) String skuCode,
            @RequestParam(required = false) String orderType) {
        return Result.success(
                allocationService.matchRule(
                        warehouseCode, ownerCode, categoryCode, skuCode, orderType));
    }

    @Operation(summary = "分页查询分配规则")
    @GetMapping("/rule/list")
    public Result<Page<AllocationRule>> pageRules(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String warehouseCode) {
        return Result.success(allocationService.pageRules(new Page<>(page, size), warehouseCode));
    }

    // ============================================================

    // 分配执行
    // ============================================================

    @Operation(summary = "执行库存分配")
    @PostMapping("/execute")
    public Result<String> allocate(
            @RequestParam String orderNo,
            @RequestParam String warehouseCode,
            @RequestParam(required = false) String ownerCode,
            @RequestParam String operator,
            @RequestBody List<Map<String, Object>> items) {
        return Result.success(
                allocationService.allocate(orderNo, warehouseCode, ownerCode, items, operator));
    }

    @Operation(summary = "重新分配")
    @PostMapping("/reallocate")
    public Result<String> reallocate(
            @RequestParam String orderNo,
            @RequestParam String warehouseCode,
            @RequestParam(required = false) String ownerCode,
            @RequestParam String operator,
            @RequestParam String reason,
            @RequestBody List<Map<String, Object>> items) {
        return Result.success(
                allocationService.reallocate(
                        orderNo, warehouseCode, ownerCode, items, operator, reason));
    }

    // ============================================================

    // 释放分配
    // ============================================================

    @Operation(summary = "释放分配")
    @PostMapping("/release/{allocationNo}")
    public Result<Integer> releaseAllocation(
            @PathVariable String allocationNo,
            @RequestParam String reason,
            @RequestParam String operator) {
        return Result.success(allocationService.releaseAllocation(allocationNo, reason, operator));
    }

    @Operation(summary = "按订单释放分配")
    @PostMapping("/release/order/{orderNo}")
    public Result<Integer> releaseByOrderNo(
            @PathVariable String orderNo,
            @RequestParam String reason,
            @RequestParam String operator) {
        return Result.success(allocationService.releaseByOrderNo(orderNo, reason, operator));
    }

    @Operation(summary = "超时自动释放")
    @PostMapping("/auto-release")
    public Result<Integer> autoReleaseTimeout() {
        return Result.success(allocationService.autoReleaseTimeout());
    }

    // ============================================================

    // 拣货确认
    // ============================================================

    @Operation(summary = "拣货确认")
    @PostMapping("/pick/{detailId}")
    public Result<AllocationDetail> confirmPick(
            @PathVariable Long detailId,
            @RequestParam BigDecimal pickedQty,
            @RequestParam String operator) {
        return Result.success(allocationService.confirmPick(detailId, pickedQty, operator));
    }

    // ============================================================

    // 查询
    // ============================================================

    @Operation(summary = "按分配单号查询明细")
    @GetMapping("/detail/{allocationNo}")
    public Result<List<AllocationDetail>> getDetailsByAllocationNo(
            @PathVariable String allocationNo) {
        return Result.success(allocationService.getDetailsByAllocationNo(allocationNo));
    }

    @Operation(summary = "按订单号查询明细")
    @GetMapping("/detail/order/{orderNo}")
    public Result<List<AllocationDetail>> getDetailsByOrderNo(@PathVariable String orderNo) {
        return Result.success(allocationService.getDetailsByOrderNo(orderNo));
    }

    @Operation(summary = "按SKU查询活跃分配")
    @GetMapping("/detail/sku/{skuCode}")
    public Result<List<AllocationDetail>> getActiveBySku(@PathVariable String skuCode) {
        return Result.success(allocationService.getActiveBySku(skuCode));
    }

    @Operation(summary = "按库位查询活跃分配")
    @GetMapping("/detail/location/{locationCode}")
    public Result<List<AllocationDetail>> getActiveByLocation(@PathVariable String locationCode) {
        return Result.success(allocationService.getActiveByLocation(locationCode));
    }

    @Operation(summary = "分页查询分配明细")
    @GetMapping("/detail/list")
    public Result<Page<AllocationDetail>> pageDetails(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String warehouseCode,
            @RequestParam(required = false) String skuCode,
            @RequestParam(required = false) String status) {
        return Result.success(
                allocationService.pageDetails(
                        new Page<>(page, size), warehouseCode, skuCode, status));
    }

    @Operation(summary = "查询分配日志")
    @GetMapping("/log/{allocationNo}")
    public Result<List<AllocationLog>> getLogsByAllocationNo(@PathVariable String allocationNo) {
        return Result.success(allocationService.getLogsByAllocationNo(allocationNo));
    }

    @Operation(summary = "按订单查询分配日志")
    @GetMapping("/log/order/{orderNo}")
    public Result<List<AllocationLog>> getLogsByOrderNo(@PathVariable String orderNo) {
        return Result.success(allocationService.getLogsByOrderNo(orderNo));
    }
}
