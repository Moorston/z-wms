package com.xwms.core.simulation.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.core.simulation.entity.*;
import com.xwms.core.simulation.mapper.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventorySimulationService {

    private final InventorySimulationMapper simulationMapper;
    private final ScenarioSimulationMapper scenarioMapper;
    private final StressTestMapper stressTestMapper;
    private final SchemeEvaluationMapper evaluationMapper;

    private static final AtomicInteger SEQ = new AtomicInteger(0);
    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    // ==================== 库存仿真 ====================

    @Transactional(rollbackFor = Exception.class)
    public InventorySimulation createSimulation(
            String simulationName,
            String warehouseCode,
            String ownerCode,
            String simulationType,
            String description,
            String initialState,
            String simulationConfig,
            String simulationParams,
            String operator) {
        InventorySimulation sim = new InventorySimulation();
        sim.setSimulationId(generateId("SIM"));
        sim.setSimulationName(simulationName);
        sim.setWarehouseCode(warehouseCode);
        sim.setOwnerCode(ownerCode);
        sim.setSimulationType(simulationType);
        sim.setDescription(description);
        sim.setInitialState(initialState);
        sim.setSimulationConfig(simulationConfig);
        sim.setSimulationParams(simulationParams);
        sim.setStatus("DRAFT");
        sim.setProgress(BigDecimal.ZERO);
        sim.setOperator(operator);
        simulationMapper.insert(sim);
        log.info(
                "创建库存仿真: id={}, name={}, type={}",
                sim.getSimulationId(),
                simulationName,
                simulationType);
        return sim;
    }

    @Transactional(rollbackFor = Exception.class)
    public InventorySimulation startSimulation(String simulationId, String operator) {
        InventorySimulation sim = simulationMapper.selectBySimulationId(simulationId);
        if (sim == null) throw new RuntimeException("仿真不存在: " + simulationId);
        sim.setStatus("RUNNING");
        sim.setStartTime(LocalDateTime.now());
        sim.setOperator(operator);
        simulationMapper.updateById(sim);
        log.info("启动库存仿真: id={}", simulationId);
        return simulationMapper.selectBySimulationId(simulationId);
    }

    @Transactional(rollbackFor = Exception.class)
    public InventorySimulation updateSimulationProgress(
            String simulationId,
            BigDecimal progress,
            String simulationResult,
            String simulationMetrics) {
        InventorySimulation sim = simulationMapper.selectBySimulationId(simulationId);
        if (sim == null) throw new RuntimeException("仿真不存在: " + simulationId);
        sim.setProgress(progress);
        if (simulationResult != null) sim.setSimulationResult(simulationResult);
        if (simulationMetrics != null) sim.setSimulationMetrics(simulationMetrics);
        simulationMapper.updateById(sim);
        log.info("更新仿真进度: id={}, progress={}%", simulationId, progress);
        return simulationMapper.selectBySimulationId(simulationId);
    }

    @Transactional(rollbackFor = Exception.class)
    public InventorySimulation completeSimulation(
            String simulationId,
            String simulationResult,
            String simulationMetrics,
            String operator) {
        InventorySimulation sim = simulationMapper.selectBySimulationId(simulationId);
        if (sim == null) throw new RuntimeException("仿真不存在: " + simulationId);
        sim.setStatus("COMPLETED");
        sim.setEndTime(LocalDateTime.now());
        sim.setProgress(BigDecimal.valueOf(100));
        sim.setSimulationResult(simulationResult);
        sim.setSimulationMetrics(simulationMetrics);
        if (sim.getStartTime() != null) {
            sim.setDurationMs(
                    java.time.Duration.between(sim.getStartTime(), sim.getEndTime()).toMillis());
        }
        sim.setOperator(operator);
        simulationMapper.updateById(sim);
        log.info("完成库存仿真: id={}, duration={}ms", simulationId, sim.getDurationMs());
        return simulationMapper.selectBySimulationId(simulationId);
    }

    @Transactional(rollbackFor = Exception.class)
    public InventorySimulation failSimulation(
            String simulationId, String errorMessage, String operator) {
        InventorySimulation sim = simulationMapper.selectBySimulationId(simulationId);
        if (sim == null) throw new RuntimeException("仿真不存在: " + simulationId);
        sim.setStatus("FAILED");
        sim.setEndTime(LocalDateTime.now());
        sim.setErrorMessage(errorMessage);
        sim.setOperator(operator);
        simulationMapper.updateById(sim);
        log.error("仿真失败: id={}, error={}", simulationId, errorMessage);
        return simulationMapper.selectBySimulationId(simulationId);
    }

    // ==================== 场景模拟 ====================

    @Transactional(rollbackFor = Exception.class)
    public ScenarioSimulation createScenario(
            String scenarioName,
            String warehouseCode,
            String ownerCode,
            String scenarioType,
            String description,
            String scenarioConfig,
            String scenarioEvents,
            String initialInventory,
            String expectedResult,
            String operator) {
        ScenarioSimulation scenario = new ScenarioSimulation();
        scenario.setScenarioId(generateId("SCN"));
        scenario.setScenarioName(scenarioName);
        scenario.setWarehouseCode(warehouseCode);
        scenario.setOwnerCode(ownerCode);
        scenario.setScenarioType(scenarioType);
        scenario.setDescription(description);
        scenario.setScenarioConfig(scenarioConfig);
        scenario.setScenarioEvents(scenarioEvents);
        scenario.setInitialInventory(initialInventory);
        scenario.setExpectedResult(expectedResult);
        scenario.setStatus("DRAFT");
        scenario.setOperator(operator);
        scenarioMapper.insert(scenario);
        log.info(
                "创建场景模拟: id={}, name={}, type={}",
                scenario.getScenarioId(),
                scenarioName,
                scenarioType);
        return scenario;
    }

    @Transactional(rollbackFor = Exception.class)
    public ScenarioSimulation runScenario(
            String scenarioId, String actualResult, String deviationAnalysis, String operator) {
        ScenarioSimulation scenario = scenarioMapper.selectByScenarioId(scenarioId);
        if (scenario == null) throw new RuntimeException("场景不存在: " + scenarioId);
        scenario.setStatus("RUNNING");
        scenario.setStartTime(LocalDateTime.now());
        scenario.setOperator(operator);
        scenarioMapper.updateById(scenario);
        // 模拟执行
        scenario.setStatus("COMPLETED");
        scenario.setEndTime(LocalDateTime.now());
        scenario.setActualResult(actualResult);
        scenario.setDeviationAnalysis(deviationAnalysis);
        if (scenario.getStartTime() != null) {
            scenario.setDurationMs(
                    java.time.Duration.between(scenario.getStartTime(), scenario.getEndTime())
                            .toMillis());
        }
        scenarioMapper.updateById(scenario);
        log.info("执行场景模拟: id={}, duration={}ms", scenarioId, scenario.getDurationMs());
        return scenarioMapper.selectByScenarioId(scenarioId);
    }

    // ==================== 压力测试 ====================

    @Transactional(rollbackFor = Exception.class)
    public StressTest createStressTest(
            String testName,
            String warehouseCode,
            String ownerCode,
            String testType,
            String description,
            String testConfig,
            Integer concurrency,
            Long totalRequests,
            String operator) {
        StressTest test = new StressTest();
        test.setTestId(generateId("STR"));
        test.setTestName(testName);
        test.setWarehouseCode(warehouseCode);
        test.setOwnerCode(ownerCode);
        test.setTestType(testType);
        test.setDescription(description);
        test.setTestConfig(testConfig);
        test.setConcurrency(concurrency);
        test.setTotalRequests(totalRequests);
        test.setStatus("DRAFT");
        test.setOperator(operator);
        stressTestMapper.insert(test);
        log.info("创建压力测试: id={}, name={}, type={}", test.getTestId(), testName, testType);
        return test;
    }

    @Transactional(rollbackFor = Exception.class)
    public StressTest runStressTest(
            String testId,
            Long successCount,
            Long failureCount,
            BigDecimal avgResponseTime,
            BigDecimal maxResponseTime,
            BigDecimal minResponseTime,
            BigDecimal p50ResponseTime,
            BigDecimal p95ResponseTime,
            BigDecimal p99ResponseTime,
            BigDecimal throughput,
            String testResult,
            String operator) {
        StressTest test = stressTestMapper.selectByTestId(testId);
        if (test == null) throw new RuntimeException("压力测试不存在: " + testId);
        test.setStatus("RUNNING");
        test.setStartTime(LocalDateTime.now());
        test.setOperator(operator);
        stressTestMapper.updateById(test);
        // 计算错误率
        BigDecimal errorRate = BigDecimal.ZERO;
        if (test.getTotalRequests() != null && test.getTotalRequests() > 0) {
            errorRate =
                    BigDecimal.valueOf(failureCount)
                            .divide(
                                    BigDecimal.valueOf(test.getTotalRequests()),
                                    4,
                                    RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100));
        }
        test.setStatus("COMPLETED");
        test.setEndTime(LocalDateTime.now());
        test.setSuccessCount(successCount);
        test.setFailureCount(failureCount);
        test.setAvgResponseTime(avgResponseTime);
        test.setMaxResponseTime(maxResponseTime);
        test.setMinResponseTime(minResponseTime);
        test.setP50ResponseTime(p50ResponseTime);
        test.setP95ResponseTime(p95ResponseTime);
        test.setP99ResponseTime(p99ResponseTime);
        test.setThroughput(throughput);
        test.setErrorRate(errorRate);
        test.setTestResult(testResult);
        if (test.getStartTime() != null) {
            test.setDurationMs(
                    java.time.Duration.between(test.getStartTime(), test.getEndTime()).toMillis());
        }
        stressTestMapper.updateById(test);
        log.info("完成压力测试: id={}, throughput={}, errorRate={}%", testId, throughput, errorRate);
        return stressTestMapper.selectByTestId(testId);
    }

    // ==================== 方案评估 ====================

    @Transactional(rollbackFor = Exception.class)
    public SchemeEvaluation createEvaluation(
            String evaluationName,
            String warehouseCode,
            String ownerCode,
            String schemeType,
            String description,
            String schemeAConfig,
            String schemeBConfig,
            String operator) {
        SchemeEvaluation eval = new SchemeEvaluation();
        eval.setEvaluationId(generateId("EVA"));
        eval.setEvaluationName(evaluationName);
        eval.setWarehouseCode(warehouseCode);
        eval.setOwnerCode(ownerCode);
        eval.setSchemeType(schemeType);
        eval.setDescription(description);
        eval.setSchemeAConfig(schemeAConfig);
        eval.setSchemeBConfig(schemeBConfig);
        eval.setStatus("DRAFT");
        eval.setOperator(operator);
        evaluationMapper.insert(eval);
        log.info(
                "创建方案评估: id={}, name={}, type={}",
                eval.getEvaluationId(),
                evaluationName,
                schemeType);
        return eval;
    }

    @Transactional(rollbackFor = Exception.class)
    public SchemeEvaluation runEvaluation(
            String evaluationId,
            String schemeAResult,
            String schemeBResult,
            String comparisonResult,
            String evaluationMetrics,
            String recommendation,
            String recommendationReason,
            String operator) {
        SchemeEvaluation eval = evaluationMapper.selectByEvaluationId(evaluationId);
        if (eval == null) throw new RuntimeException("方案评估不存在: " + evaluationId);
        eval.setStatus("RUNNING");
        eval.setStartTime(LocalDateTime.now());
        eval.setOperator(operator);
        evaluationMapper.updateById(eval);
        eval.setStatus("COMPLETED");
        eval.setEndTime(LocalDateTime.now());
        eval.setSchemeAResult(schemeAResult);
        eval.setSchemeBResult(schemeBResult);
        eval.setComparisonResult(comparisonResult);
        eval.setEvaluationMetrics(evaluationMetrics);
        eval.setRecommendation(recommendation);
        eval.setRecommendationReason(recommendationReason);
        if (eval.getStartTime() != null) {
            eval.setDurationMs(
                    java.time.Duration.between(eval.getStartTime(), eval.getEndTime()).toMillis());
        }
        evaluationMapper.updateById(eval);
        log.info("完成方案评估: id={}, recommendation={}", evaluationId, recommendation);
        return evaluationMapper.selectByEvaluationId(evaluationId);
    }

    @Transactional(rollbackFor = Exception.class)
    public SchemeEvaluation approveEvaluation(
            String evaluationId, String approver, boolean approved) {
        SchemeEvaluation eval = evaluationMapper.selectByEvaluationId(evaluationId);
        if (eval == null) throw new RuntimeException("方案评估不存在: " + evaluationId);
        eval.setStatus(approved ? "APPROVED" : "REJECTED");
        eval.setApprover(approver);
        eval.setApproveTime(LocalDateTime.now());
        evaluationMapper.updateById(eval);
        log.info("审批方案评估: id={}, approved={}", evaluationId, approved);
        return evaluationMapper.selectByEvaluationId(evaluationId);
    }

    // ==================== 分页查询 ====================

    public Page<InventorySimulation> pageSimulation(
            Page<InventorySimulation> page,
            String warehouseCode,
            String simulationType,
            String status) {
        LambdaQueryWrapper<InventorySimulation> wrapper = new LambdaQueryWrapper<>();
        if (warehouseCode != null) wrapper.eq(InventorySimulation::getWarehouseCode, warehouseCode);
        if (simulationType != null)
            wrapper.eq(InventorySimulation::getSimulationType, simulationType);
        if (status != null) wrapper.eq(InventorySimulation::getStatus, status);
        wrapper.orderByDesc(InventorySimulation::getCreatedTime);
        return simulationMapper.selectPage(page, wrapper);
    }

    public Page<ScenarioSimulation> pageScenario(
            Page<ScenarioSimulation> page,
            String warehouseCode,
            String scenarioType,
            String status) {
        LambdaQueryWrapper<ScenarioSimulation> wrapper = new LambdaQueryWrapper<>();
        if (warehouseCode != null) wrapper.eq(ScenarioSimulation::getWarehouseCode, warehouseCode);
        if (scenarioType != null) wrapper.eq(ScenarioSimulation::getScenarioType, scenarioType);
        if (status != null) wrapper.eq(ScenarioSimulation::getStatus, status);
        wrapper.orderByDesc(ScenarioSimulation::getCreatedTime);
        return scenarioMapper.selectPage(page, wrapper);
    }

    public Page<StressTest> pageStressTest(
            Page<StressTest> page, String warehouseCode, String testType, String status) {
        LambdaQueryWrapper<StressTest> wrapper = new LambdaQueryWrapper<>();
        if (warehouseCode != null) wrapper.eq(StressTest::getWarehouseCode, warehouseCode);
        if (testType != null) wrapper.eq(StressTest::getTestType, testType);
        if (status != null) wrapper.eq(StressTest::getStatus, status);
        wrapper.orderByDesc(StressTest::getCreatedTime);
        return stressTestMapper.selectPage(page, wrapper);
    }

    public Page<SchemeEvaluation> pageEvaluation(
            Page<SchemeEvaluation> page, String warehouseCode, String schemeType, String status) {
        LambdaQueryWrapper<SchemeEvaluation> wrapper = new LambdaQueryWrapper<>();
        if (warehouseCode != null) wrapper.eq(SchemeEvaluation::getWarehouseCode, warehouseCode);
        if (schemeType != null) wrapper.eq(SchemeEvaluation::getSchemeType, schemeType);
        if (status != null) wrapper.eq(SchemeEvaluation::getStatus, status);
        wrapper.orderByDesc(SchemeEvaluation::getCreatedTime);
        return evaluationMapper.selectPage(page, wrapper);
    }

    private String generateId(String prefix) {
        return prefix
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }
}
