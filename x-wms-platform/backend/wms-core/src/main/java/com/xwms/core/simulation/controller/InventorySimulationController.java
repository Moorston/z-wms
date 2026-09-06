package com.xwms.core.simulation.controller;

import java.math.BigDecimal;

import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.common.core.Result;
import com.xwms.core.simulation.entity.*;
import com.xwms.core.simulation.service.InventorySimulationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "库存模拟仿真管理", description = "库存仿真/场景模拟/压力测试/方案评估")
@RestController
@RequestMapping("/api/simulation")
@RequiredArgsConstructor
public class InventorySimulationController {

    private final InventorySimulationService simulationService;

    // ==================== 库存仿真 ====================

    @Operation(summary = "创建库存仿真")
    @PostMapping("/simulation")
    public Result<InventorySimulation> createSimulation(
            @RequestParam String simulationName,
            @RequestParam String warehouseCode,
            @RequestParam(required = false) String ownerCode,
            @RequestParam String simulationType,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String initialState,
            @RequestParam(required = false) String simulationConfig,
            @RequestParam(required = false) String simulationParams,
            @RequestParam String operator) {
        return Result.success(
                simulationService.createSimulation(
                        simulationName,
                        warehouseCode,
                        ownerCode,
                        simulationType,
                        description,
                        initialState,
                        simulationConfig,
                        simulationParams,
                        operator));
    }

    @Operation(summary = "启动库存仿真")
    @PostMapping("/simulation/{simulationId}/start")
    public Result<InventorySimulation> startSimulation(
            @PathVariable String simulationId, @RequestParam String operator) {
        return Result.success(simulationService.startSimulation(simulationId, operator));
    }

    @Operation(summary = "更新仿真进度")
    @PostMapping("/simulation/{simulationId}/progress")
    public Result<InventorySimulation> updateSimulationProgress(
            @PathVariable String simulationId,
            @RequestParam BigDecimal progress,
            @RequestParam(required = false) String simulationResult,
            @RequestParam(required = false) String simulationMetrics) {
        return Result.success(
                simulationService.updateSimulationProgress(
                        simulationId, progress, simulationResult, simulationMetrics));
    }

    @Operation(summary = "完成库存仿真")
    @PostMapping("/simulation/{simulationId}/complete")
    public Result<InventorySimulation> completeSimulation(
            @PathVariable String simulationId,
            @RequestParam(required = false) String simulationResult,
            @RequestParam(required = false) String simulationMetrics,
            @RequestParam String operator) {
        return Result.success(
                simulationService.completeSimulation(
                        simulationId, simulationResult, simulationMetrics, operator));
    }

    @Operation(summary = "仿真失败")
    @PostMapping("/simulation/{simulationId}/fail")
    public Result<InventorySimulation> failSimulation(
            @PathVariable String simulationId,
            @RequestParam String errorMessage,
            @RequestParam String operator) {
        return Result.success(
                simulationService.failSimulation(simulationId, errorMessage, operator));
    }

    @Operation(summary = "分页查询库存仿真")
    @GetMapping("/simulation/list")
    public Result<Page<InventorySimulation>> pageSimulation(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String warehouseCode,
            @RequestParam(required = false) String simulationType,
            @RequestParam(required = false) String status) {
        return Result.success(
                simulationService.pageSimulation(
                        new Page<>(page, size), warehouseCode, simulationType, status));
    }

    // ==================== 场景模拟 ====================

    @Operation(summary = "创建场景模拟")
    @PostMapping("/scenario")
    public Result<ScenarioSimulation> createScenario(
            @RequestParam String scenarioName,
            @RequestParam String warehouseCode,
            @RequestParam(required = false) String ownerCode,
            @RequestParam String scenarioType,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String scenarioConfig,
            @RequestParam(required = false) String scenarioEvents,
            @RequestParam(required = false) String initialInventory,
            @RequestParam(required = false) String expectedResult,
            @RequestParam String operator) {
        return Result.success(
                simulationService.createScenario(
                        scenarioName,
                        warehouseCode,
                        ownerCode,
                        scenarioType,
                        description,
                        scenarioConfig,
                        scenarioEvents,
                        initialInventory,
                        expectedResult,
                        operator));
    }

    @Operation(summary = "执行场景模拟")
    @PostMapping("/scenario/{scenarioId}/run")
    public Result<ScenarioSimulation> runScenario(
            @PathVariable String scenarioId,
            @RequestParam(required = false) String actualResult,
            @RequestParam(required = false) String deviationAnalysis,
            @RequestParam String operator) {
        return Result.success(
                simulationService.runScenario(
                        scenarioId, actualResult, deviationAnalysis, operator));
    }

