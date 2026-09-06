package com.xwms.core.io.controller;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.common.core.Result;
import com.xwms.core.io.entity.*;
import com.xwms.core.io.service.InventoryIoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/** 库存导入导出管理 Controller */
@Tag(name = "库存导入导出管理", description = "导入任务/导入记录/导出任务/导出记录")
@RestController
@RequestMapping("/api/io")
@RequiredArgsConstructor
public class InventoryIoController {

    private final InventoryIoService ioService;

    // ============================================================

    // 导入任务
    // ============================================================

    @Operation(summary = "创建导入任务")
    @PostMapping("/import/task")
    public Result<ImportTask> createImportTask(
            @RequestParam String taskName,
            @RequestParam String importType,
            @RequestParam(required = false) String warehouseCode,
            @RequestParam(required = false) String ownerCode,
            @RequestParam String fileName,
            @RequestParam String filePath,
            @RequestParam Long fileSize,
            @RequestParam(defaultValue = "EXCEL") String fileFormat,
            @RequestParam String operator) {
        return Result.success(
                ioService.createImportTask(
                        taskName,
                        importType,
                        warehouseCode,
                        ownerCode,
                        fileName,
                        filePath,
                        fileSize,
                        fileFormat,
                        operator));
    }

    @Operation(summary = "开始导入任务")
    @PostMapping("/import/task/{taskId}/start")
    public Result<ImportTask> startImportTask(@PathVariable String taskId) {
        return Result.success(ioService.startImportTask(taskId));
    }

    @Operation(summary = "完成导入任务")
    @PostMapping("/import/task/{taskId}/complete")
    public Result<ImportTask> completeImportTask(
            @PathVariable String taskId,
            @RequestParam int totalCount,
            @RequestParam int successCount,
            @RequestParam int failCount,
            @RequestParam int skipCount,
            @RequestParam Long durationMs,
            @RequestParam(required = false) String errorMessage) {
        return Result.success(
                ioService.completeImportTask(
                        taskId,
                        totalCount,
                        successCount,
                        failCount,
                        skipCount,
                        durationMs,
                        errorMessage));
    }

    @Operation(summary = "取消导入任务")
    @PostMapping("/import/task/{taskId}/cancel")
    public Result<ImportTask> cancelImportTask(
            @PathVariable String taskId,
            @RequestParam String reason,
            @RequestParam String operator) {
        return Result.success(ioService.cancelImportTask(taskId, reason, operator));
    }

    @Operation(summary = "按ID查询导入任务")
    @GetMapping("/import/task/{taskId}")
    public Result<ImportTask> getImportTaskById(@PathVariable String taskId) {
        return Result.success(ioService.getImportTaskById(taskId));
    }

    @Operation(summary = "按状态查询导入任务")
    @GetMapping("/import/task/status/{status}")
    public Result<List<ImportTask>> getImportTasksByStatus(@PathVariable String status) {
        return Result.success(ioService.getImportTasksByStatus(status));
    }

    @Operation(summary = "按操作人查询最近导入任务")
    @GetMapping("/import/task/recent/{operator}")
    public Result<List<ImportTask>> getRecentImportTasksByOperator(
            @PathVariable String operator, @RequestParam(defaultValue = "10") int limit) {
        return Result.success(ioService.getRecentImportTasksByOperator(operator, limit));
    }

    @Operation(summary = "分页查询导入任务")
    @GetMapping("/import/task/list")
    public Result<Page<ImportTask>> pageImportTasks(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String importType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String warehouseCode,
            @RequestParam(required = false) String operator) {
        return Result.success(
                ioService.pageImportTasks(
                        new Page<>(page, size), importType, status, warehouseCode, operator));
    }

    // ============================================================

    // 导入记录
    // ============================================================

    @Operation(summary = "批量创建导入记录")
    @PostMapping("/import/record/{taskId}/batch")
    public Result<Integer> createImportRecords(
            @PathVariable String taskId, @RequestBody List<Map<String, Object>> rows) {
        return Result.success(ioService.createImportRecords(taskId, rows));
    }

    @Operation(summary = "更新导入记录状态")
    @PostMapping("/import/record/{recordId}/status")
    public Result<ImportRecord> updateImportRecord(
            @PathVariable String recordId,
            @RequestParam String status,
            @RequestParam(required = false) String errorCode,
            @RequestParam(required = false) String errorMessage) {
        return Result.success(
                ioService.updateImportRecord(recordId, status, errorCode, errorMessage));
    }

    @Operation(summary = "按任务查询导入记录")
    @GetMapping("/import/record/task/{taskId}")
    public Result<List<ImportRecord>> getImportRecordsByTask(@PathVariable String taskId) {
        return Result.success(ioService.getImportRecordsByTask(taskId));
    }

    @Operation(summary = "按任务和状态查询导入记录")
    @GetMapping("/import/record/task/{taskId}/status/{status}")
    public Result<List<ImportRecord>> getImportRecordsByTaskAndStatus(
            @PathVariable String taskId, @PathVariable String status) {
        return Result.success(ioService.getImportRecordsByTaskAndStatus(taskId, status));
    }

    @Operation(summary = "统计导入记录数量")
    @GetMapping("/import/record/count/{taskId}/{status}")
    public Result<Integer> countImportRecordsByTaskAndStatus(
            @PathVariable String taskId, @PathVariable String status) {
        return Result.success(ioService.countImportRecordsByTaskAndStatus(taskId, status));
    }

    @Operation(summary = "分页查询导入记录")
    @GetMapping("/import/record/list")
    public Result<Page<ImportRecord>> pageImportRecords(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String taskId,
            @RequestParam(required = false) String status) {
        return Result.success(ioService.pageImportRecords(new Page<>(page, size), taskId, status));
    }

