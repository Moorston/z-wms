package com.xwms.core.metrics.service;

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

import com.xwms.core.metrics.entity.*;
import com.xwms.core.metrics.mapper.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryMetricsService {

    private final MetricsSystemMapper systemMapper;
    private final MetricsCategoryMapper categoryMapper;
    private final MetricsCalculationMapper calculationMapper;
    private final MetricsMonitorMapper monitorMapper;

    private static final AtomicInteger SEQ = new AtomicInteger(0);
    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    // ==================== 指标体系 ====================

    @Transactional(rollbackFor = Exception.class)
    public MetricsSystem createMetricsSystem(
            String systemName,
            String systemCode,
            String systemType,
            String warehouseCode,
            String ownerCode,
            String description,
            String systemConfig,
            Integer sortOrder,
            String createdBy) {
        MetricsSystem system = new MetricsSystem();
        system.setSystemId(generateId("MS"));
        system.setSystemName(systemName);
        system.setSystemCode(systemCode);
        system.setSystemType(systemType);
        system.setWarehouseCode(warehouseCode);
        system.setOwnerCode(ownerCode);
        system.setDescription(description);
        system.setSystemConfig(systemConfig);
        system.setMetricCount(0);
        system.setCategoryCount(0);
        system.setIsDefault("N");
        system.setIsActive("Y");
        system.setSortOrder(sortOrder != null ? sortOrder : 100);
        system.setCreatedBy(createdBy);
        systemMapper.insert(system);
        log.info("创建指标体系: id={}, name={}, type={}", system.getSystemId(), systemName, systemType);
        return system;
    }

    public List<MetricsSystem> getActiveSystemsByType(String systemType) {
        return systemMapper.selectActiveByType(systemType);
    }

    public MetricsSystem getDefaultSystem(String warehouseCode) {
        return systemMapper.selectDefaultByWarehouse(warehouseCode);
    }

    public Page<MetricsSystem> pageMetricsSystem(
            Page<MetricsSystem> page, String systemType, String warehouseCode, String isActive) {
        LambdaQueryWrapper<MetricsSystem> wrapper = new LambdaQueryWrapper<>();
        if (systemType != null) wrapper.eq(MetricsSystem::getSystemType, systemType);
        if (warehouseCode != null) wrapper.eq(MetricsSystem::getWarehouseCode, warehouseCode);
        if (isActive != null) wrapper.eq(MetricsSystem::getIsActive, isActive);
        wrapper.orderByAsc(MetricsSystem::getSortOrder);
        return systemMapper.selectPage(page, wrapper);
    }

    // ==================== 指标分类 ====================

    @Transactional(rollbackFor = Exception.class)
    public MetricsCategory createMetricsCategory(
            String categoryName,
            String categoryCode,
            String systemId,
            String parentCategoryId,
            Integer categoryLevel,
            String description,
            String categoryConfig,
            Integer sortOrder,
            String createdBy) {
        MetricsCategory category = new MetricsCategory();
        category.setCategoryId(generateId("MC"));
        category.setCategoryName(categoryName);
        category.setCategoryCode(categoryCode);
        category.setSystemId(systemId);
        category.setParentCategoryId(parentCategoryId);
        category.setCategoryLevel(categoryLevel != null ? categoryLevel : 1);
        category.setDescription(description);
        category.setCategoryConfig(categoryConfig);
        category.setMetricCount(0);
        category.setIsActive("Y");
        category.setSortOrder(sortOrder != null ? sortOrder : 100);
        category.setCreatedBy(createdBy);
        categoryMapper.insert(category);
        // 更新体系分类数
        MetricsSystem system = systemMapper.selectBySystemId(systemId);
        if (system != null) {
            system.setCategoryCount(system.getCategoryCount() + 1);
            systemMapper.updateById(system);
        }
        log.info(
                "创建指标分类: id={}, name={}, system={}",
                category.getCategoryId(),
                categoryName,
                systemId);
        return category;
    }

    public List<MetricsCategory> getActiveCategoriesBySystem(String systemId) {
        return categoryMapper.selectActiveBySystem(systemId);
    }

    public List<MetricsCategory> getActiveCategoriesByParent(String parentCategoryId) {
        return categoryMapper.selectActiveByParent(parentCategoryId);
    }

    public Page<MetricsCategory> pageMetricsCategory(
            Page<MetricsCategory> page, String systemId, String parentCategoryId, String isActive) {
        LambdaQueryWrapper<MetricsCategory> wrapper = new LambdaQueryWrapper<>();
        if (systemId != null) wrapper.eq(MetricsCategory::getSystemId, systemId);
        if (parentCategoryId != null)
            wrapper.eq(MetricsCategory::getParentCategoryId, parentCategoryId);
        if (isActive != null) wrapper.eq(MetricsCategory::getIsActive, isActive);
        wrapper.orderByAsc(MetricsCategory::getCategoryLevel)
                .orderByAsc(MetricsCategory::getSortOrder);
        return categoryMapper.selectPage(page, wrapper);
    }

    // ==================== 指标计算 ====================

    @Transactional(rollbackFor = Exception.class)
    public MetricsCalculation createCalculation(
            String metricId,
            String metricCode,
            String metricName,
            String systemId,
            String warehouseCode,
            String ownerCode,
            String periodType,
            LocalDateTime periodStart,
            LocalDateTime periodEnd,
            String calcFormula,
            String calcParams,
            String calcUnit,
            BigDecimal targetValue,
            BigDecimal benchmarkValue,
            String operator) {
        MetricsCalculation calc = new MetricsCalculation();
        calc.setCalcId(generateId("MC"));
        calc.setMetricId(metricId);
        calc.setMetricCode(metricCode);
        calc.setMetricName(metricName);
        calc.setSystemId(systemId);
        calc.setWarehouseCode(warehouseCode);
        calc.setOwnerCode(ownerCode);
        calc.setPeriodType(periodType);
        calc.setPeriodStart(periodStart);
        calc.setPeriodEnd(periodEnd);
        calc.setCalcFormula(calcFormula);
        calc.setCalcParams(calcParams);
        calc.setCalcUnit(calcUnit);
        calc.setTargetValue(targetValue);
        calc.setBenchmarkValue(benchmarkValue);
        calc.setStatus("PENDING");
        calc.setOperator(operator);
        calculationMapper.insert(calc);
        log.info("创建指标计算: id={}, metric={}, period={}", calc.getCalcId(), metricCode, periodType);
        return calc;
    }

    @Transactional(rollbackFor = Exception.class)
    public MetricsCalculation executeCalculation(
            String calcId, BigDecimal calcResult, String operator) {
        MetricsCalculation calc = calculationMapper.selectByCalcId(calcId);
        if (calc == null) throw new RuntimeException("指标计算不存在: " + calcId);
        calc.setStatus("RUNNING");
        calc.setCalcStartTime(LocalDateTime.now());
        calculationMapper.updateById(calc);
        // 执行计算
        calc.setCalcResult(calcResult);
        // 计算偏差
        if (calc.getTargetValue() != null) {
            calc.setDeviation(calcResult.subtract(calc.getTargetValue()));
            if (calc.getTargetValue().compareTo(BigDecimal.ZERO) != 0) {
                calc.setDeviationRate(
                        calcResult
                                .subtract(calc.getTargetValue())
                                .divide(calc.getTargetValue(), 4, RoundingMode.HALF_UP)
                                .multiply(BigDecimal.valueOf(100)));
            }
        }
        calc.setStatus("COMPLETED");
        calc.setCalcEndTime(LocalDateTime.now());
        if (calc.getCalcStartTime() != null) {
            calc.setDurationMs(
                    java.time.Duration.between(calc.getCalcStartTime(), calc.getCalcEndTime())
                            .toMillis());
        }
        calc.setOperator(operator);
        calculationMapper.updateById(calc);
        log.info(
                "执行指标计算: id={}, result={}, duration={}ms",
                calcId,
                calcResult,
                calc.getDurationMs());
        return calculationMapper.selectByCalcId(calcId);
    }

    @Transactional(rollbackFor = Exception.class)
    public MetricsCalculation failCalculation(String calcId, String errorMessage, String operator) {
        MetricsCalculation calc = calculationMapper.selectByCalcId(calcId);
        if (calc == null) throw new RuntimeException("指标计算不存在: " + calcId);
        calc.setStatus("FAILED");
        calc.setErrorMessage(errorMessage);
        calc.setCalcEndTime(LocalDateTime.now());
        calc.setOperator(operator);
        calculationMapper.updateById(calc);
        log.error("指标计算失败: id={}, error={}", calcId, errorMessage);
        return calculationMapper.selectByCalcId(calcId);
    }

    public List<MetricsCalculation> getRecentCalculationsByMetric(String metricId, int limit) {
        return calculationMapper.selectRecentByMetric(metricId, limit);
    }

    public List<MetricsCalculation> getCalculationsByWarehouseAndPeriod(
            String warehouseCode, LocalDateTime periodStart, LocalDateTime periodEnd) {
        return calculationMapper.selectByWarehouseAndPeriod(warehouseCode, periodStart, periodEnd);
    }

    public Page<MetricsCalculation> pageCalculation(
            Page<MetricsCalculation> page,
            String metricId,
            String warehouseCode,
            String status,
            String periodType) {
        LambdaQueryWrapper<MetricsCalculation> wrapper = new LambdaQueryWrapper<>();
        if (metricId != null) wrapper.eq(MetricsCalculation::getMetricId, metricId);
        if (warehouseCode != null) wrapper.eq(MetricsCalculation::getWarehouseCode, warehouseCode);
        if (status != null) wrapper.eq(MetricsCalculation::getStatus, status);
        if (periodType != null) wrapper.eq(MetricsCalculation::getPeriodType, periodType);
        wrapper.orderByDesc(MetricsCalculation::getPeriodStart);
        return calculationMapper.selectPage(page, wrapper);
    }

    // ==================== 指标监控 ====================

    @Transactional(rollbackFor = Exception.class)
    public MetricsMonitor createMetricsMonitor(
            String metricId,
            String metricCode,
            String metricName,
            String systemId,
            String warehouseCode,
            String ownerCode,
            String monitorType,
            String monitorConfig,
            BigDecimal targetValue,
            BigDecimal thresholdWarning,
            BigDecimal thresholdCritical,
            String unit,
            Integer checkInterval,
            String operator) {
        MetricsMonitor monitor = new MetricsMonitor();
        monitor.setMonitorId(generateId("MM"));
        monitor.setMetricId(metricId);
        monitor.setMetricCode(metricCode);
        monitor.setMetricName(metricName);
        monitor.setSystemId(systemId);
        monitor.setWarehouseCode(warehouseCode);
        monitor.setOwnerCode(ownerCode);
        monitor.setMonitorType(monitorType != null ? monitorType : "THRESHOLD");
        monitor.setMonitorConfig(monitorConfig);
        monitor.setTargetValue(targetValue);
        monitor.setThresholdWarning(thresholdWarning);
        monitor.setThresholdCritical(thresholdCritical);
        monitor.setUnit(unit);
        monitor.setStatus("NORMAL");
        monitor.setAlertCount(0);
        monitor.setCheckInterval(checkInterval != null ? checkInterval : 60);
        monitor.setIsActive("Y");
        monitor.setOperator(operator);
        monitorMapper.insert(monitor);
        log.info(
                "创建指标监控: id={}, metric={}, type={}",
                monitor.getMonitorId(),
                metricCode,
                monitorType);
        return monitor;
    }

    @Transactional(rollbackFor = Exception.class)
    public MetricsMonitor checkMonitor(String monitorId, BigDecimal currentValue, String operator) {
        MetricsMonitor monitor = monitorMapper.selectByMonitorId(monitorId);
        if (monitor == null) throw new RuntimeException("指标监控不存在: " + monitorId);
        // 计算变化率
        if (monitor.getCurrentValue() != null
                && monitor.getCurrentValue().compareTo(BigDecimal.ZERO) != 0) {
            BigDecimal changeRate =
                    currentValue
                            .subtract(monitor.getCurrentValue())
                            .divide(monitor.getCurrentValue(), 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100));
            monitor.setChangeRate(changeRate);
            monitor.setTrend(
                    changeRate.compareTo(BigDecimal.ZERO) > 0
                            ? "UP"
                            : changeRate.compareTo(BigDecimal.ZERO) < 0 ? "DOWN" : "STABLE");
        }
        monitor.setCurrentValue(currentValue);
        monitor.setLastCheckTime(LocalDateTime.now());
        // 判断状态
        String status = "NORMAL";
        if (monitor.getThresholdCritical() != null
                && currentValue.compareTo(monitor.getThresholdCritical()) >= 0) {
            status = "CRITICAL";
            monitor.setAlertCount(monitor.getAlertCount() + 1);
            monitor.setLastAlertTime(LocalDateTime.now());
        } else if (monitor.getThresholdWarning() != null
                && currentValue.compareTo(monitor.getThresholdWarning()) >= 0) {
            status = "WARNING";
        }
        monitor.setStatus(status);
        monitor.setOperator(operator);
        monitorMapper.updateById(monitor);
        log.info("检查指标监控: id={}, value={}, status={}", monitorId, currentValue, status);
        return monitorMapper.selectByMonitorId(monitorId);
    }

    public MetricsMonitor getActiveMonitorByMetric(String metricId) {
        return monitorMapper.selectActiveByMetric(metricId);
    }

    public List<MetricsMonitor> getActiveMonitorsByWarehouse(String warehouseCode) {
        return monitorMapper.selectActiveByWarehouse(warehouseCode);
    }

    public List<MetricsMonitor> getActiveMonitorsByStatus(String status) {
        return monitorMapper.selectActiveByStatus(status);
    }

    public Page<MetricsMonitor> pageMonitor(
            Page<MetricsMonitor> page,
            String metricId,
            String warehouseCode,
            String status,
            String isActive) {
        LambdaQueryWrapper<MetricsMonitor> wrapper = new LambdaQueryWrapper<>();
        if (metricId != null) wrapper.eq(MetricsMonitor::getMetricId, metricId);
        if (warehouseCode != null) wrapper.eq(MetricsMonitor::getWarehouseCode, warehouseCode);
        if (status != null) wrapper.eq(MetricsMonitor::getStatus, status);
        if (isActive != null) wrapper.eq(MetricsMonitor::getIsActive, isActive);
        wrapper.orderByAsc(MetricsMonitor::getMetricCode);
        return monitorMapper.selectPage(page, wrapper);
    }

    private String generateId(String prefix) {
        return prefix
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }
}
