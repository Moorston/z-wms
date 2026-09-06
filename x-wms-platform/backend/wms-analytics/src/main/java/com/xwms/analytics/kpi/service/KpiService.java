package com.xwms.analytics.kpi.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.analytics.kpi.entity.*;
import com.xwms.analytics.kpi.mapper.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** KPI报表体系核心服务 包含: KPI指标定义/KPI数据采集/KPI目标管理/KPI报表生成 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KpiService {

    private final KpiDefineMapper kpiDefineMapper;
    private final KpiDailyMapper kpiDailyMapper;
    private final KpiTargetMapper kpiTargetMapper;
    private final KpiReportMapper kpiReportMapper;

    private static final AtomicInteger TARGET_SEQ = new AtomicInteger(0);
    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    // ============================================================
    // 1. KPI指标定义
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public KpiDefine createKpiDefine(KpiDefine define) {
        kpiDefineMapper.insert(define);
        log.info("创建KPI指标: {}={}", define.getKpiCode(), define.getKpiName());
        return define;
    }

    @Transactional(rollbackFor = Exception.class)
    public KpiDefine updateKpiDefine(KpiDefine define) {
        kpiDefineMapper.updateById(define);
        return define;
    }

    public Page<KpiDefine> pageKpiDefines(Page<KpiDefine> page, String category, Integer enabled) {
        LambdaQueryWrapper<KpiDefine> wrapper = new LambdaQueryWrapper<>();
        if (category != null) wrapper.eq(KpiDefine::getKpiCategory, category);
        if (enabled != null) wrapper.eq(KpiDefine::getEnabled, enabled);
        wrapper.orderByAsc(KpiDefine::getKpiCategory).orderByAsc(KpiDefine::getSortOrder);
        return kpiDefineMapper.selectPage(page, wrapper);
    }

    public List<KpiDefine> getKpiDefinesByCategory(String category) {
        return kpiDefineMapper.selectByCategory(category);
    }

    public List<KpiDefine> getAllEnabledKpiDefines() {
        return kpiDefineMapper.selectAllEnabled();
    }

    // ============================================================
    // 2. KPI数据采集
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public KpiDaily collectKpiData(
            String kpiCode,
            LocalDate statDate,
            String warehouseCode,
            String department,
            String employeeId,
            BigDecimal actualValue,
            String dataDetail) {
        KpiDefine define =
                kpiDefineMapper.selectOne(
                        new LambdaQueryWrapper<KpiDefine>().eq(KpiDefine::getKpiCode, kpiCode));
        if (define == null) throw new RuntimeException("KPI指标不存在: " + kpiCode);

        // 查询目标值
        BigDecimal targetValue = getTargetValue(kpiCode, statDate, warehouseCode, department);

        // 计算达成率
        BigDecimal targetRate = null;
        if (targetValue != null && targetValue.compareTo(BigDecimal.ZERO) != 0) {
            targetRate =
                    actualValue
                            .divide(targetValue, 4, RoundingMode.HALF_UP)
                            .multiply(new BigDecimal("100"));
        }

        // 查询同比(去年同期)
        BigDecimal compareValue =
                getHistoryValue(
                        kpiCode, statDate.minusYears(1), warehouseCode, department, employeeId);
        BigDecimal compareRate = calcGrowthRate(actualValue, compareValue);

        // 查询环比(上月同期)
        BigDecimal ringValue =
                getHistoryValue(
                        kpiCode, statDate.minusMonths(1), warehouseCode, department, employeeId);
        BigDecimal ringRate = calcGrowthRate(actualValue, ringValue);

        // 保存或更新
        KpiDaily existing =
                kpiDailyMapper.selectOne(
                        new LambdaQueryWrapper<KpiDaily>()
                                .eq(KpiDaily::getKpiCode, kpiCode)
                                .eq(KpiDaily::getStatDate, statDate)
                                .eq(KpiDaily::getWarehouseCode, warehouseCode)
                                .eq(KpiDaily::getDepartment, department)
                                .eq(KpiDaily::getEmployeeId, employeeId != null ? employeeId : ""));

        if (existing != null) {
            existing.setActualValue(actualValue);
            existing.setTargetValue(targetValue);
            existing.setTargetRate(targetRate);
            existing.setCompareValue(compareValue);
            existing.setCompareRate(compareRate);
            existing.setRingValue(ringValue);
            existing.setRingRate(ringRate);
            existing.setDataDetail(dataDetail);
            kpiDailyMapper.updateById(existing);
            return existing;
        } else {
            KpiDaily daily = new KpiDaily();
            daily.setKpiCode(kpiCode);
            daily.setKpiName(define.getKpiName());
            daily.setKpiCategory(define.getKpiCategory());
            daily.setStatDate(statDate);
            daily.setWarehouseCode(warehouseCode);
            daily.setDepartment(department);
            daily.setEmployeeId(employeeId);
            daily.setActualValue(actualValue);
            daily.setTargetValue(targetValue);
            daily.setTargetRate(targetRate);
            daily.setCompareValue(compareValue);
            daily.setCompareRate(compareRate);
            daily.setRingValue(ringValue);
            daily.setRingRate(ringRate);
            daily.setDataDetail(dataDetail);
            kpiDailyMapper.insert(daily);
            return daily;
        }
    }

    public List<KpiDaily> getKpiHistory(String kpiCode, LocalDate start, LocalDate end) {
        return kpiDailyMapper.selectByKpiAndDateRange(kpiCode, start, end);
    }

    public List<KpiDaily> getKpiByDateAndWarehouse(LocalDate date, String warehouseCode) {
        return kpiDailyMapper.selectByDateAndWarehouse(date, warehouseCode);
    }

    // ============================================================
    // 3. KPI目标管理
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public KpiTarget createKpiTarget(KpiTarget target) {
        target.setTargetNo(generateTargetNo());
        target.setStatus("ACTIVE");
        kpiTargetMapper.insert(target);
        log.info(
                "创建KPI目标: {}={}, 周期={}{}",
                target.getKpiCode(),
                target.getTargetValue(),
                target.getTargetPeriod(),
                target.getPeriodValue());
        return target;
    }

    public Page<KpiTarget> pageKpiTargets(
            Page<KpiTarget> page,
            String kpiCode,
            String targetPeriod,
            String periodValue,
            String status) {
        LambdaQueryWrapper<KpiTarget> wrapper = new LambdaQueryWrapper<>();
        if (kpiCode != null) wrapper.eq(KpiTarget::getKpiCode, kpiCode);
        if (targetPeriod != null) wrapper.eq(KpiTarget::getTargetPeriod, targetPeriod);
        if (periodValue != null) wrapper.eq(KpiTarget::getPeriodValue, periodValue);
        if (status != null) wrapper.eq(KpiTarget::getStatus, status);
        wrapper.orderByDesc(KpiTarget::getCreatedTime);
        return kpiTargetMapper.selectPage(page, wrapper);
    }

    @Transactional(rollbackFor = Exception.class)
    public KpiTarget expireTarget(Long targetId) {
        KpiTarget target = kpiTargetMapper.selectById(targetId);
        if (target == null) throw new RuntimeException("目标不存在");
        target.setStatus("EXPIRED");
        kpiTargetMapper.updateById(target);
        return target;
    }

    // ============================================================
    // 4. KPI报表
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public KpiReport createKpiReport(KpiReport report) {
        kpiReportMapper.insert(report);
        log.info("创建KPI报表: {}={}", report.getReportCode(), report.getReportName());
        return report;
    }

    public List<KpiReport> getAllEnabledReports() {
        return kpiReportMapper.selectAllEnabled();
    }

    /** 生成报表数据 */
    public Map<String, Object> generateReportData(
            String reportCode, LocalDate startDate, LocalDate endDate) {
        KpiReport report =
                kpiReportMapper.selectOne(
                        new LambdaQueryWrapper<KpiReport>()
                                .eq(KpiReport::getReportCode, reportCode));
        if (report == null) throw new RuntimeException("报表不存在: " + reportCode);

        Map<String, Object> result = new HashMap<>();
        result.put("reportCode", reportCode);
        result.put("reportName", report.getReportName());
        result.put("startDate", startDate);
        result.put("endDate", endDate);

        // 解析KPI编码
        String[] kpiCodes =
                report.getKpiCodes() != null
                        ? report.getKpiCodes()
                                .replace("[", "")
                                .replace("]", "")
                                .replace("\"", "")
                                .split(",")
                        : new String[0];

        List<Map<String, Object>> kpiDataList = new ArrayList<>();
        for (String kpiCode : kpiCodes) {
            if (kpiCode == null || kpiCode.trim().isEmpty()) continue;
            List<KpiDaily> dailyList =
                    kpiDailyMapper.selectByKpiAndDateRange(kpiCode.trim(), startDate, endDate);
            Map<String, Object> kpiData = new HashMap<>();
            kpiData.put("kpiCode", kpiCode.trim());
            kpiData.put("data", dailyList);
            kpiDataList.add(kpiData);
        }
        result.put("kpiData", kpiDataList);

        // 更新最后生成时间
        report.setLastGenerateTime(LocalDateTime.now());
        kpiReportMapper.updateById(report);

        return result;
    }

    // ============================================================
    // 工具方法
    // ============================================================

    private BigDecimal getTargetValue(
            String kpiCode, LocalDate date, String warehouseCode, String department) {
        String month = date.format(DateTimeFormatter.ofPattern("yyyyMM"));
        List<KpiTarget> targets = kpiTargetMapper.selectActiveTargets(kpiCode, "MONTHLY", month);
        if (targets != null && !targets.isEmpty()) {
            for (KpiTarget t : targets) {
                if (warehouseCode != null && warehouseCode.equals(t.getWarehouseCode()))
                    return t.getTargetValue();
                if (department != null && department.equals(t.getDepartment()))
                    return t.getTargetValue();
                if (t.getWarehouseCode() == null && t.getDepartment() == null)
                    return t.getTargetValue();
            }
        }
        return null;
    }

    private BigDecimal getHistoryValue(
            String kpiCode,
            LocalDate date,
            String warehouseCode,
            String department,
            String employeeId) {
        KpiDaily history =
                kpiDailyMapper.selectOne(
                        new LambdaQueryWrapper<KpiDaily>()
                                .eq(KpiDaily::getKpiCode, kpiCode)
                                .eq(KpiDaily::getStatDate, date)
                                .eq(KpiDaily::getWarehouseCode, warehouseCode)
                                .eq(KpiDaily::getDepartment, department)
                                .eq(KpiDaily::getEmployeeId, employeeId != null ? employeeId : ""));
        return history != null ? history.getActualValue() : null;
    }

    private BigDecimal calcGrowthRate(BigDecimal current, BigDecimal previous) {
        if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) return null;
        return current.subtract(previous)
                .divide(previous, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
    }

    private String generateTargetNo() {
        return "TGT"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", TARGET_SEQ.incrementAndGet() % 1000);
    }
}