    @Operation(summary = "分页查询场景模拟")
    @GetMapping("/scenario/list")
    public Result<Page<ScenarioSimulation>> pageScenario(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String warehouseCode,
            @RequestParam(required = false) String scenarioType,
            @RequestParam(required = false) String status) {
        return Result.success(
                simulationService.pageScenario(
                        new Page<>(page, size), warehouseCode, scenarioType, status));
    }

    // ==================== 压力测试 ====================

    @Operation(summary = "创建压力测试")
    @PostMapping("/stress")
    public Result<StressTest> createStressTest(
            @RequestParam String testName,
            @RequestParam String warehouseCode,
            @RequestParam(required = false) String ownerCode,
            @RequestParam String testType,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String testConfig,
            @RequestParam(required = false) Integer concurrency,
            @RequestParam(required = false) Long totalRequests,
            @RequestParam String operator) {
        return Result.success(
                simulationService.createStressTest(
                        testName,
                        warehouseCode,
                        ownerCode,
                        testType,
                        description,
                        testConfig,
                        concurrency,
                        totalRequests,
                        operator));
    }

    @Operation(summary = "执行压力测试")
    @PostMapping("/stress/{testId}/run")
    public Result<StressTest> runStressTest(
            @PathVariable String testId,
            @RequestParam Long successCount,
            @RequestParam Long failureCount,
            @RequestParam BigDecimal avgResponseTime,
            @RequestParam BigDecimal maxResponseTime,
            @RequestParam BigDecimal minResponseTime,
            @RequestParam BigDecimal p50ResponseTime,
            @RequestParam BigDecimal p95ResponseTime,
            @RequestParam BigDecimal p99ResponseTime,
            @RequestParam BigDecimal throughput,
            @RequestParam(required = false) String testResult,
            @RequestParam String operator) {
        return Result.success(
                simulationService.runStressTest(
                        testId,
                        successCount,
                        failureCount,
                        avgResponseTime,
                        maxResponseTime,
                        minResponseTime,
                        p50ResponseTime,
                        p95ResponseTime,
                        p99ResponseTime,
                        throughput,
                        testResult,
                        operator));
    }

    @Operation(summary = "分页查询压力测试")
    @GetMapping("/stress/list")
    public Result<Page<StressTest>> pageStressTest(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String warehouseCode,
            @RequestParam(required = false) String testType,
            @RequestParam(required = false) String status) {
        return Result.success(
                simulationService.pageStressTest(
                        new Page<>(page, size), warehouseCode, testType, status));
    }

    // ==================== 方案评估 ====================

    @Operation(summary = "创建方案评估")
    @PostMapping("/evaluation")
    public Result<SchemeEvaluation> createEvaluation(
            @RequestParam String evaluationName,
            @RequestParam String warehouseCode,
            @RequestParam(required = false) String ownerCode,
            @RequestParam String schemeType,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String schemeAConfig,
            @RequestParam(required = false) String schemeBConfig,
            @RequestParam String operator) {
        return Result.success(
                simulationService.createEvaluation(
                        evaluationName,
                        warehouseCode,
                        ownerCode,
                        schemeType,
                        description,
                        schemeAConfig,
                        schemeBConfig,
                        operator));
    }

    @Operation(summary = "执行方案评估")
    @PostMapping("/evaluation/{evaluationId}/run")
    public Result<SchemeEvaluation> runEvaluation(
            @PathVariable String evaluationId,
            @RequestParam(required = false) String schemeAResult,
            @RequestParam(required = false) String schemeBResult,
            @RequestParam(required = false) String comparisonResult,
            @RequestParam(required = false) String evaluationMetrics,
            @RequestParam(required = false) String recommendation,
            @RequestParam(required = false) String recommendationReason,
            @RequestParam String operator) {
        return Result.success(
                simulationService.runEvaluation(
                        evaluationId,
                        schemeAResult,
                        schemeBResult,
                        comparisonResult,
                        evaluationMetrics,
                        recommendation,
                        recommendationReason,
                        operator));
    }

    @Operation(summary = "审批方案评估")
    @PostMapping("/evaluation/{evaluationId}/approve")
    public Result<SchemeEvaluation> approveEvaluation(
            @PathVariable String evaluationId,
            @RequestParam String approver,
            @RequestParam boolean approved) {
        return Result.success(
                simulationService.approveEvaluation(evaluationId, approver, approved));
    }

    @Operation(summary = "分页查询方案评估")
    @GetMapping("/evaluation/list")
    public Result<Page<SchemeEvaluation>> pageEvaluation(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String warehouseCode,
            @RequestParam(required = false) String schemeType,
            @RequestParam(required = false) String status) {
        return Result.success(
                simulationService.pageEvaluation(
                        new Page<>(page, size), warehouseCode, schemeType, status));
    }
}
