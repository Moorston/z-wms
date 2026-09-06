package com.xwms.core.dataquality.service;

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

import com.xwms.core.dataquality.entity.*;
import com.xwms.core.dataquality.mapper.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryDataQualityService {

    private final DqRuleMapper ruleMapper;
    private final DqCheckMapper checkMapper;
    private final DqReportMapper reportMapper;
    private final DqIssueMapper issueMapper;

    private static final AtomicInteger SEQ = new AtomicInteger(0);
    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    // ==================== 数据质量规则 ====================

    @Transactional(rollbackFor = Exception.class)
    public DqRule createRule(
            String ruleName,
            String ruleCode,
            String ruleType,
            String ruleCategory,
            String tableName,
            String fieldName,
            String description,
            String ruleExpression,
            String ruleConfig,
            String severity,
            BigDecimal threshold,
            Integer sortOrder,
            String createdBy) {
        DqRule rule = new DqRule();
        rule.setRuleId(generateId("DR"));
        rule.setRuleName(ruleName);
        rule.setRuleCode(ruleCode);
        rule.setRuleType(ruleType);
        rule.setRuleCategory(ruleCategory);
        rule.setTableName(tableName);
        rule.setFieldName(fieldName);
        rule.setDescription(description);
        rule.setRuleExpression(ruleExpression);
        rule.setRuleConfig(ruleConfig);
        rule.setSeverity(severity != null ? severity : "WARNING");
        rule.setThreshold(threshold);
        rule.setIsActive("Y");
        rule.setSortOrder(sortOrder != null ? sortOrder : 100);
        rule.setCreatedBy(createdBy);
        ruleMapper.insert(rule);
        log.info("创建数据质量规则: id={}, name={}, type={}", rule.getRuleId(), ruleName, ruleType);
        return rule;
    }

    public List<DqRule> getActiveRulesByType(String ruleType) {
        return ruleMapper.selectActiveByType(ruleType);
    }

    public List<DqRule> getActiveRulesByTable(String tableName) {
        return ruleMapper.selectActiveByTable(tableName);
    }

    public List<DqRule> getAllActiveRules() {
        return ruleMapper.selectAllActive();
    }

    public Page<DqRule> pageRule(
            Page<DqRule> page,
            String ruleType,
            String ruleCategory,
            String tableName,
            String isActive) {
        LambdaQueryWrapper<DqRule> wrapper = new LambdaQueryWrapper<>();
        if (ruleType != null) wrapper.eq(DqRule::getRuleType, ruleType);
        if (ruleCategory != null) wrapper.eq(DqRule::getRuleCategory, ruleCategory);
        if (tableName != null) wrapper.eq(DqRule::getTableName, tableName);
        if (isActive != null) wrapper.eq(DqRule::getIsActive, isActive);
        wrapper.orderByAsc(DqRule::getRuleType).orderByAsc(DqRule::getSortOrder);
        return ruleMapper.selectPage(page, wrapper);
    }

    // ==================== 数据质量检查 ====================

    @Transactional(rollbackFor = Exception.class)
    public DqCheck createCheck(
            String checkName,
            String ruleId,
            String ruleCode,
            String ruleName,
            String warehouseCode,
            String ownerCode,
            String checkType,
            String checkConfig,
            String operator) {
        DqCheck check = new DqCheck();
        check.setCheckId(generateId("DC"));
        check.setCheckName(checkName);
        check.setRuleId(ruleId);
        check.setRuleCode(ruleCode);
        check.setRuleName(ruleName);
        check.setWarehouseCode(warehouseCode);
        check.setOwnerCode(ownerCode);
        check.setCheckType(checkType != null ? checkType : "SCHEDULED");
        check.setCheckConfig(checkConfig);
        check.setStatus("PENDING");
        check.setOperator(operator);
        checkMapper.insert(check);
        log.info("创建数据质量检查: id={}, name={}, rule={}", check.getCheckId(), checkName, ruleCode);
        return check;
    }

    @Transactional(rollbackFor = Exception.class)
    public DqCheck executeCheck(
            String checkId,
            Long totalCount,
            Long passCount,
            Long failCount,
            String checkResult,
            String failDetails,
            String operator) {
        DqCheck check = checkMapper.selectByCheckId(checkId);
        if (check == null) throw new RuntimeException("数据质量检查不存在: " + checkId);
        check.setStatus("RUNNING");
        check.setCheckStartTime(LocalDateTime.now());
        checkMapper.updateById(check);
        // 执行检查
        check.setTotalCount(totalCount);
        check.setPassCount(passCount);
        check.setFailCount(failCount);
        // 计算通过率和失败率
        if (totalCount != null && totalCount > 0) {
            check.setPassRate(
                    BigDecimal.valueOf(passCount)
                            .divide(BigDecimal.valueOf(totalCount), 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100)));
            check.setFailRate(
                    BigDecimal.valueOf(failCount)
                            .divide(BigDecimal.valueOf(totalCount), 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100)));
        }
        check.setCheckResult(checkResult);
        check.setFailDetails(failDetails);
        check.setStatus("COMPLETED");
        check.setCheckEndTime(LocalDateTime.now());
        if (check.getCheckStartTime() != null) {
            check.setDurationMs(
                    java.time.Duration.between(check.getCheckStartTime(), check.getCheckEndTime())
                            .toMillis());
        }
        check.setOperator(operator);
        checkMapper.updateById(check);
        log.info(
                "执行数据质量检查: id={}, total={}, pass={}, fail={}, passRate={}%",
                checkId, totalCount, passCount, failCount, check.getPassRate());
        return checkMapper.selectByCheckId(checkId);
    }

    @Transactional(rollbackFor = Exception.class)
    public DqCheck failCheck(String checkId, String errorMessage, String operator) {
        DqCheck check = checkMapper.selectByCheckId(checkId);
        if (check == null) throw new RuntimeException("数据质量检查不存在: " + checkId);
        check.setStatus("FAILED");
        check.setErrorMessage(errorMessage);
        check.setCheckEndTime(LocalDateTime.now());
        check.setOperator(operator);
        checkMapper.updateById(check);
        log.error("数据质量检查失败: id={}, error={}", checkId, errorMessage);
        return checkMapper.selectByCheckId(checkId);
    }

    public List<DqCheck> getRecentChecksByRule(String ruleId, int limit) {
        return checkMapper.selectRecentByRule(ruleId, limit);
    }

    public List<DqCheck> getChecksByWarehouseAndTime(
            String warehouseCode, LocalDateTime startTime, LocalDateTime endTime) {
        return checkMapper.selectByWarehouseAndTime(warehouseCode, startTime, endTime);
    }

    public Page<DqCheck> pageCheck(
            Page<DqCheck> page,
            String ruleId,
            String warehouseCode,
            String status,
            String checkType) {
        LambdaQueryWrapper<DqCheck> wrapper = new LambdaQueryWrapper<>();
        if (ruleId != null) wrapper.eq(DqCheck::getRuleId, ruleId);
        if (warehouseCode != null) wrapper.eq(DqCheck::getWarehouseCode, warehouseCode);
        if (status != null) wrapper.eq(DqCheck::getStatus, status);
        if (checkType != null) wrapper.eq(DqCheck::getCheckType, checkType);
        wrapper.orderByDesc(DqCheck::getCheckStartTime);
        return checkMapper.selectPage(page, wrapper);
    }

    // ==================== 数据质量报告 ====================

    @Transactional(rollbackFor = Exception.class)
    public DqReport createReport(
            String reportName,
            String reportType,
            String warehouseCode,
            String ownerCode,
            String periodType,
            LocalDateTime periodStart,
            LocalDateTime periodEnd,
            Integer totalRules,
            Integer executedRules,
            Integer passRules,
            Integer failRules,
            BigDecimal overallScore,
            String overallGrade,
            String reportContent,
            String reportSummary,
            String operator) {
        DqReport report = new DqReport();
        report.setReportId(generateId("DRP"));
        report.setReportName(reportName);
        report.setReportType(reportType);
        report.setWarehouseCode(warehouseCode);
        report.setOwnerCode(ownerCode);
        report.setPeriodType(periodType != null ? periodType : "DAILY");
        report.setPeriodStart(periodStart);
        report.setPeriodEnd(periodEnd);
        report.setTotalRules(totalRules);
        report.setExecutedRules(executedRules);
        report.setPassRules(passRules);
        report.setFailRules(failRules);
        report.setOverallScore(overallScore);
        report.setOverallGrade(overallGrade);
        report.setReportContent(reportContent);
        report.setReportSummary(reportSummary);
        report.setStatus("DRAFT");
        report.setOperator(operator);
        reportMapper.insert(report);
        log.info("创建数据质量报告: id={}, name={}, type={}", report.getReportId(), reportName, reportType);
        return report;
    }

    @Transactional(rollbackFor = Exception.class)
    public DqReport generateReport(String reportId, String operator) {
        DqReport report = reportMapper.selectByReportId(reportId);
        if (report == null) throw new RuntimeException("数据质量报告不存在: " + reportId);
        report.setStatus("GENERATING");
        reportMapper.updateById(report);
        // 生成报告
        report.setStatus("COMPLETED");
        report.setGeneratedTime(LocalDateTime.now());
        report.setOperator(operator);
        reportMapper.updateById(report);
        log.info("生成数据质量报告: id={}", reportId);
        return reportMapper.selectByReportId(reportId);
    }

    public List<DqReport> getReportsByWarehouseAndPeriod(
            String warehouseCode, LocalDateTime startTime, LocalDateTime endTime) {
        return reportMapper.selectByWarehouseAndPeriod(warehouseCode, startTime, endTime);
    }

    public List<DqReport> getReportsByTypeAndStatus(String reportType, String status) {
        return reportMapper.selectByTypeAndStatus(reportType, status);
    }

    public List<DqReport> getRecentReportsByWarehouse(String warehouseCode, int limit) {
        return reportMapper.selectRecentByWarehouse(warehouseCode, limit);
    }

    public Page<DqReport> pageReport(
            Page<DqReport> page,
            String reportType,
            String warehouseCode,
            String status,
            String periodType) {
        LambdaQueryWrapper<DqReport> wrapper = new LambdaQueryWrapper<>();
        if (reportType != null) wrapper.eq(DqReport::getReportType, reportType);
        if (warehouseCode != null) wrapper.eq(DqReport::getWarehouseCode, warehouseCode);
        if (status != null) wrapper.eq(DqReport::getStatus, status);
        if (periodType != null) wrapper.eq(DqReport::getPeriodType, periodType);
        wrapper.orderByDesc(DqReport::getGeneratedTime);
        return reportMapper.selectPage(page, wrapper);
    }

    // ==================== 数据质量整改 ====================

    @Transactional(rollbackFor = Exception.class)
    public DqIssue createIssue(
            String checkId,
            String ruleId,
            String ruleCode,
            String ruleName,
            String warehouseCode,
            String ownerCode,
            String tableName,
            String fieldName,
            String issueType,
            String issueDescription,
            String issueData,
            String severity,
            String priority,
            String assignee,
            LocalDateTime dueTime,
            String operator) {
        DqIssue issue = new DqIssue();
        issue.setIssueId(generateId("DI"));
        issue.setCheckId(checkId);
        issue.setRuleId(ruleId);
        issue.setRuleCode(ruleCode);
        issue.setRuleName(ruleName);
        issue.setWarehouseCode(warehouseCode);
        issue.setOwnerCode(ownerCode);
        issue.setTableName(tableName);
        issue.setFieldName(fieldName);
        issue.setIssueType(issueType);
        issue.setIssueDescription(issueDescription);
        issue.setIssueData(issueData);
        issue.setSeverity(severity != null ? severity : "WARNING");
        issue.setStatus("OPEN");
        issue.setPriority(priority != null ? priority : "NORMAL");
        issue.setAssignee(assignee);
        issue.setReopenCount(0);
        issue.setDueTime(dueTime);
        issue.setOperator(operator);
        issueMapper.insert(issue);
        log.info("创建数据质量问题: id={}, rule={}, severity={}", issue.getIssueId(), ruleCode, severity);
        return issue;
    }

    @Transactional(rollbackFor = Exception.class)
    public DqIssue assignIssue(String issueId, String assignee, String operator) {
        DqIssue issue = issueMapper.selectByIssueId(issueId);
        if (issue == null) throw new RuntimeException("数据质量问题不存在: " + issueId);
        issue.setAssignee(assignee);
        issue.setStatus("IN_PROGRESS");
        issue.setOperator(operator);
        issueMapper.updateById(issue);
        log.info("分配数据质量问题: id={}, assignee={}", issueId, assignee);
        return issueMapper.selectByIssueId(issueId);
    }

    @Transactional(rollbackFor = Exception.class)
    public DqIssue fixIssue(String issueId, String fixPlan, String fixResult, String operator) {
        DqIssue issue = issueMapper.selectByIssueId(issueId);
        if (issue == null) throw new RuntimeException("数据质量问题不存在: " + issueId);
        issue.setFixPlan(fixPlan);
        issue.setFixResult(fixResult);
        issue.setFixTime(LocalDateTime.now());
        issue.setStatus("FIXED");
        issue.setOperator(operator);
        issueMapper.updateById(issue);
        log.info("修复数据质量问题: id={}", issueId);
        return issueMapper.selectByIssueId(issueId);
    }

    @Transactional(rollbackFor = Exception.class)
    public DqIssue verifyIssue(String issueId, String verifier, boolean passed) {
        DqIssue issue = issueMapper.selectByIssueId(issueId);
        if (issue == null) throw new RuntimeException("数据质量问题不存在: " + issueId);
        if (passed) {
            issue.setStatus("VERIFIED");
            issue.setVerifiedTime(LocalDateTime.now());
            issue.setVerifier(verifier);
            log.info("验证通过数据质量问题: id={}", issueId);
        } else {
            issue.setStatus("REOPENED");
            issue.setReopenCount(issue.getReopenCount() + 1);
            log.info("重开数据质量问题: id={}, reopenCount={}", issueId, issue.getReopenCount());
        }
        issueMapper.updateById(issue);
        return issueMapper.selectByIssueId(issueId);
    }

    @Transactional(rollbackFor = Exception.class)
    public DqIssue closeIssue(String issueId, String operator) {
        DqIssue issue = issueMapper.selectByIssueId(issueId);
        if (issue == null) throw new RuntimeException("数据质量问题不存在: " + issueId);
        issue.setStatus("CLOSED");
        issue.setOperator(operator);
        issueMapper.updateById(issue);
        log.info("关闭数据质量问题: id={}", issueId);
        return issueMapper.selectByIssueId(issueId);
    }

    public List<DqIssue> getIssuesByCheckId(String checkId) {
        return issueMapper.selectByCheckId(checkId);
    }

    public List<DqIssue> getIssuesByWarehouseAndStatus(String warehouseCode, String status) {
        return issueMapper.selectByWarehouseAndStatus(warehouseCode, status);
    }

    public List<DqIssue> getOpenIssuesByAssignee(String assignee) {
        return issueMapper.selectOpenByAssignee(assignee);
    }

    public List<DqIssue> getOpenIssuesBySeverity(String severity) {
        return issueMapper.selectOpenBySeverity(severity);
    }

    public Page<DqIssue> pageIssue(
            Page<DqIssue> page,
            String checkId,
            String warehouseCode,
            String status,
            String severity,
            String assignee) {
        LambdaQueryWrapper<DqIssue> wrapper = new LambdaQueryWrapper<>();
        if (checkId != null) wrapper.eq(DqIssue::getCheckId, checkId);
        if (warehouseCode != null) wrapper.eq(DqIssue::getWarehouseCode, warehouseCode);
        if (status != null) wrapper.eq(DqIssue::getStatus, status);
        if (severity != null) wrapper.eq(DqIssue::getSeverity, severity);
        if (assignee != null) wrapper.eq(DqIssue::getAssignee, assignee);
        wrapper.orderByDesc(DqIssue::getCreatedTime);
        return issueMapper.selectPage(page, wrapper);
    }

    private String generateId(String prefix) {
        return prefix
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }
}
