package com.xwms.core.abc.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.core.abc.entity.*;
import com.xwms.core.abc.mapper.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 库存ABC分类管理核心服务 核心能力: ABC分类规则/ABC分类计算/XYZ分类计算/分类调整/分类历史 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AbcService {

    private final AbcRuleMapper ruleMapper;
    private final AbcClassificationMapper classificationMapper;
    private final XyzClassificationMapper xyzMapper;
    private final AbcHistoryMapper historyMapper;

    private static final AtomicInteger SEQ = new AtomicInteger(0);
    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    // ============================================================

    // 1. ABC分类规则管理
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public AbcRule createRule(AbcRule rule) {
        rule.setStatus("ACTIVE");
        if (rule.getPeriodType() == null) rule.setPeriodType("MONTH");
        if (rule.getARatio() == null) rule.setARatio(new BigDecimal("80"));
        if (rule.getBRatio() == null) rule.setBRatio(new BigDecimal("15"));
        if (rule.getCRatio() == null) rule.setCRatio(new BigDecimal("5"));
        if (rule.getACountRatio() == null) rule.setACountRatio(new BigDecimal("20"));
        if (rule.getBCountRatio() == null) rule.setBCountRatio(new BigDecimal("30"));
        if (rule.getCCountRatio() == null) rule.setCCountRatio(new BigDecimal("50"));
        ruleMapper.insert(rule);
        log.info(
                "创建ABC分类规则: {}={}, classifyBy={}",
                rule.getRuleCode(),
                rule.getRuleName(),
                rule.getClassifyBy());
        return rule;
    }

    public Page<AbcRule> pageRules(Page<AbcRule> page, String warehouseCode) {
        LambdaQueryWrapper<AbcRule> wrapper = new LambdaQueryWrapper<>();
        if (warehouseCode != null) wrapper.eq(AbcRule::getWarehouseCode, warehouseCode);
        wrapper.eq(AbcRule::getStatus, "ACTIVE");
        return ruleMapper.selectPage(page, wrapper);
    }

    public AbcRule getRuleByCode(String ruleCode) {
        return ruleMapper.selectByRuleCode(ruleCode);
    }

    public AbcRule matchRule(String warehouseCode, String ownerCode, String categoryCode) {
        return ruleMapper.matchRule(warehouseCode, ownerCode, categoryCode);
    }

    // ============================================================

    // 2. ABC分类计算（核心）
    // ============================================================

    /** 执行ABC分类计算 原理: 按分类依据降序排列，累计占比达到A类阈值的为A类，依次类推 */
    @Transactional(rollbackFor = Exception.class)
    public List<AbcClassification> calculateAbcClassification(
            String warehouseCode,
            String ownerCode,
            String ruleCode,
            List<Map<String, Object>> skuData,
            LocalDateTime periodStart,
            LocalDateTime periodEnd) {
        AbcRule rule =
                ruleCode != null
                        ? ruleMapper.selectByRuleCode(ruleCode)
                        : ruleMapper.matchRule(warehouseCode, ownerCode, null);
        if (rule == null) {
            throw new RuntimeException("未找到ABC分类规则");
        }

        // 将当前分类标记为历史
        classificationMapper.markAsHistory(warehouseCode);

        String classifyId = generateClassifyId();

        // 按分类依据排序
        String sortField = rule.getClassifyBy();
        skuData.sort(
                (a, b) -> {
                    BigDecimal aVal = getSortValue(a, sortField);
                    BigDecimal bVal = getSortValue(b, sortField);
                    return bVal.compareTo(aVal); // 降序
                });

        // 计算总值
        BigDecimal totalValue =
                skuData.stream()
                        .map(d -> getSortValue(d, sortField))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 计算累计占比并分类
        BigDecimal cumulative = BigDecimal.ZERO;
        int rank = 1;
        List<AbcClassification> results = new ArrayList<>();

        for (Map<String, Object> data : skuData) {
            BigDecimal value = getSortValue(data, sortField);
            cumulative = cumulative.add(value);
            BigDecimal cumulativeRatio =
                    totalValue.compareTo(BigDecimal.ZERO) > 0
                            ? cumulative
                                    .multiply(BigDecimal.valueOf(100))
                                    .divide(totalValue, 2, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO;

            String abcClass = determineAbcClass(cumulativeRatio, rule);

            AbcClassification classification = new AbcClassification();
            classification.setClassifyId(classifyId);
            classification.setWarehouseCode(warehouseCode);
            classification.setOwnerCode(ownerCode);
            classification.setSkuCode((String) data.get("skuCode"));
            classification.setSkuName((String) data.get("skuName"));
            classification.setCategoryCode((String) data.get("categoryCode"));
            classification.setAbcClass(abcClass);
            classification.setSalesAmount(toBigDecimal(data.get("salesAmount")));
            classification.setSalesQty(toBigDecimal(data.get("salesQty")));
            classification.setProfit(toBigDecimal(data.get("profit")));
            classification.setTurnoverRate(toBigDecimal(data.get("turnoverRate")));
            classification.setTurnoverDays(toBigDecimal(data.get("turnoverDays")));
            classification.setAvgInventory(toBigDecimal(data.get("avgInventory")));
            classification.setCumulativeRatio(cumulativeRatio);
            classification.setRankNo(rank++);
            classification.setPeriodStart(periodStart);
            classification.setPeriodEnd(periodEnd);
            classification.setClassifyTime(LocalDateTime.now());
            classification.setStatus("CURRENT");
            classificationMapper.insert(classification);

            // 记录分类变化历史
            recordClassificationHistory(
                    warehouseCode,
                    classification.getSkuCode(),
                    null,
                    abcClass,
                    null,
                    null,
                    "自动分类",
                    "SYSTEM");

            results.add(classification);
        }

        log.info(
                "ABC分类计算完成: warehouse={}, skuCount={}, classifyId={}",
                warehouseCode,
                results.size(),
                classifyId);
        return results;
    }

    /** 确定ABC分类 */
    private String determineAbcClass(BigDecimal cumulativeRatio, AbcRule rule) {
        if (cumulativeRatio.compareTo(rule.getARatio()) <= 0) {
            return "A";
        } else if (cumulativeRatio.compareTo(rule.getARatio().add(rule.getBRatio())) <= 0) {
            return "B";
        } else {
            return "C";
        }
    }

    private BigDecimal getSortValue(Map<String, Object> data, String sortField) {
        return switch (sortField) {
            case "SALES_AMOUNT" -> toBigDecimal(data.get("salesAmount"));
            case "SALES_QTY" -> toBigDecimal(data.get("salesQty"));
            case "PROFIT" -> toBigDecimal(data.get("profit"));
            case "TURNOVER" -> toBigDecimal(data.get("turnoverRate"));
            default -> BigDecimal.ZERO;
        };
    }

    private BigDecimal toBigDecimal(Object obj) {
        if (obj == null) return BigDecimal.ZERO;
        if (obj instanceof BigDecimal) return (BigDecimal) obj;
        return new BigDecimal(obj.toString());
    }

    // ============================================================

    // 3. XYZ分类计算（需求波动性）
    // ============================================================

    /** 执行XYZ分类计算 X: CV <= 0.2 (需求稳定) Y: 0.2 < CV <= 0.5 (需求波动中等) Z: CV > 0.5 (需求波动大) */
    @Transactional(rollbackFor = Exception.class)
    public List<XyzClassification> calculateXyzClassification(
            String warehouseCode,
            List<Map<String, Object>> demandData,
            LocalDateTime periodStart,
            LocalDateTime periodEnd) {
        xyzMapper.markAsHistory(warehouseCode);

        String classifyId = generateClassifyId();
        BigDecimal thresholdX = new BigDecimal("0.2");
        BigDecimal thresholdY = new BigDecimal("0.5");

        List<XyzClassification> results = new ArrayList<>();

        for (Map<String, Object> data : demandData) {
            String skuCode = (String) data.get("skuCode");
            @SuppressWarnings("unchecked")
            List<BigDecimal> demands = (List<BigDecimal>) data.get("demands");

            if (demands == null || demands.isEmpty()) continue;

            // 计算平均值
            BigDecimal avg =
                    demands.stream()
                            .reduce(BigDecimal.ZERO, BigDecimal::add)
                            .divide(BigDecimal.valueOf(demands.size()), 4, RoundingMode.HALF_UP);

            // 计算标准差
            BigDecimal variance =
                    demands.stream()
                            .map(d -> d.subtract(avg).pow(2))
                            .reduce(BigDecimal.ZERO, BigDecimal::add)
                            .divide(BigDecimal.valueOf(demands.size()), 4, RoundingMode.HALF_UP);
            BigDecimal stdDev = BigDecimal.valueOf(Math.sqrt(variance.doubleValue()));

            // 计算变异系数 CV = 标准差 / 平均值
            BigDecimal cv =
                    avg.compareTo(BigDecimal.ZERO) > 0
                            ? stdDev.divide(avg, 4, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO;

            String xyzClass =
                    cv.compareTo(thresholdX) <= 0 ? "X" : cv.compareTo(thresholdY) <= 0 ? "Y" : "Z";

            XyzClassification xyz = new XyzClassification();
            xyz.setClassifyId(classifyId);
            xyz.setWarehouseCode(warehouseCode);
            xyz.setSkuCode(skuCode);
            xyz.setXyzClass(xyzClass);
            xyz.setAvgDemand(avg);
            xyz.setStdDev(stdDev);
            xyz.setCv(cv);
            xyz.setMaxDemand(demands.stream().max(BigDecimal::compareTo).orElse(BigDecimal.ZERO));
            xyz.setMinDemand(demands.stream().min(BigDecimal::compareTo).orElse(BigDecimal.ZERO));
            xyz.setDemandCvThresholdX(thresholdX);
            xyz.setDemandCvThresholdY(thresholdY);
            xyz.setPeriodStart(periodStart);
            xyz.setPeriodEnd(periodEnd);
            xyz.setClassifyTime(LocalDateTime.now());
            xyz.setStatus("CURRENT");
            xyzMapper.insert(xyz);

            results.add(xyz);
        }

        log.info("XYZ分类计算完成: warehouse={}, skuCount={}", warehouseCode, results.size());
        return results;
    }

    // ============================================================

    // 4. 手动调整分类
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public AbcClassification adjustAbcClass(
            String skuCode, String newAbcClass, String reason, String operator) {
        AbcClassification current = classificationMapper.selectCurrentBySku(skuCode);
        if (current == null) throw new RuntimeException("未找到SKU的当前分类: " + skuCode);

        String oldAbcClass = current.getAbcClass();
        current.setAbcClass(newAbcClass);
        classificationMapper.updateById(current);

        // 记录历史
        String changeType = determineChangeType(oldAbcClass, newAbcClass);
        recordClassificationHistory(
                current.getWarehouseCode(),
                skuCode,
                oldAbcClass,
                newAbcClass,
                null,
                null,
                reason,
                operator);

        log.info("手动调整ABC分类: sku={}, {}->{}, reason={}", skuCode, oldAbcClass, newAbcClass, reason);
        return current;
    }

    private String determineChangeType(String oldClass, String newClass) {
        if (oldClass == null || newClass == null) return "UNCHANGED";
        int oldRank = "A".equals(oldClass) ? 3 : "B".equals(oldClass) ? 2 : 1;
        int newRank = "A".equals(newClass) ? 3 : "B".equals(newClass) ? 2 : 1;
        if (newRank > oldRank) return "UPGRADE";
        if (newRank < oldRank) return "DOWNGRADE";
        return "UNCHANGED";
    }

    // ============================================================

    // 5. 查询
    // ============================================================

    public List<AbcClassification> getCurrentClassification(String warehouseCode) {
        return classificationMapper.selectCurrentByWarehouse(warehouseCode);
    }

    public AbcClassification getCurrentBySku(String skuCode) {
        return classificationMapper.selectCurrentBySku(skuCode);
    }

    public List<AbcClassification> getByClass(String warehouseCode, String abcClass) {
        return classificationMapper.selectByClass(warehouseCode, abcClass);
    }

    public XyzClassification getXyzBySku(String skuCode) {
        return xyzMapper.selectCurrentBySku(skuCode);
    }

    public List<XyzClassification> getXyzByWarehouse(String warehouseCode) {
        return xyzMapper.selectCurrentByWarehouse(warehouseCode);
    }

    public List<AbcHistory> getHistoryBySku(String skuCode) {
        return historyMapper.selectBySku(skuCode);
    }

    public List<AbcHistory> getHistoryByWarehouse(String warehouseCode) {
        return historyMapper.selectByWarehouse(warehouseCode);
    }

    /** 获取AX组合分类（ABC+XYZ九象限） */
    public Map<String, List<String>> getAxMatrix(String warehouseCode) {
        List<AbcClassification> abcList =
                classificationMapper.selectCurrentByWarehouse(warehouseCode);
        Map<String, List<String>> matrix = new HashMap<>();

        for (AbcClassification abc : abcList) {
            XyzClassification xyz = xyzMapper.selectCurrentBySku(abc.getSkuCode());
            String xyzClass = xyz != null ? xyz.getXyzClass() : "?";
            String key = abc.getAbcClass() + xyzClass;
            matrix.computeIfAbsent(key, k -> new ArrayList<>()).add(abc.getSkuCode());
        }

        return matrix;
    }

    // ============================================================

    // 工具方法
    // ============================================================

    private void recordClassificationHistory(
            String warehouseCode,
            String skuCode,
            String oldAbcClass,
            String newAbcClass,
            String oldXyzClass,
            String newXyzClass,
            String reason,
            String operator) {
        AbcHistory history = new AbcHistory();
        history.setHistoryId(generateHistoryId());
        history.setWarehouseCode(warehouseCode);
        history.setSkuCode(skuCode);
        history.setOldAbcClass(oldAbcClass);
        history.setNewAbcClass(newAbcClass);
        history.setOldXyzClass(oldXyzClass);
        history.setNewXyzClass(newXyzClass);
        history.setChangeType(determineChangeType(oldAbcClass, newAbcClass));
        history.setChangeReason(reason);
        history.setOperator(operator);
        history.setChangeTime(LocalDateTime.now());
        historyMapper.insert(history);
    }

    private String generateClassifyId() {
        return "ABC"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }

    private String generateHistoryId() {
        return "HIS"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }
}
