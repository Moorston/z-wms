package com.xwms.core.safety.service;

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

import com.xwms.core.safety.entity.*;
import com.xwms.core.safety.mapper.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 库存安全库存管理核心服务 核心能力: 安全库存规则/安全库存计算/补货建议 职责边界: 只负责安全库存管理 安全库存预警 -> alert模块 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SafetyStockService {

    private final SafetyRuleMapper ruleMapper;
    private final SafetyStockMapper stockMapper;
    private final ReorderSuggestionMapper suggestionMapper;

    private static final AtomicInteger SEQ = new AtomicInteger(0);
    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    // ============================================================

    // 1. 安全库存规则管理
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public SafetyRule createRule(SafetyRule rule) {
        rule.setStatus("ACTIVE");
        if (rule.getPriority() == null) rule.setPriority(5);
        if (rule.getServiceLevel() == null) rule.setServiceLevel(new BigDecimal("95"));
        if (rule.getZScore() == null) rule.setZScore(new BigDecimal("1.645"));
        ruleMapper.insert(rule);
        log.info(
                "创建安全库存规则: {}={}, method={}",
                rule.getRuleCode(),
                rule.getRuleName(),
                rule.getCalcMethod());
        return rule;
    }

    public Page<SafetyRule> pageRules(Page<SafetyRule> page, String warehouseCode) {
        LambdaQueryWrapper<SafetyRule> wrapper = new LambdaQueryWrapper<>();
        if (warehouseCode != null) wrapper.eq(SafetyRule::getWarehouseCode, warehouseCode);
        wrapper.eq(SafetyRule::getStatus, "ACTIVE");
        return ruleMapper.selectPage(page, wrapper);
    }

    public SafetyRule getRuleByCode(String ruleCode) {
        return ruleMapper.selectByRuleCode(ruleCode);
    }

    public SafetyRule matchRule(
            String warehouseCode,
            String ownerCode,
            String skuCode,
            String categoryCode,
            String abcClass) {
        return ruleMapper.matchRule(warehouseCode, ownerCode, skuCode, categoryCode, abcClass);
    }

    // ============================================================

    // 2. 安全库存计算（核心）
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public SafetyStock calculateSafetyStock(
            String warehouseCode,
            String ownerCode,
            String skuCode,
            String skuName,
            String categoryCode,
            String abcClass,
            BigDecimal avgDemand,
            BigDecimal stdDemand,
            BigDecimal leadTime,
            BigDecimal currentStock,
            String ruleCode) {
        SafetyRule rule =
                ruleCode != null
                        ? ruleMapper.selectByRuleCode(ruleCode)
                        : ruleMapper.matchRule(
                                warehouseCode, ownerCode, skuCode, categoryCode, abcClass);
        if (rule == null) {
            throw new RuntimeException("未找到安全库存规则");
        }

        BigDecimal safetyStock;
        BigDecimal reorderPoint;
        BigDecimal maxStock;

        switch (rule.getCalcMethod()) {
            case "FIXED" -> {
                safetyStock =
                        rule.getFixedSafety() != null ? rule.getFixedSafety() : BigDecimal.ZERO;
                reorderPoint =
                        rule.getFixedReorder() != null ? rule.getFixedReorder() : safetyStock;
                maxStock =
                        rule.getFixedMax() != null
                                ? rule.getFixedMax()
                                : reorderPoint.multiply(BigDecimal.valueOf(2));
            }
            case "STATISTICAL" -> {
                BigDecimal zScore =
                        rule.getZScore() != null ? rule.getZScore() : new BigDecimal("1.645");
                safetyStock =
                        zScore.multiply(stdDemand)
                                .multiply(BigDecimal.valueOf(Math.sqrt(leadTime.doubleValue())))
                                .setScale(4, RoundingMode.HALF_UP);
                reorderPoint = avgDemand.multiply(leadTime).add(safetyStock);
                maxStock = reorderPoint.add(avgDemand.multiply(leadTime));
            }
            case "LEAD_TIME" -> {
                safetyStock =
                        avgDemand
                                .multiply(leadTime)
                                .multiply(new BigDecimal("0.2"))
                                .setScale(4, RoundingMode.HALF_UP);
                reorderPoint = avgDemand.multiply(leadTime).add(safetyStock);
                maxStock = reorderPoint.add(avgDemand.multiply(leadTime));
            }
            case "SERVICE_LEVEL" -> {
                BigDecimal zScore = getZScore(rule.getServiceLevel());
                safetyStock =
                        zScore.multiply(stdDemand)
                                .multiply(BigDecimal.valueOf(Math.sqrt(leadTime.doubleValue())))
                                .setScale(4, RoundingMode.HALF_UP);
                reorderPoint = avgDemand.multiply(leadTime).add(safetyStock);
                maxStock = reorderPoint.add(avgDemand.multiply(leadTime));
            }
            default -> {
                safetyStock = BigDecimal.ZERO;
                reorderPoint = BigDecimal.ZERO;
                maxStock = BigDecimal.ZERO;
            }
        }

        String stockStatus =
                determineStockStatus(currentStock, safetyStock, reorderPoint, maxStock);

        stockMapper.markAsHistory(warehouseCode);

        SafetyStock stock = new SafetyStock();
        stock.setStockId(generateStockId());
        stock.setWarehouseCode(warehouseCode);
        stock.setOwnerCode(ownerCode);
        stock.setSkuCode(skuCode);
        stock.setSkuName(skuName);
        stock.setCategoryCode(categoryCode);
        stock.setAbcClass(abcClass);
        stock.setSafetyStock(safetyStock);
        stock.setReorderPoint(reorderPoint);
        stock.setMaxStock(maxStock);
        stock.setAvgDemand(avgDemand);
        stock.setStdDemand(stdDemand);
        stock.setLeadTime(leadTime);
        stock.setServiceLevel(rule.getServiceLevel());
        stock.setCalcMethod(rule.getCalcMethod());
        stock.setCurrentStock(currentStock);
        stock.setStockStatus(stockStatus);
        stock.setCalcTime(LocalDateTime.now());
        stock.setStatus("CURRENT");
        stockMapper.insert(stock);

        checkReorder(stock);

        log.info(
                "计算安全库存: sku={}, method={}, safety={}, reorder={}, max={}, status={}",
                skuCode,
                rule.getCalcMethod(),
                safetyStock,
                reorderPoint,
                maxStock,
                stockStatus);
        return stock;
    }

    private BigDecimal getZScore(BigDecimal serviceLevel) {
        if (serviceLevel == null) return new BigDecimal("1.645");
        double sl = serviceLevel.doubleValue();
        if (sl >= 99) return new BigDecimal("2.326");
        if (sl >= 98) return new BigDecimal("2.054");
        if (sl >= 95) return new BigDecimal("1.645");
        if (sl >= 90) return new BigDecimal("1.282");
        if (sl >= 85) return new BigDecimal("1.036");
        return new BigDecimal("0.842");
    }

    private String determineStockStatus(
            BigDecimal currentStock,
            BigDecimal safetyStock,
            BigDecimal reorderPoint,
            BigDecimal maxStock) {
        if (currentStock.compareTo(BigDecimal.ZERO) <= 0) return "OUT_OF_STOCK";
        if (currentStock.compareTo(safetyStock) < 0) return "BELOW_SAFETY";
        if (currentStock.compareTo(reorderPoint) < 0) return "BELOW_REORDER";
        if (maxStock != null && currentStock.compareTo(maxStock) > 0) return "OVER_STOCK";
        return "NORMAL";
    }

    // ============================================================

    // 3. 补货建议
    // ============================================================

    private void checkReorder(SafetyStock stock) {
        if ("BELOW_REORDER".equals(stock.getStockStatus())
                || "BELOW_SAFETY".equals(stock.getStockStatus())
                || "OUT_OF_STOCK".equals(stock.getStockStatus())) {
            createReorderSuggestion(stock);
        }
    }

    private ReorderSuggestion createReorderSuggestion(SafetyStock stock) {
        BigDecimal suggestQty = stock.getMaxStock().subtract(stock.getCurrentStock());
        String suggestType =
                "OUT_OF_STOCK".equals(stock.getStockStatus())
                        ? "URGENT"
                        : "BELOW_SAFETY".equals(stock.getStockStatus()) ? "URGENT" : "NORMAL";

        ReorderSuggestion suggestion = new ReorderSuggestion();
        suggestion.setSuggestionNo(generateSuggestionNo());
        suggestion.setWarehouseCode(stock.getWarehouseCode());
        suggestion.setOwnerCode(stock.getOwnerCode());
        suggestion.setSkuCode(stock.getSkuCode());
        suggestion.setSkuName(stock.getSkuName());
        suggestion.setCurrentStock(stock.getCurrentStock());
        suggestion.setReorderPoint(stock.getReorderPoint());
        suggestion.setSafetyStock(stock.getSafetyStock());
        suggestion.setMaxStock(stock.getMaxStock());
        suggestion.setSuggestQty(suggestQty);
        suggestion.setSuggestType(suggestType);
        suggestion.setStatus("PENDING");
        suggestion.setSource("AUTO");
        suggestionMapper.insert(suggestion);

        log.info("生成补货建议: sku={}, type={}, qty={}", stock.getSkuCode(), suggestType, suggestQty);
        return suggestion;
    }

    @Transactional(rollbackFor = Exception.class)
    public ReorderSuggestion convertToReplenish(
            String suggestionNo, String replenishNo, String operator) {
        ReorderSuggestion suggestion = suggestionMapper.selectBySuggestionNo(suggestionNo);
        if (suggestion == null) throw new RuntimeException("补货建议不存在: " + suggestionNo);
        if (!"PENDING".equals(suggestion.getStatus())) {
            throw new RuntimeException("补货建议状态不正确: " + suggestion.getStatus());
        }
        suggestionMapper.updateStatus(suggestionNo, "CONVERTED", replenishNo);
        log.info("补货建议转单: {} -> {}", suggestionNo, replenishNo);
        return suggestionMapper.selectBySuggestionNo(suggestionNo);
    }

    @Transactional(rollbackFor = Exception.class)
    public ReorderSuggestion ignoreSuggestion(String suggestionNo, String operator) {
        ReorderSuggestion suggestion = suggestionMapper.selectBySuggestionNo(suggestionNo);
        if (suggestion == null) throw new RuntimeException("补货建议不存在: " + suggestionNo);
        suggestionMapper.updateStatus(suggestionNo, "IGNORED", null);
        log.info("忽略补货建议: {}", suggestionNo);
        return suggestionMapper.selectBySuggestionNo(suggestionNo);
    }

    // ============================================================

    // 4. 查询
    // ============================================================

    public List<SafetyStock> getCurrentStock(String warehouseCode) {
        return stockMapper.selectCurrentByWarehouse(warehouseCode);
    }

    public SafetyStock getCurrentBySku(String skuCode) {
        return stockMapper.selectCurrentBySku(skuCode);
    }

    public List<SafetyStock> getByStockStatus(String warehouseCode, String stockStatus) {
        return stockMapper.selectByStockStatus(warehouseCode, stockStatus);
    }

    public Page<ReorderSuggestion> pageSuggestions(
            Page<ReorderSuggestion> page, String warehouseCode, String status) {
        LambdaQueryWrapper<ReorderSuggestion> wrapper = new LambdaQueryWrapper<>();
        if (warehouseCode != null) wrapper.eq(ReorderSuggestion::getWarehouseCode, warehouseCode);
        if (status != null) wrapper.eq(ReorderSuggestion::getStatus, status);
        wrapper.orderByDesc(ReorderSuggestion::getCreatedTime);
        return suggestionMapper.selectPage(page, wrapper);
    }

    public ReorderSuggestion getSuggestionByNo(String suggestionNo) {
        return suggestionMapper.selectBySuggestionNo(suggestionNo);
    }

    // ============================================================

    // 工具方法
    // ============================================================

    private String generateStockId() {
        return "SS"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }

    private String generateSuggestionNo() {
        return "RS"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }
}
