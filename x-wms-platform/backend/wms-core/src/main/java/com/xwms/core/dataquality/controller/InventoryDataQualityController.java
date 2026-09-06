package com.xwms.core.dataquality.controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.common.core.Result;
import com.xwms.core.dataquality.entity.*;
import com.xwms.core.dataquality.service.InventoryDataQualityService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "库存数据质量管理", description = "数据质量规则/检查/报告/整改")
@RestController
@RequestMapping("/api/dataquality")
@RequiredArgsConstructor
public class InventoryDataQualityController {

    private final InventoryDataQualityService dataQualityService;

    // ==================== 数据质量规则 ====================

    @Operation(summary = "创建数据质量规则")
    @PostMapping("/rule")
    public Result<DqRule> createRule(
            @RequestParam String ruleName,
            @RequestParam String ruleCode,
            @RequestParam String ruleType,
            @RequestParam(required = false) String ruleCategory,
            @RequestParam(required = false) String tableName,
            @RequestParam(required = false) String fieldName,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String ruleExpression,
            @RequestParam(required = false) String ruleConfig,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) BigDecimal threshold,
            @RequestParam(required = false) Integer sortOrder,
            @RequestParam String createdBy) {
        return Result.success(
                dataQualityService.createRule(
                        ruleName,
                        ruleCode,
                        ruleType,
                        ruleCategory,
                        tableName,
                        fieldName,
                        description,
                        ruleExpression,
                        ruleConfig,
                        severity,
                        threshold,
                        sortOrder,
                        createdBy));
    }

    @Operation(summary = "按类型获取活跃规则")
    @GetMapping("/rule/type/{ruleType}")
    public Result<List<DqRule>> getActiveRulesByType(@PathVariable String ruleType) {
        return Result.success(dataQualityService.getActiveRulesByType(ruleType));
    }

    @Operation(summary = "按表获取活跃规则")
    @GetMapping("/rule/table/{tableName}")
    public Result<List<DqRule>> getActiveRulesByTable(@PathVariable String tableName) {
        return Result.success(dataQualityService.getActiveRulesByTable(tableName));
    }

    @Operation(summary = "获取所有活跃规则")
    @GetMapping("/rule/all-active")
    public Result<List<DqRule>> getAllActiveRules() {
        return Result.success(dataQualityService.getAllActiveRules());
    }

    @Operation(summary = "分页查询数据质量规则")
    @GetMapping("/rule/list")
    public Result<Page<DqRule>> pageRule(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String ruleType,
            @RequestParam(required = false) String ruleCategory,
            @RequestParam(required = false) String tableName,
            @RequestParam(required = false) String isActive) {
        return Result.success(
                dataQualityService.pageRule(
                        new Page<>(page, size), ruleType, ruleCategory, tableName, isActive));
    }

    // ==================== 数据质量检查 ====================

    @Operation(summary = "创建数据质量检查")
    @PostMapping("/check")
    public Result<DqCheck> createCheck(
            @RequestParam String checkName,
            @RequestParam String ruleId,
            @RequestParam(required = false) String ruleCode,
            @RequestParam(required = false) String ruleName,
            @RequestParam String warehouseCode,
            @RequestParam(required = false) String ownerCode,
            @RequestParam(required = false) String checkType,
            @RequestParam(required = false) String checkConfig,
            @RequestParam String operator) {
        return Result.success(
                dataQualityService.createCheck(
                        checkName,
                        ruleId,
                        ruleCode,
                        ruleName,
                        warehouseCode,
                        ownerCode,
                        checkType,
                        checkConfig,
                        operator));
    }

    @Operation(summary = "执行数据质量检查")
    @PostMapping("/check/{checkId}/execute")
    public Result<DqCheck> executeCheck(
            @PathVariable String checkId,
            @RequestParam Long totalCount,
            @RequestParam Long passCount,
            @RequestParam Long failCount,
            @RequestParam(required = false) String checkResult,
            @RequestParam(required = false) String failDetails,
            @RequestParam String operator) {
        return Result.success(
                dataQualityService.executeCheck(
                        checkId,
                        totalCount,
                        passCount,
                        failCount,
                        checkResult,
                        failDetails,
                        operator));
    }

    @Operation(summary = "数据质量检查失败")
    @PostMapping("/check/{checkId}/fail")
    public Result<DqCheck> failCheck(
            @PathVariable String checkId,
            @RequestParam String errorMessage,
            @RequestParam String operator) {
        return Result.success(dataQualityService.failCheck(checkId, errorMessage, operator));
    }

    @Operation(summary = "按规则获取最近检查")
    @GetMapping("/check/recent/{ruleId}")
    public Result<List<DqCheck>> getRecentChecksByRule(
            @PathVariable String ruleId, @RequestParam(defaultValue = "10") int limit) {
        return Result.success(dataQualityService.getRecentChecksByRule(ruleId, limit));
    }

    @Operation(summary = "按仓库和时间获取检查")
    @GetMapping("/check/warehouse-time")
    public Result<List<DqCheck>> getChecksByWarehouseAndTime(
            @RequestParam String warehouseCode,
            @RequestParam LocalDateTime startTime,
            @RequestParam LocalDateTime endTime) {
        return Result.success(
                dataQualityService.getChecksByWarehouseAndTime(warehouseCode, startTime, endTime));
    }

    @Operation(summary = "分页查询数据质量检查")
    @GetMapping("/check/list")
    public Result<Page<DqCheck>> pageCheck(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String ruleId,
            @RequestParam(required = false) String warehouseCode,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String checkType) {
        return Result.success(
                dataQualityService.pageCheck(
                        new Page<>(page, size), ruleId, warehouseCode, status, checkType));
    }

    // ==================== 数据质量报告 ====================

    @Operation(summary = "创建数据质量报告")
    @PostMapping("/report")
    public Result<DqReport> createReport(
            @RequestParam String reportName,
            @RequestParam String reportType,
            @RequestParam String warehouseCode,
            @RequestParam(required = false) String ownerCode,
            @RequestParam(required = false) String periodType,
            @RequestParam LocalDateTime periodStart,
            @RequestParam LocalDateTime periodEnd,
            @RequestParam(required = false) Integer totalRules,
            @RequestParam(required = false) Integer executedRules,
            @RequestParam(required = false) Integer passRules,
            @RequestParam(required = false) Integer failRules,
            @RequestParam(required = false) BigDecimal overallScore,
            @RequestParam(required = false) String overallGrade,
            @RequestParam(required = false) String reportContent,
            @RequestParam(required = false) String reportSummary,
            @RequestParam String operator) {
        return Result.success(
                dataQualityService.createReport(
                        reportName,
                        reportType,
                        warehouseCode,
                        ownerCode,
                        periodType,
                        periodStart,
                        periodEnd,
                        totalRules,
                        executedRules,
                        passRules,
                        failRules,
                        overallScore,
                        overallGrade,
                        reportContent,
                        reportSummary,
                        operator));
    }

    @Operation(summary = "生成数据质量报告")
    @PostMapping("/report/{reportId}/generate")
    public Result<DqReport> generateReport(
            @PathVariable String reportId, @RequestParam String operator) {
        return Result.success(dataQualityService.generateReport(reportId, operator));
    }

    @Operation(summary = "按仓库和期间获取报告")
    @GetMapping("/report/warehouse-period")
    public Result<List<DqReport>> getReportsByWarehouseAndPeriod(
            @RequestParam String warehouseCode,
            @RequestParam LocalDateTime startTime,
            @RequestParam LocalDateTime endTime) {
        return Result.success(
                dataQualityService.getReportsByWarehouseAndPeriod(
                        warehouseCode, startTime, endTime));
    }

    @Operation(summary = "按类型和状态获取报告")
    @GetMapping("/report/type-status")
    public Result<List<DqReport>> getReportsByTypeAndStatus(
            @RequestParam String reportType, @RequestParam String status) {
        return Result.success(dataQualityService.getReportsByTypeAndStatus(reportType, status));
    }

    @Operation(summary = "按仓库获取最近报告")
    @GetMapping("/report/recent/{warehouseCode}")
    public Result<List<DqReport>> getRecentReportsByWarehouse(
            @PathVariable String warehouseCode, @RequestParam(defaultValue = "10") int limit) {
        return Result.success(dataQualityService.getRecentReportsByWarehouse(warehouseCode, limit));
    }

    @Operation(summary = "分页查询数据质量报告")
    @GetMapping("/report/list")
    public Result<Page<DqReport>> pageReport(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String reportType,
            @RequestParam(required = false) String warehouseCode,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String periodType) {
        return Result.success(
                dataQualityService.pageReport(
                        new Page<>(page, size), reportType, warehouseCode, status, periodType));
    }

    // ==================== 数据质量整改 ====================

    @Operation(summary = "创建数据质量问题")
    @PostMapping("/issue")
    public Result<DqIssue> createIssue(
            @RequestParam(required = false) String checkId,
            @RequestParam String ruleId,
            @RequestParam(required = false) String ruleCode,
            @RequestParam(required = false) String ruleName,
            @RequestParam String warehouseCode,
            @RequestParam(required = false) String ownerCode,
            @RequestParam(required = false) String tableName,
            @RequestParam(required = false) String fieldName,
            @RequestParam(required = false) String issueType,
            @RequestParam(required = false) String issueDescription,
            @RequestParam(required = false) String issueData,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String assignee,
            @RequestParam(required = false) LocalDateTime dueTime,
            @RequestParam String operator) {
        return Result.success(
                dataQualityService.createIssue(
                        checkId,
                        ruleId,
                        ruleCode,
                        ruleName,
                        warehouseCode,
                        ownerCode,
                        tableName,
                        fieldName,
                        issueType,
                        issueDescription,
                        issueData,
                        severity,
                        priority,
                        assignee,
                        dueTime,
                        operator));
    }

    @Operation(summary = "分配数据质量问题")
    @PostMapping("/issue/{issueId}/assign")
    public Result<DqIssue> assignIssue(
            @PathVariable String issueId,
            @RequestParam String assignee,
            @RequestParam String operator) {
        return Result.success(dataQualityService.assignIssue(issueId, assignee, operator));
    }

    @Operation(summary = "修复数据质量问题")
    @PostMapping("/issue/{issueId}/fix")
    public Result<DqIssue> fixIssue(
            @PathVariable String issueId,
            @RequestParam(required = false) String fixPlan,
            @RequestParam(required = false) String fixResult,
            @RequestParam String operator) {
        return Result.success(dataQualityService.fixIssue(issueId, fixPlan, fixResult, operator));
    }

    @Operation(summary = "验证数据质量问题")
    @PostMapping("/issue/{issueId}/verify")
    public Result<DqIssue> verifyIssue(
            @PathVariable String issueId,
            @RequestParam String verifier,
            @RequestParam boolean passed) {
        return Result.success(dataQualityService.verifyIssue(issueId, verifier, passed));
    }

    @Operation(summary = "关闭数据质量问题")
    @PostMapping("/issue/{issueId}/close")
    public Result<DqIssue> closeIssue(@PathVariable String issueId, @RequestParam String operator) {
        return Result.success(dataQualityService.closeIssue(issueId, operator));
    }

    @Operation(summary = "按检查ID获取问题")
    @GetMapping("/issue/check/{checkId}")
    public Result<List<DqIssue>> getIssuesByCheckId(@PathVariable String checkId) {
        return Result.success(dataQualityService.getIssuesByCheckId(checkId));
    }

    @Operation(summary = "按仓库和状态获取问题")
    @GetMapping("/issue/warehouse-status")
    public Result<List<DqIssue>> getIssuesByWarehouseAndStatus(
            @RequestParam String warehouseCode, @RequestParam String status) {
        return Result.success(
                dataQualityService.getIssuesByWarehouseAndStatus(warehouseCode, status));
    }

    @Operation(summary = "按处理人获取未关闭问题")
    @GetMapping("/issue/assignee/{assignee}")
    public Result<List<DqIssue>> getOpenIssuesByAssignee(@PathVariable String assignee) {
        return Result.success(dataQualityService.getOpenIssuesByAssignee(assignee));
    }

    @Operation(summary = "按严重程度获取未关闭问题")
    @GetMapping("/issue/severity/{severity}")
    public Result<List<DqIssue>> getOpenIssuesBySeverity(@PathVariable String severity) {
        return Result.success(dataQualityService.getOpenIssuesBySeverity(severity));
    }

    @Operation(summary = "分页查询数据质量问题")
    @GetMapping("/issue/list")
    public Result<Page<DqIssue>> pageIssue(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String checkId,
            @RequestParam(required = false) String warehouseCode,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String assignee) {
        return Result.success(
                dataQualityService.pageIssue(
                        new Page<>(page, size),
                        checkId,
                        warehouseCode,
                        status,
                        severity,
                        assignee));
    }
}
