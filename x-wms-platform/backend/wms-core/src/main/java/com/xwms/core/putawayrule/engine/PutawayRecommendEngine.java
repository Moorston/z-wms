package com.xwms.core.putawayrule.engine;

import java.util.*;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.xwms.core.putawayrule.cache.PutawayRuleCacheService;
import com.xwms.core.putawayrule.dto.*;
import com.xwms.core.putawayrule.entity.PutawayRule;
import com.xwms.core.putawayrule.entity.PutawayRuleLine;
import com.xwms.core.putawayrule.scorer.LocationScorer;
import com.xwms.core.putawayrule.service.LocationQueryService;
import com.xwms.core.putawayrule.service.PutawayRuleService;
import com.xwms.core.putawayrule.strategy.RuleCodeStrategy;
import com.xwms.core.putawayrule.strategy.RuleCodeStrategyFactory;
import com.xwms.core.putawayrule.validator.ExtendedConstraintValidator;
import com.xwms.core.putawayrule.validator.LocationLimitValidator;
import com.xwms.core.putawayrule.validator.LocationValidationResult;
import com.xwms.core.putawayrule.validator.SpaceLimitValidator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 上架库位推荐引擎（核心） 5步流程：规则解析 → 规则链执行 → 三维度校验 → 排序打分 → 结果封装 降级策略：规则引擎异常 → TEMP库位 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PutawayRecommendEngine {

    private final PutawayRuleService ruleService;
    private final PutawayRuleCacheService ruleCacheService;
    private final RuleCodeStrategyFactory strategyFactory;
    private final LocationLimitValidator locationLimitValidator;
    private final SpaceLimitValidator spaceLimitValidator;
    private final ExtendedConstraintValidator extendedConstraintValidator;
    private final LocationScorer locationScorer;
    private final LocationQueryService locationQueryService;
    private final ObjectMapper objectMapper;

    /** 核心推荐方法 */
    public PutawayRecommendResult recommend(PutawayRecommendContext context) {
        long startTime = System.currentTimeMillis();

        try {
            // Step 1: 规则解析
            PutawayRule rule = resolveRule(context);
            if (rule == null) {
                return PutawayRecommendResult.fail("NO_RULE", "未找到适用的上架规则");
            }

            List<PutawayRuleLine> lines = ruleService.getRuleLines(rule.getId());
            if (lines == null || lines.isEmpty()) {
                return PutawayRecommendResult.fail("NO_RULE_LINE", "规则无配置行: " + rule.getRuleCode());
            }

            // Step 2: 规则链执行
            RuleExecutionResult executionResult = executeRuleChain(rule, lines, context);
            if (!executionResult.isSuccess()) {
                return PutawayRecommendResult.fail(
                        "NO_CANDIDATE", "所有规则行均无可用库位: " + rule.getRuleCode());
            }

            // Step 3 & 4: 三维度校验 + 排序打分
            List<PutawayRecommendResult.RecommendLocation> candidates =
                    validateAndScore(executionResult, context);

            if (candidates.isEmpty()) {
                return PutawayRecommendResult.fail(
                        "ALL_VALIDATE_FAIL", "所有候选库位均未通过校验: 行号=" + executionResult.getHitLineNo());
            }

            // Step 5: 结果封装
            String reason = buildReason(rule, executionResult, candidates);
            PutawayRecommendResult result =
                    PutawayRecommendResult.success(
                            rule.getId(),
                            rule.getRuleCode(),
                            executionResult.getHitLineNo(),
                            candidates,
                            reason);
            result.setCostTime(System.currentTimeMillis() - startTime);
            return result;

        } catch (Exception e) {
            log.error(
                    "上架推荐引擎异常: sku={}, warehouse={}",
                    context.getSkuCode(),
                    context.getWarehouseCode(),
                    e);
            return fallbackRecommend(context, startTime, e);
        }
    }

    // Step 1: 规则解析（多级缓存：本地Caffeine → Redis → Oracle）
    private PutawayRule resolveRule(PutawayRecommendContext context) {
        // 从缓存获取匹配的规则ID列表
        List<Long> ruleIds =
                ruleCacheService.getMatchedRuleIds(
                        context.getWarehouseCode(), context.getOwnerCode(), context.getSkuCode());
        if (ruleIds == null || ruleIds.isEmpty()) {
            return null;
        }

        // 从缓存逐个获取规则，按优先级排序
        List<PutawayRule> rules = new ArrayList<>();
        for (Long ruleId : ruleIds) {
            PutawayRule rule = ruleCacheService.getRuleWithLines(ruleId);
            if (rule != null && "ACTIVE".equals(rule.getStatus())) {
                rules.add(rule);
            }
        }

        if (rules.isEmpty()) {
            return null;
        }

        // 按优先级排序，取最高优先级的规则
        rules.sort(
                Comparator.comparing(
                        PutawayRule::getPriority, Comparator.nullsLast(Comparator.naturalOrder())));
        return rules.get(0);
    }

    private boolean isWarehouseApplicable(PutawayRule rule, String warehouseCode) {
        if (rule.getWarehouseCode() != null && !rule.getWarehouseCode().isEmpty()) {
            return rule.getWarehouseCode().equals(warehouseCode);
        }
        if (rule.getWarehouseCodes() != null && !rule.getWarehouseCodes().isEmpty()) {
            try {
                List<String> codes =
                        objectMapper.readValue(
                                rule.getWarehouseCodes(), new TypeReference<List<String>>() {});
                return codes.contains(warehouseCode);
            } catch (Exception e) {
                log.warn("解析warehouseCodes失败: {}", rule.getWarehouseCodes());
            }
        }
        return true;
    }

    private boolean isOwnerApplicable(PutawayRule rule, String ownerCode) {
        if (rule.getOwnerCode() != null && !rule.getOwnerCode().isEmpty()) {
            return rule.getOwnerCode().equals(ownerCode);
        }
        return true;
    }

    // Step 2: 规则链执行
    private RuleExecutionResult executeRuleChain(
            PutawayRule rule, List<PutawayRuleLine> lines, PutawayRecommendContext context) {
        Map<Integer, PutawayRuleLine> lineMap =
                lines.stream().collect(Collectors.toMap(PutawayRuleLine::getLineNo, l -> l));

        int currentLineNo = lines.get(0).getLineNo();
        Set<Integer> visitedLines = new HashSet<>();

        while (currentLineNo > 0
                && lineMap.containsKey(currentLineNo)
                && !visitedLines.contains(currentLineNo)) {
            visitedLines.add(currentLineNo);
            PutawayRuleLine line = lineMap.get(currentLineNo);

            if (!matchCondition(line, context)) {
                currentLineNo = getNextLineNo(lines, currentLineNo);
                continue;
            }

            try {
                RuleCodeStrategy strategy = strategyFactory.getStrategy(line.getRuleCode());
                List<String> candidateCodes = strategy.getCandidateLocations(line, context);

                if (candidateCodes != null && !candidateCodes.isEmpty()) {
                    RuleExecutionResult result = new RuleExecutionResult();
                    result.setSuccess(true);
                    result.setHitLineNo(currentLineNo);
                    result.setRuleLine(line);
                    result.setCandidateCodes(candidateCodes);
                    return result;
                }
            } catch (Exception e) {
                log.warn("规则代码执行异常: ruleCode={}, lineNo={}", line.getRuleCode(), currentLineNo, e);
            }

            if (line.getFailJumpLine() != null && line.getFailJumpLine() > 0) {
                currentLineNo = line.getFailJumpLine();
            } else {
                currentLineNo = getNextLineNo(lines, currentLineNo);
            }
        }

        RuleExecutionResult result = new RuleExecutionResult();
        result.setSuccess(false);
        return result;
    }

    private boolean matchCondition(PutawayRuleLine line, PutawayRecommendContext context) {
        if (line.getConditionJson() == null || line.getConditionJson().isEmpty()) {
            return true;
        }

        try {
            LineCondition condition =
                    objectMapper.readValue(line.getConditionJson(), LineCondition.class);

            if (condition.getOrderType() != null
                    && !condition.getOrderType().isEmpty()
                    && !condition.getOrderType().equals(context.getOrderType())) {
                return false;
            }
            if (condition.getPackageLevel() != null
                    && !condition.getPackageLevel().isEmpty()
                    && !condition.getPackageLevel().equals(context.getPackageLevel())) {
                return false;
            }
            if (condition.getCycleLevel() != null
                    && !condition.getCycleLevel().isEmpty()
                    && !condition.getCycleLevel().equals(context.getCycleLevel())) {
                return false;
            }
            if (condition.getBatchAttrKey() != null && !condition.getBatchAttrKey().isEmpty()) {
                String actualValue =
                        context.getBatchAttrs() != null
                                ? context.getBatchAttrs().get(condition.getBatchAttrKey())
                                : null;
                if (!condition.getBatchAttrValue().equals(actualValue)) {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            log.warn("解析规则行条件失败: {}", line.getConditionJson(), e);
            return false;
        }
    }

    private int getNextLineNo(List<PutawayRuleLine> lines, int currentLineNo) {
        return lines.stream()
                .map(PutawayRuleLine::getLineNo)
                .filter(no -> no > currentLineNo)
                .min(Integer::compareTo)
                .orElse(-1);
    }

    // Step 3 & 4: 三维度校验 + 排序打分
    private List<PutawayRecommendResult.RecommendLocation> validateAndScore(
            RuleExecutionResult executionResult, PutawayRecommendContext context) {

        PutawayRuleLine line = executionResult.getRuleLine();
        List<String> candidateCodes = executionResult.getCandidateCodes();

        List<LocationLimit> locationLimits =
                parseJsonList(line.getLocationLimitsJson(), LocationLimit.class);
        List<SpaceLimit> spaceLimits = parseJsonList(line.getSpaceLimitsJson(), SpaceLimit.class);
        List<ExtendedConstraint> extendedConstraints =
                parseJsonList(line.getExtendedConstraintsJson(), ExtendedConstraint.class);

        List<LocationQueryService.PutawayLocation> locations =
                candidateCodes.stream()
                        .map(
                                code ->
                                        locationQueryService
                                                .queryLocationsByArea(
                                                        context.getWarehouseCode(), null)
                                                .stream()
                                                .filter(l -> l.getLocationCode().equals(code))
                                                .findFirst()
                                                .orElse(null))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());

        List<LocationQueryService.PutawayLocation> validLocations = new ArrayList<>();
        for (LocationQueryService.PutawayLocation location : locations) {
            LocationValidationResult r1 =
                    locationLimitValidator.validate(locationLimits, location, context);
            if (!r1.isPassed()) continue;
            LocationValidationResult r2 =
                    spaceLimitValidator.validate(spaceLimits, location, context);
            if (!r2.isPassed()) continue;
            LocationValidationResult r3 =
                    extendedConstraintValidator.validate(extendedConstraints, location, context);
            if (!r3.isPassed()) continue;
            validLocations.add(location);
        }

        if (validLocations.isEmpty()) {
            return List.of();
        }

        LocationQueryService.PutawayLocation basePoint =
                locationQueryService.getReceivingPoint(context.getWarehouseCode());
        List<LocationScorer.ScoredLocation> scored =
                locationScorer.scoreAndSort(validLocations, context, basePoint);

        return scored.stream()
                .map(this::convertToRecommendLocation)
                .limit(10)
                .collect(Collectors.toList());
    }

    private PutawayRecommendResult.RecommendLocation convertToRecommendLocation(
            LocationScorer.ScoredLocation scored) {
        PutawayRecommendResult.RecommendLocation loc =
                new PutawayRecommendResult.RecommendLocation();
        LocationQueryService.PutawayLocation l = scored.getLocation();
        loc.setLocationCode(l.getLocationCode());
        loc.setZoneCode(l.getAreaCode());
        loc.setDistance(scored.getDistance());
        loc.setScore(scored.getScore());
        loc.setLocationType(l.getLocationType());
        loc.setCurrentSku(l.getCurrentSku());
        loc.setCurrentLot(l.getCurrentBatch());
        if (l.getCapacity() != null
                && l.getUsedCapacity() != null
                && l.getCapacity().compareTo(java.math.BigDecimal.ZERO) > 0) {
            loc.setAvailableVolumeRatio(
                    1 - l.getUsedCapacity().doubleValue() / l.getCapacity().doubleValue());
        }
        if (l.getMaxWeight() != null) {
            loc.setAvailableWeight(l.getMaxWeight().doubleValue());
        }
        return loc;
    }

    // Step 5: 结果封装
    private String buildReason(
            PutawayRule rule,
            RuleExecutionResult executionResult,
            List<PutawayRecommendResult.RecommendLocation> candidates) {
        PutawayRecommendResult.RecommendLocation top = candidates.get(0);
        return String.format(
                "规则[%s]行[%d]命中，推荐库位[%s]，距离%.1fm，综合分%.1f",
                rule.getRuleCode(),
                executionResult.getHitLineNo(),
                top.getLocationCode(),
                top.getDistance(),
                top.getScore());
    }

    // 降级策略
    private PutawayRecommendResult fallbackRecommend(
            PutawayRecommendContext context, long startTime, Exception e) {
        try {
            PutawayRecommendResult.RecommendLocation temp =
                    new PutawayRecommendResult.RecommendLocation();
            temp.setLocationCode("TEMP");
            temp.setZoneCode("TEMP");
            temp.setScore(0);
            PutawayRecommendResult result =
                    PutawayRecommendResult.success(
                            null,
                            "FALLBACK",
                            null,
                            List.of(temp),
                            "推荐引擎异常，降级到TEMP库位: " + e.getMessage());
            result.setCostTime(System.currentTimeMillis() - startTime);
            return result;
        } catch (Exception ex) {
            return PutawayRecommendResult.fail("ENGINE_ERROR", "推荐引擎异常: " + e.getMessage());
        }
    }

    private <T> List<T> parseJsonList(String json, Class<T> clazz) {
        if (json == null || json.isEmpty()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(
                    json, objectMapper.getTypeFactory().constructCollectionType(List.class, clazz));
        } catch (Exception e) {
            log.warn("解析JSON列表失败: {}", json, e);
            return List.of();
        }
    }

    @lombok.Data
    private static class RuleExecutionResult {
        private boolean success;
        private Integer hitLineNo;
        private PutawayRuleLine ruleLine;
        private List<String> candidateCodes;
    }
}