    // ============================================================

    // 导出任务
    // ============================================================

    @Operation(summary = "创建导出任务")
    @PostMapping("/export/task")
    public Result<ExportTask> createExportTask(
            @RequestParam String taskName,
            @RequestParam String exportType,
            @RequestParam(required = false) String warehouseCode,
            @RequestParam(required = false) String ownerCode,
            @RequestParam(required = false) String queryParams,
            @RequestParam(required = false) String exportColumns,
            @RequestParam(defaultValue = "EXCEL") String fileFormat,
            @RequestParam String operator) {
        return Result.success(
                ioService.createExportTask(
                        taskName,
                        exportType,
                        warehouseCode,
                        ownerCode,
                        queryParams,
                        exportColumns,
                        fileFormat,
                        operator));
    }

    @Operation(summary = "开始导出任务")
    @PostMapping("/export/task/{taskId}/start")
    public Result<ExportTask> startExportTask(@PathVariable String taskId) {
        return Result.success(ioService.startExportTask(taskId));
    }

    @Operation(summary = "完成导出任务")
    @PostMapping("/export/task/{taskId}/complete")
    public Result<ExportTask> completeExportTask(
            @PathVariable String taskId,
            @RequestParam int totalCount,
            @RequestParam int exportedCount,
            @RequestParam(required = false) String fileName,
            @RequestParam(required = false) String filePath,
            @RequestParam(required = false) Long fileSize,
            @RequestParam Long durationMs,
            @RequestParam(required = false) String errorMessage) {
        return Result.success(
                ioService.completeExportTask(
                        taskId,
                        totalCount,
                        exportedCount,
                        fileName,
                        filePath,
                        fileSize,
                        durationMs,
                        errorMessage));
    }

    @Operation(summary = "取消导出任务")
    @PostMapping("/export/task/{taskId}/cancel")
    public Result<ExportTask> cancelExportTask(
            @PathVariable String taskId,
            @RequestParam String reason,
            @RequestParam String operator) {
        return Result.success(ioService.cancelExportTask(taskId, reason, operator));
    }

    @Operation(summary = "记录下载")
    @PostMapping("/export/task/{taskId}/download")
    public Result<Integer> recordDownload(@PathVariable String taskId) {
        return Result.success(ioService.recordDownload(taskId));
    }

    @Operation(summary = "按ID查询导出任务")
    @GetMapping("/export/task/{taskId}")
    public Result<ExportTask> getExportTaskById(@PathVariable String taskId) {
        return Result.success(ioService.getExportTaskById(taskId));
    }

    @Operation(summary = "按状态查询导出任务")
    @GetMapping("/export/task/status/{status}")
    public Result<List<ExportTask>> getExportTasksByStatus(@PathVariable String status) {
        return Result.success(ioService.getExportTasksByStatus(status));
    }

    @Operation(summary = "按操作人查询最近导出任务")
    @GetMapping("/export/task/recent/{operator}")
    public Result<List<ExportTask>> getRecentExportTasksByOperator(
            @PathVariable String operator, @RequestParam(defaultValue = "10") int limit) {
        return Result.success(ioService.getRecentExportTasksByOperator(operator, limit));
    }

    @Operation(summary = "分页查询导出任务")
    @GetMapping("/export/task/list")
    public Result<Page<ExportTask>> pageExportTasks(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String exportType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String warehouseCode,
            @RequestParam(required = false) String operator) {
        return Result.success(
                ioService.pageExportTasks(
                        new Page<>(page, size), exportType, status, warehouseCode, operator));
    }

    // ============================================================

    // 导出记录
    // ============================================================

    @Operation(summary = "批量创建导出记录")
    @PostMapping("/export/record/{taskId}/batch")
    public Result<Integer> createExportRecords(
            @PathVariable String taskId, @RequestBody List<Map<String, Object>> rows) {
        return Result.success(ioService.createExportRecords(taskId, rows));
    }

    @Operation(summary = "更新导出记录状态")
    @PostMapping("/export/record/{recordId}/status")
    public Result<ExportRecord> updateExportRecord(
            @PathVariable String recordId,
            @RequestParam String status,
            @RequestParam(required = false) String errorMessage) {
        return Result.success(ioService.updateExportRecord(recordId, status, errorMessage));
    }

    @Operation(summary = "按任务查询导出记录")
    @GetMapping("/export/record/task/{taskId}")
    public Result<List<ExportRecord>> getExportRecordsByTask(@PathVariable String taskId) {
        return Result.success(ioService.getExportRecordsByTask(taskId));
    }

    @Operation(summary = "按任务和状态查询导出记录")
    @GetMapping("/export/record/task/{taskId}/status/{status}")
    public Result<List<ExportRecord>> getExportRecordsByTaskAndStatus(
            @PathVariable String taskId, @PathVariable String status) {
        return Result.success(ioService.getExportRecordsByTaskAndStatus(taskId, status));
    }

    @Operation(summary = "统计导出记录数量")
    @GetMapping("/export/record/count/{taskId}/{status}")
    public Result<Integer> countExportRecordsByTaskAndStatus(
            @PathVariable String taskId, @PathVariable String status) {
        return Result.success(ioService.countExportRecordsByTaskAndStatus(taskId, status));
    }

    @Operation(summary = "分页查询导出记录")
    @GetMapping("/export/record/list")
    public Result<Page<ExportRecord>> pageExportRecords(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String taskId,
            @RequestParam(required = false) String status) {
        return Result.success(ioService.pageExportRecords(new Page<>(page, size), taskId, status));
    }
}
