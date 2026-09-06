package com.xwms.core.abc.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.common.core.Result;
import com.xwms.core.abc.entity.*;
import com.xwms.core.abc.service.AbcService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/** 库存ABC分类管理 Controller */
@Tag(name = "库存ABC分类管理", description = "ABC分类规则/ABC分类计算/XYZ分类/分类调整")
@RestController
@RequestMapping("/api/abc")
@RequiredArgsConstructor
public class AbcController {

    private final AbcService abcService;

    // ============================================================

    // ABC分类规则
    // ============================================================

    @Operation(summary = "创建ABC分类规则")
    @PostMapping("/rule")
    public Result<AbcRule> createRule(@RequestBody AbcRule rule) {
        return Result.success(abcService.createRule(rule));
    }

    @Operation(summary = "分页查询ABC分类规则")
    @GetMapping("/rule")
    public Result<Page<AbcRule>> pageRules(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String warehouseCode) {
        return Result.success(abcService.pageRules(new Page<>(page, size), warehouseCode));
    }

    @Operation(summary = "按编码查询ABC分类规则")
    @GetMapping("/rule/{ruleCode}")
    public Result<AbcRule> getRuleByCode(@PathVariable String ruleCode) {
        return Result.success(abcService.getRuleByCode(ruleCode));
    }

    @Operation(summary = "匹配ABC分类规则")
    @GetMapping("/rule/match")
    public Result<AbcRule> matchRule(
            @RequestParam String warehouseCode,
            @RequestParam(required = false) String ownerCode,
            @RequestParam(required = false) String categoryCode) {
        return Result.success(abcService.matchRule(warehouseCode, ownerCode, categoryCode));
    }

    // ============================================================

    // ABC分类计算
    // ============================================================

    @Operation(summary = "执行ABC分类计算")
    @PostMapping("/calculate")
    public Result<List<AbcClassification>> calculateAbcClassification(
            @RequestParam String warehouseCode,
            @RequestParam(required = false) String ownerCode,
            @RequestParam(required = false) String ruleCode,
            @RequestBody List<Map<String, Object>> skuData,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
                    LocalDateTime periodStart,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
                    LocalDateTime periodEnd) {
        return Result.success(
                abcService.calculateAbcClassification(
                        warehouseCode, ownerCode, ruleCode, skuData, periodStart, periodEnd));
    }

    @Operation(summary = "执行XYZ分类计算")
    @PostMapping("/xyz/calculate")
    public Result<List<XyzClassification>> calculateXyzClassification(
            @RequestParam String warehouseCode,
            @RequestBody List<Map<String, Object>> demandData,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
                    LocalDateTime periodStart,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
                    LocalDateTime periodEnd) {
        return Result.success(
                abcService.calculateXyzClassification(
                        warehouseCode, demandData, periodStart, periodEnd));
    }

    // ============================================================

    // 分类调整
    // ============================================================

    @Operation(summary = "手动调整ABC分类")
    @PostMapping("/adjust")
    public Result<AbcClassification> adjustAbcClass(
            @RequestParam String skuCode,
            @RequestParam String newAbcClass,
            @RequestParam String reason,
            @RequestParam(required = false) String operator) {
        return Result.success(abcService.adjustAbcClass(skuCode, newAbcClass, reason, operator));
    }

    // ============================================================

    // 查询
    // ============================================================

    @Operation(summary = "查询当前ABC分类")
    @GetMapping("/current/{warehouseCode}")
    public Result<List<AbcClassification>> getCurrentClassification(
            @PathVariable String warehouseCode) {
        return Result.success(abcService.getCurrentClassification(warehouseCode));
    }

    @Operation(summary = "按SKU查询当前分类")
    @GetMapping("/sku/{skuCode}")
    public Result<AbcClassification> getCurrentBySku(@PathVariable String skuCode) {
        return Result.success(abcService.getCurrentBySku(skuCode));
    }

    @Operation(summary = "按ABC分类查询")
    @GetMapping("/class/{warehouseCode}/{abcClass}")
    public Result<List<AbcClassification>> getByClass(
            @PathVariable String warehouseCode, @PathVariable String abcClass) {
        return Result.success(abcService.getByClass(warehouseCode, abcClass));
    }

    @Operation(summary = "按SKU查询XYZ分类")
    @GetMapping("/xyz/sku/{skuCode}")
    public Result<XyzClassification> getXyzBySku(@PathVariable String skuCode) {
        return Result.success(abcService.getXyzBySku(skuCode));
    }

    @Operation(summary = "查询仓库XYZ分类")
    @GetMapping("/xyz/{warehouseCode}")
    public Result<List<XyzClassification>> getXyzByWarehouse(@PathVariable String warehouseCode) {
        return Result.success(abcService.getXyzByWarehouse(warehouseCode));
    }

    @Operation(summary = "查询AX九象限矩阵")
    @GetMapping("/matrix/{warehouseCode}")
    public Result<Map<String, List<String>>> getAxMatrix(@PathVariable String warehouseCode) {
        return Result.success(abcService.getAxMatrix(warehouseCode));
    }

    @Operation(summary = "按SKU查询分类历史")
    @GetMapping("/history/sku/{skuCode}")
    public Result<List<AbcHistory>> getHistoryBySku(@PathVariable String skuCode) {
        return Result.success(abcService.getHistoryBySku(skuCode));
    }

    @Operation(summary = "按仓库查询分类历史")
    @GetMapping("/history/{warehouseCode}")
    public Result<List<AbcHistory>> getHistoryByWarehouse(@PathVariable String warehouseCode) {
        return Result.success(abcService.getHistoryByWarehouse(warehouseCode));
    }
}
