package com.xwms.base.batch.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.base.batch.entity.*;
import com.xwms.base.batch.service.BatchAttributeService;
import com.xwms.common.core.Result;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/** 批次属性管理 Controller */
@Tag(name = "批次属性管理", description = "批次属性/属性值/追踪规则/追踪日志")
@RestController
@RequestMapping("/api/batch")
@RequiredArgsConstructor
public class BatchAttributeController {

    private final BatchAttributeService batchAttributeService;

    // ============================================================
    // 批次属性定义
    // ============================================================

    @Operation(summary = "创建批次属性")
    @PostMapping("/attribute")
    public Result<BatchAttribute> createAttribute(@RequestBody BatchAttribute attribute) {
        return Result.success(batchAttributeService.createAttribute(attribute));
    }

    @Operation(summary = "更新批次属性")
    @PutMapping("/attribute")
    public Result<BatchAttribute> updateAttribute(@RequestBody BatchAttribute attribute) {
        return Result.success(batchAttributeService.updateAttribute(attribute));
    }

    @Operation(summary = "分页查询批次属性")
    @GetMapping("/attribute")
    public Result<Page<BatchAttribute>> pageAttributes(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String attrCategory,
            @RequestParam(required = false) String attrType,
            @RequestParam(required = false) Integer enabled) {
        return Result.success(
                batchAttributeService.pageAttributes(
                        new Page<>(page, size), attrCategory, attrType, enabled));
    }

    @Operation(summary = "按分类查询属性")
    @GetMapping("/attribute/category/{category}")
    public Result<List<BatchAttribute>> getAttributesByCategory(@PathVariable String category) {
        return Result.success(batchAttributeService.getAttributesByCategory(category));
    }

    @Operation(summary = "查询所有启用属性")
    @GetMapping("/attribute/enabled")
    public Result<List<BatchAttribute>> getAllEnabledAttributes() {
        return Result.success(batchAttributeService.getAllEnabledAttributes());
    }

    // ============================================================
    // 批次属性值
    // ============================================================

    @Operation(summary = "保存批次属性值")
    @PostMapping("/value")
    public Result<Integer> saveBatchValues(
            @RequestParam String batchNo,
            @RequestParam String skuCode,
            @RequestParam(required = false) String warehouseCode,
            @RequestParam(required = false) String sourceType,
            @RequestParam(required = false) String sourceRef,
            @RequestBody Map<String, String> attrValues) {
        return Result.success(
                batchAttributeService.saveBatchValues(
                        batchNo, skuCode, warehouseCode, attrValues, sourceType, sourceRef));
    }

    @Operation(summary = "获取批次属性值(Map)")
    @GetMapping("/value/map")
    public Result<Map<String, String>> getBatchValues(
            @RequestParam String batchNo, @RequestParam String skuCode) {
        return Result.success(batchAttributeService.getBatchValues(batchNo, skuCode));
    }

    @Operation(summary = "获取批次属性值(列表)")
    @GetMapping("/value/list")
    public Result<List<BatchValue>> getBatchValueList(
            @RequestParam String batchNo, @RequestParam String skuCode) {
        return Result.success(batchAttributeService.getBatchValueList(batchNo, skuCode));
    }

    @Operation(summary = "获取批次所有属性值")
    @GetMapping("/value/{batchNo}")
    public Result<List<BatchValue>> getBatchAllValues(@PathVariable String batchNo) {
        return Result.success(batchAttributeService.getBatchAllValues(batchNo));
    }

    @Operation(summary = "校验必填属性")
    @GetMapping("/value/validate")
    public Result<List<String>> validateRequiredAttributes(
            @RequestParam String batchNo, @RequestParam String skuCode) {
        return Result.success(batchAttributeService.validateRequiredAttributes(batchNo, skuCode));
    }

    // ============================================================
    // 批次追踪规则
    // ============================================================

    @Operation(summary = "创建追踪规则")
    @PostMapping("/trace-rule")
    public Result<BatchTraceRule> createTraceRule(@RequestBody BatchTraceRule rule) {
        return Result.success(batchAttributeService.createTraceRule(rule));
    }

    @Operation(summary = "分页查询追踪规则")
    @GetMapping("/trace-rule")
    public Result<Page<BatchTraceRule>> pageTraceRules(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String ruleType,
            @RequestParam(required = false) Integer enabled) {
        return Result.success(
                batchAttributeService.pageTraceRules(new Page<>(page, size), ruleType, enabled));
    }

    @Operation(summary = "按类型查询追踪规则")
    @GetMapping("/trace-rule/type/{ruleType}")
    public Result<List<BatchTraceRule>> getTraceRulesByType(@PathVariable String ruleType) {
        return Result.success(batchAttributeService.getTraceRulesByType(ruleType));
    }

    // ============================================================
    // 批次追踪日志
    // ============================================================

    @Operation(summary = "记录追踪日志")
    @PostMapping("/trace-log")
    public Result<BatchTraceLog> recordTraceLog(
            @RequestParam String batchNo,
            @RequestParam(required = false) String skuCode,
            @RequestParam String operationType,
            @RequestParam(required = false) String operationNo,
            @RequestParam(required = false) String fromLocation,
            @RequestParam(required = false) String toLocation,
            @RequestParam(required = false) BigDecimal quantity,
            @RequestParam(required = false) String operator,
            @RequestParam(required = false) String traceData,
            @RequestParam(required = false) String traceId) {
        return Result.success(
                batchAttributeService.recordTraceLog(
                        batchNo,
                        skuCode,
                        operationType,
                        operationNo,
                        fromLocation,
                        toLocation,
                        quantity,
                        operator,
                        traceData,
                        traceId));
    }

    @Operation(summary = "分页查询追踪日志")
    @GetMapping("/trace-log")
    public Result<Page<BatchTraceLog>> pageTraceLogs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String batchNo,
            @RequestParam(required = false) String skuCode,
            @RequestParam(required = false) String operationType,
            @RequestParam(required = false) String operationNo) {
        return Result.success(
                batchAttributeService.pageTraceLogs(
                        new Page<>(page, size), batchNo, skuCode, operationType, operationNo));
    }

    @Operation(summary = "正向追踪")
    @GetMapping("/trace/forward")
    public Result<List<BatchTraceLog>> forwardTrace(
            @RequestParam String batchNo, @RequestParam(required = false) String skuCode) {
        return Result.success(batchAttributeService.forwardTrace(batchNo, skuCode));
    }

    @Operation(summary = "反向追踪")
    @GetMapping("/trace/backward")
    public Result<List<BatchTraceLog>> backwardTrace(
            @RequestParam String batchNo, @RequestParam(required = false) String skuCode) {
        return Result.success(batchAttributeService.backwardTrace(batchNo, skuCode));
    }
}
