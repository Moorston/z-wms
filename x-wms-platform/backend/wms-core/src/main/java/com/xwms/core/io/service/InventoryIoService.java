package com.xwms.core.io.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.core.io.entity.*;
import com.xwms.core.io.mapper.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 库存导入导出管理核心服务 核心能力: 导入任务/导入记录/导出任务/导出记录 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryIoService {

    private final ImportTaskMapper importTaskMapper;
    private final ImportRecordMapper importRecordMapper;
    private final ExportTaskMapper exportTaskMapper;
    private final ExportRecordMapper exportRecordMapper;

    private static final AtomicInteger SEQ = new AtomicInteger(0);
    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    // ============================================================

    // 1. 导入任务管理
    // ============================================================

    /** 创建导入任务 */
    @Transactional(rollbackFor = Exception.class)
    public ImportTask createImportTask(
            String taskName,
            String importType,
            String warehouseCode,
            String ownerCode,
            String fileName,
            String filePath,
            Long fileSize,
            String fileFormat,
            String operator) {
        ImportTask task = new ImportTask();
        task.setTaskId(generateImportTaskId());
        task.setTaskName(taskName);
        task.setImportType(importType);
        task.setWarehouseCode(warehouseCode);
        task.setOwnerCode(ownerCode);
        task.setFileName(fileName);
        task.setFilePath(filePath);
        task.setFileSize(fileSize);
        task.setFileFormat(fileFormat != null ? fileFormat : "EXCEL");
        task.setTotalCount(0);
        task.setSuccessCount(0);
        task.setFailCount(0);
        task.setSkipCount(0);
        task.setStatus("PENDING");
        task.setOperator(operator);
        importTaskMapper.insert(task);

        log.info(
                "创建导入任务: taskId={}, type={}, file={}, operator={}",
                task.getTaskId(),
                importType,
                fileName,
                operator);
        return task;
    }

    /** 开始导入任务 */
    @Transactional(rollbackFor = Exception.class)
    public ImportTask startImportTask(String taskId) {
        ImportTask task = importTaskMapper.selectByTaskId(taskId);
        if (task == null) throw new RuntimeException("导入任务不存在: " + taskId);
        if (!"PENDING".equals(task.getStatus())) {
            throw new RuntimeException("任务状态不正确: " + task.getStatus());
        }

        importTaskMapper.startTask(taskId, "PROCESSING");
        log.info("开始导入任务: taskId={}", taskId);
        return importTaskMapper.selectByTaskId(taskId);
    }

    /** 完成导入任务 */
    @Transactional(rollbackFor = Exception.class)
    public ImportTask completeImportTask(
            String taskId,
            int totalCount,
            int successCount,
            int failCount,
            int skipCount,
            Long durationMs,
            String errorMessage) {
        String status = failCount > 0 && successCount == 0 ? "FAILED" : "COMPLETED";
        importTaskMapper.completeTask(
                taskId,
                status,
                totalCount,
                successCount,
                failCount,
                skipCount,
                durationMs,
                errorMessage);
        log.info(
                "完成导入任务: taskId={}, total={}, success={}, fail={}, skip={}",
                taskId,
                totalCount,
                successCount,
                failCount,
                skipCount);
        return importTaskMapper.selectByTaskId(taskId);
    }

    /** 取消导入任务 */
    @Transactional(rollbackFor = Exception.class)
    public ImportTask cancelImportTask(String taskId, String reason, String operator) {
        ImportTask task = importTaskMapper.selectByTaskId(taskId);
        if (task == null) throw new RuntimeException("导入任务不存在: " + taskId);
        if (!"PENDING".equals(task.getStatus()) && !"PROCESSING".equals(task.getStatus())) {
            throw new RuntimeException("任务状态不正确: " + task.getStatus());
        }

        importTaskMapper.completeTask(
                taskId,
                "CANCELLED",
                task.getTotalCount(),
                task.getSuccessCount(),
                task.getFailCount(),
                task.getSkipCount(),
                0L,
                reason);
        log.info("取消导入任务: taskId={}, reason={}", taskId, reason);
        return importTaskMapper.selectByTaskId(taskId);
    }

    public ImportTask getImportTaskById(String taskId) {
        return importTaskMapper.selectByTaskId(taskId);
    }

    public List<ImportTask> getImportTasksByStatus(String status) {
        return importTaskMapper.selectByStatus(status);
    }

    public List<ImportTask> getRecentImportTasksByOperator(String operator, int limit) {
        return importTaskMapper.selectRecentByOperator(operator, limit);
    }

    public Page<ImportTask> pageImportTasks(
            Page<ImportTask> page,
            String importType,
            String status,
            String warehouseCode,
            String operator) {
        LambdaQueryWrapper<ImportTask> wrapper = new LambdaQueryWrapper<>();
        if (importType != null) wrapper.eq(ImportTask::getImportType, importType);
        if (status != null) wrapper.eq(ImportTask::getStatus, status);
        if (warehouseCode != null) wrapper.eq(ImportTask::getWarehouseCode, warehouseCode);
        if (operator != null) wrapper.eq(ImportTask::getOperator, operator);
        wrapper.orderByDesc(ImportTask::getCreatedTime);
        return importTaskMapper.selectPage(page, wrapper);
    }

    // ============================================================

    // 2. 导入记录管理
    // ============================================================

    /** 批量创建导入记录 */
    @Transactional(rollbackFor = Exception.class)
    public int createImportRecords(String taskId, List<Map<String, Object>> rows) {
        int count = 0;
        for (int i = 0; i < rows.size(); i++) {
            ImportRecord record = new ImportRecord();
            record.setRecordId(generateImportRecordId());
            record.setTaskId(taskId);
            record.setRowNo(i + 1);
            record.setRowData(rows.get(i).toString());
            record.setImportStatus("PENDING");
            record.setRetryCount(0);
            importRecordMapper.insert(record);
            count++;
        }
        log.info("创建导入记录: taskId={}, count={}", taskId, count);
        return count;
    }

    /** 更新导入记录状态 */
    @Transactional(rollbackFor = Exception.class)
    public ImportRecord updateImportRecord(
            String recordId, String status, String errorCode, String errorMessage) {
        importRecordMapper.updateStatus(recordId, status, errorCode, errorMessage);
        return importRecordMapper.selectOne(
                new LambdaQueryWrapper<ImportRecord>().eq(ImportRecord::getRecordId, recordId));
    }

    public List<ImportRecord> getImportRecordsByTask(String taskId) {
        return importRecordMapper.selectByTaskId(taskId);
    }

    public List<ImportRecord> getImportRecordsByTaskAndStatus(String taskId, String status) {
        return importRecordMapper.selectByTaskIdAndStatus(taskId, status);
    }

    public int countImportRecordsByTaskAndStatus(String taskId, String status) {
        return importRecordMapper.countByTaskIdAndStatus(taskId, status);
    }

    public Page<ImportRecord> pageImportRecords(
            Page<ImportRecord> page, String taskId, String status) {
        LambdaQueryWrapper<ImportRecord> wrapper = new LambdaQueryWrapper<>();
        if (taskId != null) wrapper.eq(ImportRecord::getTaskId, taskId);
        if (status != null) wrapper.eq(ImportRecord::getImportStatus, status);
        wrapper.orderByAsc(ImportRecord::getRowNo);
        return importRecordMapper.selectPage(page, wrapper);
    }

    // ============================================================

    // 3. 导出任务管理
    // ============================================================

    /** 创建导出任务 */
    @Transactional(rollbackFor = Exception.class)
    public ExportTask createExportTask(
            String taskName,
            String exportType,
            String warehouseCode,
            String ownerCode,
            String queryParams,
            String exportColumns,
            String fileFormat,
            String operator) {
        ExportTask task = new ExportTask();
        task.setTaskId(generateExportTaskId());
        task.setTaskName(taskName);
        task.setExportType(exportType);
        task.setWarehouseCode(warehouseCode);
        task.setOwnerCode(ownerCode);
        task.setQueryParams(queryParams);
        task.setExportColumns(exportColumns);
        task.setFileFormat(fileFormat != null ? fileFormat : "EXCEL");
        task.setTotalCount(0);
        task.setExportedCount(0);
        task.setStatus("PENDING");
        task.setOperator(operator);
        task.setDownloadCount(0);
        // 默认7天后过期
        task.setExpireTime(LocalDateTime.now().plusDays(7));
        exportTaskMapper.insert(task);

        log.info("创建导出任务: taskId={}, type={}, operator={}", task.getTaskId(), exportType, operator);
        return task;
    }

    /** 开始导出任务 */
    @Transactional(rollbackFor = Exception.class)
    public ExportTask startExportTask(String taskId) {
        ExportTask task = exportTaskMapper.selectByTaskId(taskId);
        if (task == null) throw new RuntimeException("导出任务不存在: " + taskId);
        if (!"PENDING".equals(task.getStatus())) {
            throw new RuntimeException("任务状态不正确: " + task.getStatus());
        }

        exportTaskMapper.startTask(taskId, "PROCESSING");
        log.info("开始导出任务: taskId={}", taskId);
        return exportTaskMapper.selectByTaskId(taskId);
    }

    /** 完成导出任务 */
    @Transactional(rollbackFor = Exception.class)
    public ExportTask completeExportTask(
            String taskId,
            int totalCount,
            int exportedCount,
            String fileName,
            String filePath,
            Long fileSize,
            Long durationMs,
            String errorMessage) {
        String status = "COMPLETED";
        exportTaskMapper.completeTask(
                taskId,
                status,
                totalCount,
                exportedCount,
                fileName,
                filePath,
                fileSize,
                durationMs,
                errorMessage);
        log.info(
                "完成导出任务: taskId={}, total={}, exported={}, file={}",
                taskId,
                totalCount,
                exportedCount,
                fileName);
        return exportTaskMapper.selectByTaskId(taskId);
    }

    /** 取消导出任务 */
    @Transactional(rollbackFor = Exception.class)
    public ExportTask cancelExportTask(String taskId, String reason, String operator) {
        ExportTask task = exportTaskMapper.selectByTaskId(taskId);
        if (task == null) throw new RuntimeException("导出任务不存在: " + taskId);
        if (!"PENDING".equals(task.getStatus()) && !"PROCESSING".equals(task.getStatus())) {
            throw new RuntimeException("任务状态不正确: " + task.getStatus());
        }

        exportTaskMapper.completeTask(
                taskId,
                "CANCELLED",
                task.getTotalCount(),
                task.getExportedCount(),
                null,
                null,
                null,
                0L,
                reason);
        log.info("取消导出任务: taskId={}, reason={}", taskId, reason);
        return exportTaskMapper.selectByTaskId(taskId);
    }

    /** 记录下载 */
    @Transactional(rollbackFor = Exception.class)
    public int recordDownload(String taskId) {
        return exportTaskMapper.incrementDownloadCount(taskId);
    }

    public ExportTask getExportTaskById(String taskId) {
        return exportTaskMapper.selectByTaskId(taskId);
    }

    public List<ExportTask> getExportTasksByStatus(String status) {
        return exportTaskMapper.selectByStatus(status);
    }

    public List<ExportTask> getRecentExportTasksByOperator(String operator, int limit) {
        return exportTaskMapper.selectRecentByOperator(operator, limit);
    }

    public Page<ExportTask> pageExportTasks(
            Page<ExportTask> page,
            String exportType,
            String status,
            String warehouseCode,
            String operator) {
        LambdaQueryWrapper<ExportTask> wrapper = new LambdaQueryWrapper<>();
        if (exportType != null) wrapper.eq(ExportTask::getExportType, exportType);
        if (status != null) wrapper.eq(ExportTask::getStatus, status);
        if (warehouseCode != null) wrapper.eq(ExportTask::getWarehouseCode, warehouseCode);
        if (operator != null) wrapper.eq(ExportTask::getOperator, operator);
        wrapper.orderByDesc(ExportTask::getCreatedTime);
        return exportTaskMapper.selectPage(page, wrapper);
    }

    // ============================================================

    // 4. 导出记录管理
    // ============================================================

    /** 批量创建导出记录 */
    @Transactional(rollbackFor = Exception.class)
    public int createExportRecords(String taskId, List<Map<String, Object>> rows) {
        int count = 0;
        for (int i = 0; i < rows.size(); i++) {
            ExportRecord record = new ExportRecord();
            record.setRecordId(generateExportRecordId());
            record.setTaskId(taskId);
            record.setRowNo(i + 1);
            record.setRowData(rows.get(i).toString());
            record.setExportStatus("PENDING");
            exportRecordMapper.insert(record);
            count++;
        }
        log.info("创建导出记录: taskId={}, count={}", taskId, count);
        return count;
    }

    /** 更新导出记录状态 */
    @Transactional(rollbackFor = Exception.class)
    public ExportRecord updateExportRecord(String recordId, String status, String errorMessage) {
        exportRecordMapper.updateStatus(recordId, status, errorMessage);
        return exportRecordMapper.selectOne(
                new LambdaQueryWrapper<ExportRecord>().eq(ExportRecord::getRecordId, recordId));
    }

    public List<ExportRecord> getExportRecordsByTask(String taskId) {
        return exportRecordMapper.selectByTaskId(taskId);
    }

    public List<ExportRecord> getExportRecordsByTaskAndStatus(String taskId, String status) {
        return exportRecordMapper.selectByTaskIdAndStatus(taskId, status);
    }

    public int countExportRecordsByTaskAndStatus(String taskId, String status) {
        return exportRecordMapper.countByTaskIdAndStatus(taskId, status);
    }

    public Page<ExportRecord> pageExportRecords(
            Page<ExportRecord> page, String taskId, String status) {
        LambdaQueryWrapper<ExportRecord> wrapper = new LambdaQueryWrapper<>();
        if (taskId != null) wrapper.eq(ExportRecord::getTaskId, taskId);
        if (status != null) wrapper.eq(ExportRecord::getExportStatus, status);
        wrapper.orderByAsc(ExportRecord::getRowNo);
        return exportRecordMapper.selectPage(page, wrapper);
    }

    // ============================================================

    // 工具方法
    // ============================================================

    private String generateImportTaskId() {
        return "IT"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }

    private String generateImportRecordId() {
        return "IR"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }

    private String generateExportTaskId() {
        return "ET"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }

    private String generateExportRecordId() {
        return "ER"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }
}
