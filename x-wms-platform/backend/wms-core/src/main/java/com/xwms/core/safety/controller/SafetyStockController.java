package com.xwms.core.safety.controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.common.core.Result;
import com.xwms.core.safety.entity.*;
import com.xwms.core.safety.service.SafetyStockService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/** 库存安全库存管理 Controller 职责边界: 只负责安全库存管理 安全库存预警 -> alert模块 */
@Tag(name = "库存安全库存管理", description = "安全库存规则/安全库存计算/补货建议")
@RestController
@RequestMapping("/api/safety")
@RequiredArgsConstructor
public class SafetyStockController {

    private final SafetyStockService safetyStockService;

    // ============================================================

    // 安全库存规则
    // ============================================================

    @Operation(summary = "创建安全库存规则")
    @PostMapping("/rule")
    public Result<SafetyRule> createRule(@RequestBody SafetyRule rule) {
        return Result.success(safetyStockService.createRule(rule));
    }

    @Operation(summary = "分页查询安全库存规则")
    @GetMapping("/rule")
    public Result<Page<SafetyRule>> pageRules(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String warehouseCode) {
        return Result.success(safetyStockService.pageRules(new Page<>(page, size), warehouseCode));
    }

    @Operation(summary = "按编码查询安全库存规则")
    @GetMapping("/rule/{ruleCode}")
    public Result<SafetyRule> getRuleByCode(@PathVariable String ruleCode) {
        return Result.success(safetyStockService.getRuleByCode(ruleCode));
    }

    // ============================================================

    // 安全库存计算
    // ============================================================

    @Operation(summary = "计算安全库存")
    @PostMapping("/calculate")
    public Result<SafetyStock> calculateSafetyStock(
            @RequestParam String warehouseCode,
            @RequestParam String ownerCode,
            @RequestParam String skuCode,
            @RequestParam(required = false) String skuName,
            @RequestParam(required = false) String categoryCode,
            @RequestParam(required = false) String abcClass,
            @RequestParam BigDecimal avgDemand,
            @RequestParam BigDecimal stdDemand,
            @RequestParam BigDecimal leadTime,
            @RequestParam BigDecimal currentStock,
            @RequestParam(required = false) String ruleCode) {
        return Result.success(
                safetyStockService.calculateSafetyStock(
                        warehouseCode,
                        ownerCode,
                        skuCode,
                        skuName,
                        categoryCode,
                        abcClass,
                        avgDemand,
                        stdDemand,
                        leadTime,
                        currentStock,
                        ruleCode));
    }

    @Operation(summary = "获取当前安全库存")
    @GetMapping("/current")
    public Result<List<SafetyStock>> getCurrentStock(@RequestParam String warehouseCode) {
        return Result.success(safetyStockService.getCurrentStock(warehouseCode));
    }

    @Operation(summary = "按SKU获取当前安全库存")
    @GetMapping("/current/sku/{skuCode}")
    public Result<SafetyStock> getCurrentBySku(@PathVariable String skuCode) {
        return Result.success(safetyStockService.getCurrentBySku(skuCode));
    }

    @Operation(summary = "按库存状态获取安全库存")
    @GetMapping("/status")
    public Result<List<SafetyStock>> getByStockStatus(
            @RequestParam String warehouseCode, @RequestParam String stockStatus) {
        return Result.success(safetyStockService.getByStockStatus(warehouseCode, stockStatus));
    }

    // ============================================================

    // 补货建议
    // ============================================================

    @Operation(summary = "分页查询补货建议")
    @GetMapping("/suggestion")
    public Result<Page<ReorderSuggestion>> pageSuggestions(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String warehouseCode,
            @RequestParam(required = false) String status) {
        return Result.success(
                safetyStockService.pageSuggestions(new Page<>(page, size), warehouseCode, status));
    }

    @Operation(summary = "按编号查询补货建议")
    @GetMapping("/suggestion/{suggestionNo}")
    public Result<ReorderSuggestion> getSuggestionByNo(@PathVariable String suggestionNo) {
        return Result.success(safetyStockService.getSuggestionByNo(suggestionNo));
    }

    @Operation(summary = "补货建议转单")
    @PostMapping("/suggestion/{suggestionNo}/convert")
    public Result<ReorderSuggestion> convertToReplenish(
            @PathVariable String suggestionNo,
            @RequestParam String replenishNo,
            @RequestParam(required = false) String operator) {
        return Result.success(
                safetyStockService.convertToReplenish(suggestionNo, replenishNo, operator));
    }

    @Operation(summary = "忽略补货建议")
    @PostMapping("/suggestion/{suggestionNo}/ignore")
    public Result<ReorderSuggestion> ignoreSuggestion(
            @PathVariable String suggestionNo, @RequestParam(required = false) String operator) {
        return Result.success(safetyStockService.ignoreSuggestion(suggestionNo, operator));
    }
}
