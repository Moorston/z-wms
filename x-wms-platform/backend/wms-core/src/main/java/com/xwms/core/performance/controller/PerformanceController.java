package com.xwms.core.performance.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.common.core.Result;
import com.xwms.core.performance.entity.*;
import com.xwms.core.performance.service.PerformanceService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/** 人员绩效 Controller */
@Tag(name = "人员绩效", description = "人员档案/作业记录/绩效日报/绩效考核")
@RestController
@RequestMapping("/api/performance")
@RequiredArgsConstructor
public class PerformanceController {

    private final PerformanceService performanceService;

    // ============================================================

    // 人员档案
    // ============================================================

    @Operation(summary = "创建人员")
    @PostMapping("/employees")
    public Result<Employee> createEmployee(@RequestBody Employee employee) {
        return Result.success(performanceService.createEmployee(employee));
    }

    @Operation(summary = "查询人员详情")
    @GetMapping("/employees/{id}")
    public Result<Employee> getEmployee(@PathVariable Long id) {
        return Result.success(performanceService.getEmployee(id));
    }

    @Operation(summary = "分页查询人员")
    @GetMapping("/employees")
    public Result<Page<Employee>> pageEmployees(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String warehouse,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String status) {
        return Result.success(
                performanceService.pageEmployees(
                        new Page<>(page, size), warehouse, department, status));
    }

    @Operation(summary = "查询在职人员")
    @GetMapping("/employees/active")
    public Result<List<Employee>> getActiveEmployees(@RequestParam String warehouse) {
        return Result.success(performanceService.getActiveEmployees(warehouse));
    }

    // ============================================================

    // 作业记录
    // ============================================================

    @Operation(summary = "记录作业")
    @PostMapping("/work-records")
    public Result<WorkRecord> createWorkRecord(@RequestBody WorkRecord record) {
        return Result.success(performanceService.createWorkRecord(record));
    }

    @Operation(summary = "查询人员作业记录")
    @GetMapping("/work-records")
    public Result<List<WorkRecord>> getWorkRecords(
            @RequestParam Long employeeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return Result.success(performanceService.getWorkRecords(employeeId, date));
    }

    // ============================================================

    // 绩效日报
    // ============================================================

    @Operation(summary = "计算绩效日报")
    @PostMapping("/daily/calculate")
    public Result<PerformanceDaily> calculateDaily(
            @RequestParam Long employeeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return Result.success(performanceService.calculateDailyPerformance(employeeId, date));
    }

    @Operation(summary = "查询绩效日报")
    @GetMapping("/daily")
    public Result<PerformanceDaily> getDaily(
            @RequestParam Long employeeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return Result.success(performanceService.getDailyPerformance(employeeId, date));
    }

    @Operation(summary = "绩效日报排名")
    @GetMapping("/daily/ranking")
    public Result<List<PerformanceDaily>> getDailyRanking(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam String warehouse) {
        return Result.success(performanceService.getDailyRanking(date, warehouse));
    }

    // ============================================================

    // 绩效考核
    // ============================================================

    @Operation(summary = "生成绩效考核")
    @PostMapping("/assess/generate")
    public Result<PerformanceAssess> generateAssess(
            @RequestParam Long employeeId, @RequestParam String period, @RequestParam String type) {
        return Result.success(performanceService.generateAssess(employeeId, period, type));
    }

    @Operation(summary = "确认考核")
    @PutMapping("/assess/{id}/confirm")
    public Result<PerformanceAssess> confirmAssess(
            @PathVariable Long id, @RequestParam String assessBy) {
        return Result.success(performanceService.confirmAssess(id, assessBy));
    }

    @Operation(summary = "发布考核")
    @PutMapping("/assess/{id}/publish")
    public Result<PerformanceAssess> publishAssess(@PathVariable Long id) {
        return Result.success(performanceService.publishAssess(id));
    }

    @Operation(summary = "查询人员考核记录")
    @GetMapping("/assess/employee/{employeeId}")
    public Result<List<PerformanceAssess>> getAssessRecords(@PathVariable Long employeeId) {
        return Result.success(performanceService.getAssessRecords(employeeId));
    }

    @Operation(summary = "考核排名")
    @GetMapping("/assess/ranking")
    public Result<List<PerformanceAssess>> getAssessRanking(
            @RequestParam String period, @RequestParam String warehouse) {
        return Result.success(performanceService.getAssessRanking(period, warehouse));
    }
}
