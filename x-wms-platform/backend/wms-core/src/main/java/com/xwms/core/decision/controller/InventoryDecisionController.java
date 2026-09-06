package com.xwms.core.decision.controller;

import java.math.BigDecimal;

import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.common.core.Result;
import com.xwms.core.decision.entity.*;
import com.xwms.core.decision.service.InventoryDecisionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "库存决策支持管理", description = "库存决策/库存优化/库存策略/库存诊断")
@RestController
@RequestMapping("/api/decision")
@RequiredArgsConstructor
public class InventoryDecisionController {

    private final InventoryDecisionService decisionService;

    // ==================== 库存决策 ====================

    @Operation(summary = "创建库存决策")
    @PostMapping("/decision")
    public Result<InventoryDecision> createDecision(
            @RequestParam String warehouseCode,
            @RequestParam(required = false) String ownerCode,
            @RequestParam(required = false) String skuCode,
            @RequestParam(required = false) String skuName,
            @RequestParam(required = false) String categoryCode,
            @RequestParam String decisionType,
            @RequestParam(required = false) String decisionName,
            @RequestParam(required = false) String decisionContent,
            @RequestParam(required = false) String decisionReason,
            @RequestParam(required = false) String decisionBasis,
            @RequestParam(required = false) String expectedImpact,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) BigDecimal confidence,
            @RequestParam String operator) {
        return Result.success(
                decisionService.createDecision(
                        warehouseCode,
                        ownerCode,
                        skuCode,
                        skuName,
                        categoryCode,
                        decisionType,
                        decisionName,
                        decisionContent,
                        decisionReason,
                        decisionBasis,
                        expectedImpact,
                        priority,
                        confidence,
                        operator));
    }

    @Operation(summary = "提交决策审核")
    @PostMapping("/decision/{decisionId}/submit")
    public Result<InventoryDecision> submitDecision(
            @PathVariable String decisionId, @RequestParam String operator) {
        return Result.success(decisionService.submitDecision(decisionId, operator));
    }

    @Operation(summary = "审批决策")
    @PostMapping("/decision/{decisionId}/approve")
    public Result<InventoryDecision> approveDecision(
            @PathVariable String decisionId,
            @RequestParam String approver,
            @RequestParam(required = false) String comment,
            @RequestParam boolean approved) {
        return Result.success(
                decisionService.approveDecision(decisionId, approver, comment, approved));
    }

    @Operation(summary = "执行决策")
    @PostMapping("/decision/{decisionId}/execute")
    public Result<InventoryDecision> executeDecision(
            @PathVariable String decisionId,
            @RequestParam(required = false) String actualImpact,
            @RequestParam String operator) {
        return Result.success(decisionService.executeDecision(decisionId, actualImpact, operator));
    }

    @Operation(summary = "分页查询库存决策")
    @GetMapping("/decision/list")
    public Result<Page<InventoryDecision>> pageDecision(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String warehouseCode,
            @RequestParam(required = false) String decisionType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String skuCode) {
        return Result.success(
                decisionService.pageDecision(
                        new Page<>(page, size), warehouseCode, decisionType, status, skuCode));
    }

    // ==================== 库存优化 ====================

    @Operation(summary = "创建库存优化")
    @PostMapping("/optimization")
    public Result<InventoryOptimization> createOptimization(
            @RequestParam String warehouseCode,
            @RequestParam(required = false) String ownerCode,
            @RequestParam String optimizationType,
            @RequestParam(required = false) String optimizationName,
            @RequestParam(required = false) String currentState,
            @RequestParam(required = false) String targetState,
            @RequestParam(required = false) String optimizationPlan,
            @RequestParam(required = false) String expectedBenefit,
            @RequestParam(required = false) String implementationPlan,
            @RequestParam(required = false) String priority,
            @RequestParam String operator) {
        return Result.success(
                decisionService.createOptimization(
                        warehouseCode,
                        ownerCode,
                        optimizationType,
                        optimizationName,
                        currentState,
                        targetState,
                        optimizationPlan,
                        expectedBenefit,
                        implementationPlan,
                        priority,
                        operator));
    }

    @Operation(summary = "开始实施优化")
    @PostMapping("/optimization/{optimizationId}/start")
    public Result<InventoryOptimization> startImplementation(
            @PathVariable String optimizationId, @RequestParam String operator) {
        return Result.success(decisionService.startImplementation(optimizationId, operator));
    }

    @Operation(summary = "完成优化实施")
    @PostMapping("/optimization/{optimizationId}/complete")
    public Result<InventoryOptimization> completeImplementation(
            @PathVariable String optimizationId,
            @RequestParam(required = false) String actualBenefit,
            @RequestParam String operator) {
        return Result.success(
                decisionService.completeImplementation(optimizationId, actualBenefit, operator));
    }

    @Operation(summary = "分页查询库存优化")
    @GetMapping("/optimization/list")
    public Result<Page<InventoryOptimization>> pageOptimization(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String warehouseCode,
            @RequestParam(required = false) String optimizationType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String implementationStatus) {
        return Result.success(
                decisionService.pageOptimization(
                        new Page<>(page, size),
                        warehouseCode,
                        optimizationType,
                        status,
                        implementationStatus));
    }

    // ==================== 库存策略 ====================

    @Operation(summary = "创建库存策略")
    @PostMapping("/strategy")
    public Result<InventoryStrategy> createStrategy(
            @RequestParam String strategyCode,
            @RequestParam String strategyName,
            @RequestParam String strategyType,
            @RequestParam(required = false) String warehouseCode,
            @RequestParam(required = false) String ownerCode,
            @RequestParam(required = false) String categoryCode,
            @RequestParam(required = false) String skuCode,
            @RequestParam(required = false) String strategyConfig,
            @RequestParam(required = false) String strategyRules,
            @RequestParam(required = false) Integer priority,
            @RequestParam(required = false) String description,
            @RequestParam String createdBy) {
        return Result.success(
                decisionService.createStrategy(
                        strategyCode,
                        strategyName,
                        strategyType,
                        warehouseCode,
                        ownerCode,
                        categoryCode,
                        skuCode,
                        strategyConfig,
                        strategyRules,
                        priority,
                        description,
                        createdBy));
    }

    @Operation(summary = "匹配策略")
    @GetMapping("/strategy/match")
    public Result<InventoryStrategy> matchStrategy(
            @RequestParam String strategyType,
            @RequestParam String warehouseCode,
            @RequestParam(required = false) String ownerCode,
            @RequestParam(required = false) String categoryCode,
            @RequestParam(required = false) String skuCode) {
        return Result.success(
                decisionService.matchStrategy(
                        strategyType, warehouseCode, ownerCode, categoryCode, skuCode));
    }

    @Operation(summary = "切换策略状态")
    @PostMapping("/strategy/{strategyId}/activate")
    public Result<InventoryStrategy> activateStrategy(
            @PathVariable String strategyId,
            @RequestParam boolean active,
            @RequestParam String operator) {
        return Result.success(decisionService.activateStrategy(strategyId, active, operator));
    }

    @Operation(summary = "分页查询库存策略")
    @GetMapping("/strategy/list")
    public Result<Page<InventoryStrategy>> pageStrategy(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String strategyType,
            @RequestParam(required = false) String warehouseCode,
            @RequestParam(required = false) String isActive) {
        return Result.success(
                decisionService.pageStrategy(
                        new Page<>(page, size), strategyType, warehouseCode, isActive));
    }

    // ==================== 库存诊断 ====================

    @Operation(summary = "创建库存诊断")
    @PostMapping("/diagnosis")
    public Result<InventoryDiagnosis> createDiagnosis(
            @RequestParam String warehouseCode,
            @RequestParam(required = false) String ownerCode,
            @RequestParam String diagnosisType,
            @RequestParam(required = false) String diagnosisName,
            @RequestParam(required = false) String diagnosisScope,
            @RequestParam(required = false) String scopeCode,
            @RequestParam(required = false) BigDecimal currentValue,
            @RequestParam(required = false) BigDecimal benchmarkValue,
            @RequestParam(required = false) BigDecimal targetValue,
            @RequestParam(required = false) String rootCause,
            @RequestParam(required = false) String diagnosisResult,
            @RequestParam(required = false) String recommendations,
            @RequestParam(required = false) String actionPlan,
            @RequestParam(required = false) String priority,
            @RequestParam String operator) {
        return Result.success(
                decisionService.createDiagnosis(
                        warehouseCode,
                        ownerCode,
                        diagnosisType,
                        diagnosisName,
                        diagnosisScope,
                        scopeCode,
                        currentValue,
                        benchmarkValue,
                        targetValue,
                        rootCause,
                        diagnosisResult,
                        recommendations,
                        actionPlan,
                        priority,
                        operator));
    }

    @Operation(summary = "解决诊断")
    @PostMapping("/diagnosis/{diagnosisId}/resolve")
    public Result<InventoryDiagnosis> resolveDiagnosis(
            @PathVariable String diagnosisId,
            @RequestParam String handler,
            @RequestParam(required = false) String handleResult) {
        return Result.success(decisionService.resolveDiagnosis(diagnosisId, handler, handleResult));
    }

    @Operation(summary = "忽略诊断")
    @PostMapping("/diagnosis/{diagnosisId}/ignore")
    public Result<InventoryDiagnosis> ignoreDiagnosis(
            @PathVariable String diagnosisId,
            @RequestParam String handler,
            @RequestParam(required = false) String reason) {
        return Result.success(decisionService.ignoreDiagnosis(diagnosisId, handler, reason));
    }

    @Operation(summary = "分页查询库存诊断")
    @GetMapping("/diagnosis/list")
    public Result<Page<InventoryDiagnosis>> pageDiagnosis(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String warehouseCode,
            @RequestParam(required = false) String diagnosisType,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String status) {
        return Result.success(
                decisionService.pageDiagnosis(
                        new Page<>(page, size), warehouseCode, diagnosisType, severity, status));
    }
}
