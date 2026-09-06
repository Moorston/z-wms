package com.xwms.analytics.kpi.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.analytics.kpi.entity.*;
import com.xwms.analytics.kpi.service.KpiService;
import com.xwms.common.core.Result;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/** KPI报表体系 Controller */
@Tag(name = "KPI报表体系", description = "KPI指标定义/数据采集/目标管理/报表生成")
@RestController
@RequestMapping("/api/kpi")
@RequiredArgsConstructor
public class KpiController {

    private final KpiService kpiService;

    // ============================================================
    // KPI指标定义
    // ============================================================

    @Operation(summary = "创建KPI指标")
    @PostMapping("/define")
    public Result<KpiDefine> createKpiDefine(@RequestBody KpiDefine define) {
        return Result.success(kpiService.createKpiDefine(define));
    }

    @Operation(summary = "更新KPI指标")
    @PutMapping("/define")
    public Result<KpiDefine> updateKpiDefine(@RequestBody KpiDefine define) {
        return Result.success(kpiService.updateKpiDefine(define));
    }

    @Operation(summary = "分页查询KPI指标")
    @GetMapping("/define")
    public Result<Page<KpiDefine>> pageKpiDefines(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer enabled) {
        return Result.success(kpiService.pageKpiDefines(new Page<>(page, size), category, enabled));
    }

    @Operation(summary = "按分类查询KPI指标")
    @GetMapping("/define/category/{category}")
    public Result<List<KpiDefine>> getKpiDefinesByCategory(@PathVariable String category) {
        return Result.success(kpiService.getKpiDefinesByCategory(category));
    }

    @Operation(summary = "查询所有启用的KPI指标")
    @GetMapping("/define/enabled")
    public Result<List<KpiDefine>> getAllEnabledKpiDefines() {
        return Result.success(kpiService.getAllEnabledKpiDefines());
    }

    // ============================================================
    // KPI数据采集
    // ============================================================

    @Operation(summary = "采集KPI数据")
    @PostMapping("/collect")
    public Result<KpiDaily> collectKpiData(
            @RequestParam String kpiCode,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate statDate,
            @RequestParam(required = false) String warehouseCode,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String employeeId,
            @RequestParam BigDecimal actualValue,
            @RequestParam(required = false) String dataDetail) {
        return Result.success(
                kpiService.collectKpiData(
                        kpiCode,
                        statDate,
                        warehouseCode,
                        department,
                        employeeId,
                        actualValue,
                        dataDetail));
    }

    @Operation(summary = "查询KPI历史数据")
    @GetMapping("/history")
    public Result<List<KpiDaily>> getKpiHistory(
            @RequestParam String kpiCode,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return Result.success(kpiService.getKpiHistory(kpiCode, start, end));
    }

    @Operation(summary = "按日期和仓库查询KPI")
    @GetMapping("/daily")
    public Result<List<KpiDaily>> getKpiByDateAndWarehouse(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam String warehouseCode) {
        return Result.success(kpiService.getKpiByDateAndWarehouse(date, warehouseCode));
    }

    // ============================================================
    // KPI目标管理
    // ============================================================

    @Operation(summary = "创建KPI目标")
    @PostMapping("/target")
    public Result<KpiTarget> createKpiTarget(@RequestBody KpiTarget target) {
        return Result.success(kpiService.createKpiTarget(target));
    }

    @Operation(summary = "分页查询KPI目标")
    @GetMapping("/target")
    public Result<Page<KpiTarget>> pageKpiTargets(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String kpiCode,
            @RequestParam(required = false) String targetPeriod,
            @RequestParam(required = false) String periodValue,
            @RequestParam(required = false) String status) {
        return Result.success(
                kpiService.pageKpiTargets(
                        new Page<>(page, size), kpiCode, targetPeriod, periodValue, status));
    }

    @Operation(summary = "过期KPI目标")
    @PutMapping("/target/{id}/expire")
    public Result<KpiTarget> expireTarget(@PathVariable Long id) {
        return Result.success(kpiService.expireTarget(id));
    }

    // ============================================================
    // KPI报表
    // ============================================================

    @Operation(summary = "创建KPI报表")
    @PostMapping("/report")
    public Result<KpiReport> createKpiReport(@RequestBody KpiReport report) {
        return Result.success(kpiService.createKpiReport(report));
    }

    @Operation(summary = "查询所有启用的报表")
    @GetMapping("/report/enabled")
    public Result<List<KpiReport>> getAllEnabledReports() {
        return Result.success(kpiService.getAllEnabledReports());
    }

    @Operation(summary = "生成报表数据")
    @GetMapping("/report/generate")
    public Result<Map<String, Object>> generateReportData(
            @RequestParam String reportCode,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return Result.success(kpiService.generateReportData(reportCode, startDate, endDate));
    }
}
