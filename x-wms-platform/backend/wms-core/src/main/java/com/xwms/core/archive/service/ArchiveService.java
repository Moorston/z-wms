package com.xwms.core.archive.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.common.exception.BizException;
import com.xwms.core.archive.entity.*;
import com.xwms.core.archive.enums.ArchiveStatus;
import com.xwms.core.archive.mapper.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 数据归档核心服务 包含: 归档规则/归档任务/批次处理/归档查询 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArchiveService {

    private final ArchiveRuleMapper ruleMapper;
    private final ArchiveTaskMapper taskMapper;
    private final ArchiveBatchMapper batchMapper;
    private final ArchiveRecordMapper recordMapper;
    private final JdbcTemplate jdbcTemplate;

    private static final AtomicInteger TASK_SEQ = new AtomicInteger(0);
    private static final AtomicInteger BATCH_SEQ = new AtomicInteger(0);
    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    // ============================================================

    // 1. 归档规则管理
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public ArchiveRule createRule(ArchiveRule rule) {
        rule.setEnabled(1);
        if (rule.getArchiveTable() == null) {
            rule.setArchiveTable(rule.getTableName() + "_ARCH");
        }
        ruleMapper.insert(rule);
        log.info("创建归档规则: {}, 表={}", rule.getRuleCode(), rule.getTableName());
        return rule;
    }

    public ArchiveRule getRule(Long id) {
        ArchiveRule rule = ruleMapper.selectById(id);
        if (rule == null) throw new BizException("归档规则不存在: " + id);
        return rule;
    }

    public Page<ArchiveRule> pageRules(Page<ArchiveRule> page, String tableName, Integer enabled) {
        LambdaQueryWrapper<ArchiveRule> wrapper = new LambdaQueryWrapper<>();
        if (tableName != null) wrapper.eq(ArchiveRule::getTableName, tableName);
        if (enabled != null) wrapper.eq(ArchiveRule::getEnabled, enabled);
        wrapper.orderByDesc(ArchiveRule::getCreatedTime);
        return ruleMapper.selectPage(page, wrapper);
    }

    public List<ArchiveRule> getEnabledRules() {
        return ruleMapper.selectEnabled();
    }

    @Transactional(rollbackFor = Exception.class)
    public ArchiveRule toggleRule(Long id, boolean enabled) {
        ArchiveRule rule = getRule(id);
        rule.setEnabled(enabled ? 1 : 0);
        ruleMapper.updateById(rule);
        log.info("归档规则{}: {}", enabled ? "启用" : "禁用", rule.getRuleCode());
        return rule;
    }

    // ============================================================

    // 2. 执行归档任务 (核心, PowerJob定时触发)
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public ArchiveTask executeArchive(Long ruleId, String triggeredBy) {
        ArchiveRule rule = getRule(ruleId);
        if (rule.getEnabled() == 0) {
            throw new BizException("归档规则已禁用: " + rule.getRuleCode());
        }

        // 1. 创建归档任务
        ArchiveTask task = new ArchiveTask();
        task.setTaskNo(generateTaskNo());
        task.setRuleId(ruleId);
        task.setRuleCode(rule.getRuleCode());
        task.setTableName(rule.getTableName());
        task.setArchiveType(rule.getArchiveType());
        task.setStatus(ArchiveStatus.RUNNING.getCode());
        task.setTotalCount(0L);
        task.setArchivedCount(0L);
        task.setFailedCount(0L);
        task.setStartTime(LocalDateTime.now());
        task.setCreatedBy(triggeredBy);
        taskMapper.insert(task);

        try {
            // 2. 统计待归档记录数
            long totalCount = countArchiveRecords(rule);
            task.setTotalCount(totalCount);

            if (totalCount == 0) {
                task.setStatus(ArchiveStatus.COMPLETED.getCode());
                task.setEndTime(LocalDateTime.now());
                task.setDurationMin(0);
                taskMapper.updateById(task);
                ruleMapper.updateLastExecuteTime(ruleId);
                log.info("归档任务{}无待归档数据", task.getTaskNo());
                return task;
            }

            // 3. 分批归档
            int batchSize = rule.getBatchSize() != null ? rule.getBatchSize() : 1000;
            int totalBatches = (int) Math.ceil((double) totalCount / batchSize);
            long archivedCount = 0;
            long failedCount = 0;

            for (int i = 0; i < totalBatches; i++) {
                ArchiveBatch batch = executeBatch(task, rule, i, batchSize);
                if (ArchiveStatus.COMPLETED.getCode().equals(batch.getStatus())) {
                    archivedCount += batch.getRecordCount() != null ? batch.getRecordCount() : 0;
                } else {
                    failedCount += batch.getRecordCount() != null ? batch.getRecordCount() : 0;
                }
                task.setArchivedCount(archivedCount);
                task.setFailedCount(failedCount);
                taskMapper.updateById(task);
            }

            // 4. 完成任务
            task.setStatus(
                    failedCount > 0
                            ? ArchiveStatus.COMPLETED.getCode()
                            : ArchiveStatus.COMPLETED.getCode());
            task.setEndTime(LocalDateTime.now());
            long minutes =
                    java.time.Duration.between(task.getStartTime(), LocalDateTime.now())
                            .toMinutes();
            task.setDurationMin((int) minutes);
            taskMapper.updateById(task);
            ruleMapper.updateLastExecuteTime(ruleId);

            log.info(
                    "归档任务{}完成: 总数={}, 归档={}, 失败={}",
                    task.getTaskNo(),
                    totalCount,
                    archivedCount,
                    failedCount);
            return task;

        } catch (Exception e) {
            task.setStatus(ArchiveStatus.FAILED.getCode());
            task.setErrorMsg(e.getMessage());
            task.setEndTime(LocalDateTime.now());
            taskMapper.updateById(task);
            log.error("归档任务{}失败: {}", task.getTaskNo(), e.getMessage(), e);
            throw new BizException("归档失败: " + e.getMessage());
        }
    }

    /** 执行单批归档 */
    private ArchiveBatch executeBatch(
            ArchiveTask task, ArchiveRule rule, int batchIndex, int batchSize) {
        ArchiveBatch batch = new ArchiveBatch();
        batch.setBatchNo(generateBatchNo());
        batch.setTaskId(task.getId());
        batch.setTaskNo(task.getTaskNo());
        batch.setBatchIndex(batchIndex);
        batch.setStatus(ArchiveStatus.RUNNING.getCode());
        batch.setStartTime(LocalDateTime.now());
        batchMapper.insert(batch);

        try {
            // 构建归档SQL
            String archiveSql = buildArchiveSql(rule, batchSize);
            int count = jdbcTemplate.update(archiveSql);
            batch.setRecordCount(count);
            batch.setStatus(ArchiveStatus.COMPLETED.getCode());
            batch.setEndTime(LocalDateTime.now());
            batchMapper.updateById(batch);
            log.info("归档批次{}完成: 记录数={}", batch.getBatchNo(), count);
        } catch (Exception e) {
            batch.setStatus(ArchiveStatus.FAILED.getCode());
            batch.setErrorMsg(e.getMessage());
            batch.setEndTime(LocalDateTime.now());
            batchMapper.updateById(batch);
            log.error("归档批次{}失败: {}", batch.getBatchNo(), e.getMessage());
        }
        return batch;
    }

    /** 统计待归档记录数 */
    private long countArchiveRecords(ArchiveRule rule) {
        String whereClause = buildWhereClause(rule);
        String countSql = "SELECT COUNT(*) FROM " + rule.getTableName() + " WHERE " + whereClause;
        Long count = jdbcTemplate.queryForObject(countSql, Long.class);
        return count != null ? count : 0;
    }

    /** 构建归档SQL */
    private String buildArchiveSql(ArchiveRule rule, int limit) {
        String whereClause = buildWhereClause(rule);
        String archiveTable = rule.getArchiveTable();
        String limitClause = " LIMIT " + limit;

        if ("MOVE".equals(rule.getArchiveMode())) {
            // 移动: 先插入归档表, 再删除源表
            return "INSERT INTO "
                    + archiveTable
                    + " SELECT * FROM "
                    + rule.getTableName()
                    + " WHERE "
                    + whereClause
                    + limitClause
                    + "; DELETE FROM "
                    + rule.getTableName()
                    + " WHERE "
                    + whereClause
                    + limitClause;
        } else if ("COPY".equals(rule.getArchiveMode())) {
            // 复制: 仅插入归档表
            return "INSERT INTO "
                    + archiveTable
                    + " SELECT * FROM "
                    + rule.getTableName()
                    + " WHERE "
                    + whereClause
                    + limitClause;
        } else {
            // DELETE: 仅删除源表
            return "DELETE FROM " + rule.getTableName() + " WHERE " + whereClause + limitClause;
        }
    }

    /** 构建WHERE条件 */
    private String buildWhereClause(ArchiveRule rule) {
        if ("DATE".equals(rule.getArchiveType())) {
            LocalDate cutoffDate =
                    LocalDate.now()
                            .minusDays(rule.getRetainDays() != null ? rule.getRetainDays() : 365);
            return rule.getDateField() + " < DATE '" + cutoffDate + "'";
        } else if ("STATUS".equals(rule.getArchiveType())) {
            return rule.getStatusField() + " IN (" + rule.getStatusValues() + ")";
        } else {
            return "1=1";
        }
    }

    // ============================================================

    // 3. 归档任务查询
    // ============================================================

    public ArchiveTask getTask(Long id) {
        ArchiveTask task = taskMapper.selectById(id);
        if (task == null) throw new BizException("归档任务不存在: " + id);
        return task;
    }

    public Page<ArchiveTask> pageTasks(Page<ArchiveTask> page, Long ruleId, String status) {
        LambdaQueryWrapper<ArchiveTask> wrapper = new LambdaQueryWrapper<>();
        if (ruleId != null) wrapper.eq(ArchiveTask::getRuleId, ruleId);
        if (status != null) wrapper.eq(ArchiveTask::getStatus, status);
        wrapper.orderByDesc(ArchiveTask::getCreatedTime);
        return taskMapper.selectPage(page, wrapper);
    }

    public List<ArchiveTask> getTasksByRule(Long ruleId) {
        return taskMapper.selectByRuleId(ruleId);
    }

    public List<ArchiveBatch> getTaskBatches(Long taskId) {
        return batchMapper.selectByTaskId(taskId);
    }

    // ============================================================

    // 4. 归档数据查询
    // ============================================================

    public ArchiveRecord getArchiveRecord(String tableName, Long sourceId) {
        return recordMapper.selectBySource(tableName, sourceId);
    }

    public List<ArchiveRecord> getBatchRecords(Long batchId) {
        return recordMapper.selectByBatchId(batchId);
    }

    // ============================================================

    // 工具方法
    // ============================================================

    private String generateTaskNo() {
        return "AT"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", TASK_SEQ.incrementAndGet() % 1000);
    }

    private String generateBatchNo() {
        return "AB"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", BATCH_SEQ.incrementAndGet() % 1000);
    }
}
