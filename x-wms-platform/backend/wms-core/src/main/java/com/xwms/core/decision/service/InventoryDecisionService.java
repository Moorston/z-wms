package com.xwms.core.decision.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.core.decision.entity.*;
import com.xwms.core.decision.mapper.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryDecisionService {

    private final InventoryDecisionMapper decisionMapper;
    private final InventoryOptimizationMapper optimizationMapper;
    private final InventoryStrategyMapper strategyMapper;
    private final InventoryDiagnosisMapper diagnosisMapper;

    private static final AtomicInteger SEQ = new AtomicInteger(0);
    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    // ==================== 库存决策 ====================

    @Transactional(rollbackFor = Exception.class)
    public InventoryDecision createDecision(
            String warehouseCode,
            String ownerCode,
            String skuCode,
            String skuName,
            String categoryCode,
            String decisionType,
            String decisionName,
            String decisionContent,
            String decisionReason,
            String decisionBasis,
            String expectedImpact,
            String priority,
            BigDecimal confidence,
            String operator) {
        InventoryDecision decision = new InventoryDecision();
        decision.setDecisionId(generateId("DEC"));
        decision.setWarehouseCode(warehouseCode);
        decision.setOwnerCode(ownerCode);
        decision.setSkuCode(skuCode);
        decision.setSkuName(skuName);
        decision.setCategoryCode(categoryCode);
        decision.setDecisionType(decisionType);
        decision.setDecisionName(decisionName);
        decision.setDecisionContent(decisionContent);
        decision.setDecisionReason(decisionReason);
        decision.setDecisionBasis(decisionBasis);
        decision.setExpectedImpact(expectedImpact);
        decision.setPriority(priority != null ? priority : "NORMAL");
        decision.setConfidence(confidence);
        decision.setStatus("DRAFT");
        decision.setOperator(operator);
        decision.setDecisionTime(LocalDateTime.now());
        decisionMapper.insert(decision);
        log.info(
                "创建库存决策: id={}, type={}, name={}",
                decision.getDecisionId(),
                decisionType,
                decisionName);
        return decision;
    }

    @Transactional(rollbackFor = Exception.class)
    public InventoryDecision submitDecision(String decisionId, String operator) {
        InventoryDecision decision = decisionMapper.selectByDecisionId(decisionId);
        if (decision == null) throw new RuntimeException("决策不存在: " + decisionId);
        decision.setStatus("REVIEW");
        decision.setOperator(operator);
        decision.setOperateTime(LocalDateTime.now());
        decisionMapper.updateById(decision);
        log.info("提交决策审核: id={}", decisionId);
        return decisionMapper.selectByDecisionId(decisionId);
    }

    @Transactional(rollbackFor = Exception.class)
    public InventoryDecision approveDecision(
            String decisionId, String approver, String comment, boolean approved) {
        InventoryDecision decision = decisionMapper.selectByDecisionId(decisionId);
        if (decision == null) throw new RuntimeException("决策不存在: " + decisionId);
        decision.setStatus(approved ? "APPROVED" : "REJECTED");
        decision.setApprover(approver);
        decision.setApproveTime(LocalDateTime.now());
        decision.setApproveComment(comment);
        decisionMapper.updateById(decision);
        log.info("审批决策: id={}, approved={}", decisionId, approved);
        return decisionMapper.selectByDecisionId(decisionId);
    }

    @Transactional(rollbackFor = Exception.class)
    public InventoryDecision executeDecision(
            String decisionId, String actualImpact, String operator) {
        InventoryDecision decision = decisionMapper.selectByDecisionId(decisionId);
        if (decision == null) throw new RuntimeException("决策不存在: " + decisionId);
        if (!"APPROVED".equals(decision.getStatus()))
            throw new RuntimeException("决策状态不是已批准: " + decision.getStatus());
        decision.setStatus("EXECUTED");
        decision.setActualImpact(actualImpact);
        decision.setOperator(operator);
        decision.setOperateTime(LocalDateTime.now());
        decisionMapper.updateById(decision);
        log.info("执行决策: id={}", decisionId);
        return decisionMapper.selectByDecisionId(decisionId);
    }

    // ==================== 库存优化 ====================

    @Transactional(rollbackFor = Exception.class)
    public InventoryOptimization createOptimization(
            String warehouseCode,
            String ownerCode,
            String optimizationType,
            String optimizationName,
            String currentState,
            String targetState,
            String optimizationPlan,
            String expectedBenefit,
            String implementationPlan,
            String priority,
            String operator) {
        InventoryOptimization opt = new InventoryOptimization();
        opt.setOptimizationId(generateId("OPT"));
        opt.setWarehouseCode(warehouseCode);
        opt.setOwnerCode(ownerCode);
        opt.setOptimizationType(optimizationType);
        opt.setOptimizationName(optimizationName);
        opt.setCurrentState(currentState);
        opt.setTargetState(targetState);
        opt.setOptimizationPlan(optimizationPlan);
        opt.setExpectedBenefit(expectedBenefit);
        opt.setImplementationPlan(implementationPlan);
        opt.setImplementationStatus("PENDING");
        opt.setPriority(priority != null ? priority : "NORMAL");
        opt.setStatus("DRAFT");
        opt.setOperator(operator);
        opt.setOptimizationTime(LocalDateTime.now());
        optimizationMapper.insert(opt);
        log.info(
                "创建库存优化: id={}, type={}, name={}",
                opt.getOptimizationId(),
                optimizationType,
                optimizationName);
        return opt;
    }

    @Transactional(rollbackFor = Exception.class)
    public InventoryOptimization startImplementation(String optimizationId, String operator) {
        InventoryOptimization opt = optimizationMapper.selectByOptimizationId(optimizationId);
        if (opt == null) throw new RuntimeException("优化方案不存在: " + optimizationId);
        opt.setImplementationStatus("IN_PROGRESS");
        opt.setImplementationStart(LocalDateTime.now());
        opt.setOperator(operator);
        opt.setOperateTime(LocalDateTime.now());
        optimizationMapper.updateById(opt);
        log.info("开始实施优化: id={}", optimizationId);
        return optimizationMapper.selectByOptimizationId(optimizationId);
    }

    @Transactional(rollbackFor = Exception.class)
    public InventoryOptimization completeImplementation(
            String optimizationId, String actualBenefit, String operator) {
        InventoryOptimization opt = optimizationMapper.selectByOptimizationId(optimizationId);
        if (opt == null) throw new RuntimeException("优化方案不存在: " + optimizationId);
        opt.setImplementationStatus("COMPLETED");
        opt.setImplementationEnd(LocalDateTime.now());
        opt.setActualBenefit(actualBenefit);
        opt.setStatus("EXECUTED");
        opt.setOperator(operator);
        opt.setOperateTime(LocalDateTime.now());
        optimizationMapper.updateById(opt);
        log.info("完成优化实施: id={}", optimizationId);
        return optimizationMapper.selectByOptimizationId(optimizationId);
    }

    // ==================== 库存策略 ====================

    @Transactional(rollbackFor = Exception.class)
    public InventoryStrategy createStrategy(
            String strategyCode,
            String strategyName,
            String strategyType,
            String warehouseCode,
            String ownerCode,
            String categoryCode,
            String skuCode,
            String strategyConfig,
            String strategyRules,
            Integer priority,
            String description,
            String createdBy) {
        InventoryStrategy strategy = new InventoryStrategy();
        strategy.setStrategyId(generateId("STR"));
        strategy.setStrategyCode(strategyCode);
        strategy.setStrategyName(strategyName);
        strategy.setStrategyType(strategyType);
        strategy.setWarehouseCode(warehouseCode);
        strategy.setOwnerCode(ownerCode);
        strategy.setCategoryCode(categoryCode);
        strategy.setSkuCode(skuCode);
        strategy.setStrategyConfig(strategyConfig);
        strategy.setStrategyRules(strategyRules);
        strategy.setPriority(priority != null ? priority : 100);
        strategy.setIsActive("Y");
        strategy.setVersion(1);
        strategy.setDescription(description);
        strategy.setCreatedBy(createdBy);
        strategyMapper.insert(strategy);
        log.info(
                "创建库存策略: id={}, code={}, type={}",
                strategy.getStrategyId(),
                strategyCode,
                strategyType);
        return strategy;
    }

    /** 匹配策略（核心逻辑） 按匹配精度优先级: SKU级 > 品类级 > 货主级 > 仓库级 > 全局 */
    public InventoryStrategy matchStrategy(
            String strategyType,
            String warehouseCode,
            String ownerCode,
            String categoryCode,
            String skuCode) {
        List<InventoryStrategy> strategies = strategyMapper.selectActiveByType(strategyType);
        LocalDateTime now = LocalDateTime.now();
        for (InventoryStrategy s : strategies) {
            if (s.getEffectiveStart() != null && now.isBefore(s.getEffectiveStart())) continue;
            if (s.getEffectiveEnd() != null && now.isAfter(s.getEffectiveEnd())) continue;
            boolean match = true;
            if (s.getSkuCode() != null && !s.getSkuCode().equals(skuCode)) match = false;
            if (s.getCategoryCode() != null && !s.getCategoryCode().equals(categoryCode))
                match = false;
            if (s.getOwnerCode() != null && !s.getOwnerCode().equals(ownerCode)) match = false;
            if (s.getWarehouseCode() != null && !s.getWarehouseCode().equals(warehouseCode))
                match = false;
            if (match) return s;
        }
        return null;
    }

    @Transactional(rollbackFor = Exception.class)
    public InventoryStrategy activateStrategy(String strategyId, boolean active, String operator) {
        InventoryStrategy strategy = strategyMapper.selectByStrategyId(strategyId);
        if (strategy == null) throw new RuntimeException("策略不存在: " + strategyId);
        strategy.setIsActive(active ? "Y" : "N");
        strategy.setUpdatedBy(operator);
        strategyMapper.updateById(strategy);
        log.info("切换策略状态: id={}, active={}", strategyId, active);
        return strategyMapper.selectByStrategyId(strategyId);
    }

    // ==================== 库存诊断 ====================

    @Transactional(rollbackFor = Exception.class)
    public InventoryDiagnosis createDiagnosis(
            String warehouseCode,
            String ownerCode,
            String diagnosisType,
            String diagnosisName,
            String diagnosisScope,
            String scopeCode,
            BigDecimal currentValue,
            BigDecimal benchmarkValue,
            BigDecimal targetValue,
            String rootCause,
            String diagnosisResult,
            String recommendations,
            String actionPlan,
            String priority,
            String operator) {
        InventoryDiagnosis diagnosis = new InventoryDiagnosis();
        diagnosis.setDiagnosisId(generateId("DIA"));
        diagnosis.setWarehouseCode(warehouseCode);
        diagnosis.setOwnerCode(ownerCode);
        diagnosis.setDiagnosisType(diagnosisType);
        diagnosis.setDiagnosisName(diagnosisName);
        diagnosis.setDiagnosisScope(diagnosisScope != null ? diagnosisScope : "WAREHOUSE");
        diagnosis.setScopeCode(scopeCode);
        diagnosis.setCurrentValue(currentValue);
        diagnosis.setBenchmarkValue(benchmarkValue);
        diagnosis.setTargetValue(targetValue);
        if (currentValue != null && targetValue != null) {
            diagnosis.setDeviation(currentValue.subtract(targetValue));
            if (targetValue.compareTo(BigDecimal.ZERO) != 0) {
                diagnosis.setDeviationRate(
                        currentValue
                                .subtract(targetValue)
                                .divide(targetValue, 4, RoundingMode.HALF_UP)
                                .multiply(BigDecimal.valueOf(100)));
            }
        }
        diagnosis.setSeverity(judgeSeverity(currentValue, targetValue, benchmarkValue));
        diagnosis.setRootCause(rootCause);
        diagnosis.setDiagnosisResult(diagnosisResult);
        diagnosis.setRecommendations(recommendations);
        diagnosis.setActionPlan(actionPlan);
        diagnosis.setPriority(priority != null ? priority : "NORMAL");
        diagnosis.setStatus("PENDING");
        diagnosis.setOperator(operator);
        diagnosis.setDiagnosisTime(LocalDateTime.now());
        diagnosisMapper.insert(diagnosis);
        log.info(
                "创建库存诊断: id={}, type={}, severity={}",
                diagnosis.getDiagnosisId(),
                diagnosisType,
                diagnosis.getSeverity());
        return diagnosis;
    }

    private String judgeSeverity(
            BigDecimal currentValue, BigDecimal targetValue, BigDecimal benchmarkValue) {
        if (currentValue == null || targetValue == null) return "NORMAL";
        BigDecimal rate = currentValue.subtract(targetValue).abs();
        if (benchmarkValue != null && benchmarkValue.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal ratio = rate.divide(benchmarkValue, 4, RoundingMode.HALF_UP);
            if (ratio.compareTo(BigDecimal.valueOf(0.5)) >= 0) return "CRITICAL";
            if (ratio.compareTo(BigDecimal.valueOf(0.2)) >= 0) return "WARNING";
            if (ratio.compareTo(BigDecimal.valueOf(0.05)) <= 0) return "GOOD";
        }
        return "NORMAL";
    }

    @Transactional(rollbackFor = Exception.class)
    public InventoryDiagnosis resolveDiagnosis(
            String diagnosisId, String handler, String handleResult) {
        InventoryDiagnosis diagnosis = diagnosisMapper.selectByDiagnosisId(diagnosisId);
        if (diagnosis == null) throw new RuntimeException("诊断不存在: " + diagnosisId);
        diagnosis.setStatus("RESOLVED");
        diagnosis.setHandler(handler);
        diagnosis.setHandleTime(LocalDateTime.now());
        diagnosis.setHandleResult(handleResult);
        diagnosisMapper.updateById(diagnosis);
        log.info("解决诊断: id={}", diagnosisId);
        return diagnosisMapper.selectByDiagnosisId(diagnosisId);
    }

    @Transactional(rollbackFor = Exception.class)
    public InventoryDiagnosis ignoreDiagnosis(String diagnosisId, String handler, String reason) {
        InventoryDiagnosis diagnosis = diagnosisMapper.selectByDiagnosisId(diagnosisId);
        if (diagnosis == null) throw new RuntimeException("诊断不存在: " + diagnosisId);
        diagnosis.setStatus("IGNORED");
        diagnosis.setHandler(handler);
        diagnosis.setHandleTime(LocalDateTime.now());
        diagnosis.setHandleResult("已忽略: " + reason);
        diagnosisMapper.updateById(diagnosis);
        log.info("忽略诊断: id={}, reason={}", diagnosisId, reason);
        return diagnosisMapper.selectByDiagnosisId(diagnosisId);
    }

    // ==================== 分页查询 ====================

    public Page<InventoryDecision> pageDecision(
            Page<InventoryDecision> page,
            String warehouseCode,
            String decisionType,
            String status,
            String skuCode) {
        LambdaQueryWrapper<InventoryDecision> wrapper = new LambdaQueryWrapper<>();
        if (warehouseCode != null) wrapper.eq(InventoryDecision::getWarehouseCode, warehouseCode);
        if (decisionType != null) wrapper.eq(InventoryDecision::getDecisionType, decisionType);
        if (status != null) wrapper.eq(InventoryDecision::getStatus, status);
        if (skuCode != null) wrapper.eq(InventoryDecision::getSkuCode, skuCode);
        wrapper.orderByDesc(InventoryDecision::getDecisionTime);
        return decisionMapper.selectPage(page, wrapper);
    }

    public Page<InventoryOptimization> pageOptimization(
            Page<InventoryOptimization> page,
            String warehouseCode,
            String optimizationType,
            String status,
            String implementationStatus) {
        LambdaQueryWrapper<InventoryOptimization> wrapper = new LambdaQueryWrapper<>();
        if (warehouseCode != null)
            wrapper.eq(InventoryOptimization::getWarehouseCode, warehouseCode);
        if (optimizationType != null)
            wrapper.eq(InventoryOptimization::getOptimizationType, optimizationType);
        if (status != null) wrapper.eq(InventoryOptimization::getStatus, status);
        if (implementationStatus != null)
            wrapper.eq(InventoryOptimization::getImplementationStatus, implementationStatus);
        wrapper.orderByDesc(InventoryOptimization::getOptimizationTime);
        return optimizationMapper.selectPage(page, wrapper);
    }

    public Page<InventoryStrategy> pageStrategy(
            Page<InventoryStrategy> page,
            String strategyType,
            String warehouseCode,
            String isActive) {
        LambdaQueryWrapper<InventoryStrategy> wrapper = new LambdaQueryWrapper<>();
        if (strategyType != null) wrapper.eq(InventoryStrategy::getStrategyType, strategyType);
        if (warehouseCode != null) wrapper.eq(InventoryStrategy::getWarehouseCode, warehouseCode);
        if (isActive != null) wrapper.eq(InventoryStrategy::getIsActive, isActive);
        wrapper.orderByAsc(InventoryStrategy::getPriority);
        return strategyMapper.selectPage(page, wrapper);
    }

    public Page<InventoryDiagnosis> pageDiagnosis(
            Page<InventoryDiagnosis> page,
            String warehouseCode,
            String diagnosisType,
            String severity,
            String status) {
        LambdaQueryWrapper<InventoryDiagnosis> wrapper = new LambdaQueryWrapper<>();
        if (warehouseCode != null) wrapper.eq(InventoryDiagnosis::getWarehouseCode, warehouseCode);
        if (diagnosisType != null) wrapper.eq(InventoryDiagnosis::getDiagnosisType, diagnosisType);
        if (severity != null) wrapper.eq(InventoryDiagnosis::getSeverity, severity);
        if (status != null) wrapper.eq(InventoryDiagnosis::getStatus, status);
        wrapper.orderByDesc(InventoryDiagnosis::getDiagnosisTime);
        return diagnosisMapper.selectPage(page, wrapper);
    }

    private String generateId(String prefix) {
        return prefix
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }
}
