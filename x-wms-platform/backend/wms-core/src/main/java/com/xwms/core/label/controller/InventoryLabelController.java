package com.xwms.core.label.controller;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.common.core.Result;
import com.xwms.core.label.entity.*;
import com.xwms.core.label.service.InventoryLabelService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/** 库存标签/条码管理 Controller */
@Tag(name = "库存标签/条码管理", description = "标签模板/条码规则/标签任务/条码记录")
@RestController
@RequestMapping("/api/label")
@RequiredArgsConstructor
public class InventoryLabelController {

    private final InventoryLabelService labelService;

    // ============================================================

    // 标签模板
    // ============================================================

    @Operation(summary = "创建标签模板")
    @PostMapping("/template")
    public Result<LabelTemplate> createTemplate(@RequestBody LabelTemplate template) {
        return Result.success(labelService.createTemplate(template));
    }

    @Operation(summary = "按编码查询标签模板")
    @GetMapping("/template/{templateCode}")
    public Result<LabelTemplate> getTemplateByCode(@PathVariable String templateCode) {
        return Result.success(labelService.getTemplateByCode(templateCode));
    }

    @Operation(summary = "按类型查询标签模板")
    @GetMapping("/template/type/{templateType}")
    public Result<List<LabelTemplate>> getTemplatesByType(@PathVariable String templateType) {
        return Result.success(labelService.getTemplatesByType(templateType));
    }

    @Operation(summary = "按仓库查询标签模板")
    @GetMapping("/template/warehouse/{warehouseCode}")
    public Result<List<LabelTemplate>> getTemplatesByWarehouse(@PathVariable String warehouseCode) {
        return Result.success(labelService.getTemplatesByWarehouse(warehouseCode));
    }

    @Operation(summary = "分页查询标签模板")
    @GetMapping("/template/list")
    public Result<Page<LabelTemplate>> pageTemplates(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String templateType,
            @RequestParam(required = false) String warehouseCode) {
        return Result.success(
                labelService.pageTemplates(new Page<>(page, size), templateType, warehouseCode));
    }

    // ============================================================

    // 条码规则
    // ============================================================

    @Operation(summary = "创建条码规则")
    @PostMapping("/rule")
    public Result<BarcodeRule> createRule(@RequestBody BarcodeRule rule) {
        return Result.success(labelService.createRule(rule));
    }

    @Operation(summary = "按编码查询条码规则")
    @GetMapping("/rule/{ruleCode}")
    public Result<BarcodeRule> getRuleByCode(@PathVariable String ruleCode) {
        return Result.success(labelService.getRuleByCode(ruleCode));
    }

    @Operation(summary = "按类型查询条码规则")
    @GetMapping("/rule/type/{barcodeType}")
    public Result<List<BarcodeRule>> getRulesByType(@PathVariable String barcodeType) {
        return Result.success(labelService.getRulesByType(barcodeType));
    }

    @Operation(summary = "按仓库和类型查询条码规则")
    @GetMapping("/rule/warehouse-type")
    public Result<BarcodeRule> getRuleByWarehouseAndType(
            @RequestParam String warehouseCode, @RequestParam String barcodeType) {
        return Result.success(labelService.getRuleByWarehouseAndType(warehouseCode, barcodeType));
    }

    @Operation(summary = "分页查询条码规则")
    @GetMapping("/rule/list")
    public Result<Page<BarcodeRule>> pageRules(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String barcodeType,
            @RequestParam(required = false) String warehouseCode) {
        return Result.success(
                labelService.pageRules(new Page<>(page, size), barcodeType, warehouseCode));
    }

    // ============================================================

    // 条码生成与解析
    // ============================================================

    @Operation(summary = "生成条码")
    @PostMapping("/barcode/generate")
    public Result<BarcodeRecord> generateBarcode(
            @RequestParam String ruleCode,
            @RequestParam(required = false) String warehouseCode,
            @RequestParam(required = false) String ownerCode,
            @RequestParam String bizKey,
            @RequestParam(required = false) String bizType,
            @RequestParam String operator) {
        return Result.success(
                labelService.generateBarcode(
                        ruleCode, warehouseCode, ownerCode, bizKey, bizType, operator));
    }

    @Operation(summary = "解析条码")
    @PostMapping("/barcode/parse")
    public Result<BarcodeRecord> parseBarcode(@RequestParam String barcode) {
        return Result.success(labelService.parseBarcode(barcode));
    }

    // ============================================================

    // 标签任务
    // ============================================================

    @Operation(summary = "创建标签任务")
    @PostMapping("/task")
    public Result<LabelTask> createTask(
            @RequestParam(required = false) String taskName,
            @RequestParam String templateCode,
            @RequestParam(required = false) String warehouseCode,
            @RequestParam(required = false) String ownerCode,
            @RequestParam String bizType,
            @RequestParam(required = false) String bizNo,
            @RequestParam int totalCount,
            @RequestParam(required = false) String printer,
            @RequestParam(required = false) String printMode,
            @RequestParam String operator) {
        return Result.success(
                labelService.createTask(
                        taskName,
                        templateCode,
                        warehouseCode,
                        ownerCode,
                        bizType,
                        bizNo,
                        totalCount,
                        printer,
                        printMode,
                        operator));
    }

    @Operation(summary = "开始标签任务")
    @PostMapping("/task/{taskId}/start")
    public Result<LabelTask> startTask(@PathVariable String taskId) {
        return Result.success(labelService.startTask(taskId));
    }

    @Operation(summary = "完成标签任务")
    @PostMapping("/task/{taskId}/complete")
    public Result<LabelTask> completeTask(
            @PathVariable String taskId,
            @RequestParam int printedCount,
            @RequestParam int failedCount,
            @RequestParam Long durationMs,
            @RequestParam(required = false) String errorMessage) {
        return Result.success(
                labelService.completeTask(
                        taskId, printedCount, failedCount, durationMs, errorMessage));
    }

    @Operation(summary = "取消标签任务")
    @PostMapping("/task/{taskId}/cancel")
    public Result<LabelTask> cancelTask(
            @PathVariable String taskId,
            @RequestParam String reason,
            @RequestParam String operator) {
        return Result.success(labelService.cancelTask(taskId, reason, operator));
    }

    @Operation(summary = "按ID查询标签任务")
    @GetMapping("/task/{taskId}")
    public Result<LabelTask> getTaskById(@PathVariable String taskId) {
        return Result.success(labelService.getTaskById(taskId));
    }

    @Operation(summary = "按状态查询标签任务")
    @GetMapping("/task/status/{status}")
    public Result<List<LabelTask>> getTasksByStatus(@PathVariable String status) {
        return Result.success(labelService.getTasksByStatus(status));
    }

    @Operation(summary = "按模板查询最近标签任务")
    @GetMapping("/task/recent/{templateCode}")
    public Result<List<LabelTask>> getRecentTasksByTemplate(
            @PathVariable String templateCode, @RequestParam(defaultValue = "10") int limit) {
        return Result.success(labelService.getRecentTasksByTemplate(templateCode, limit));
    }

    @Operation(summary = "分页查询标签任务")
    @GetMapping("/task/list")
    public Result<Page<LabelTask>> pageTasks(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String templateCode,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String warehouseCode,
            @RequestParam(required = false) String bizType) {
        return Result.success(
                labelService.pageTasks(
                        new Page<>(page, size), templateCode, status, warehouseCode, bizType));
    }

    // ============================================================

    // 条码记录
    // ============================================================

    @Operation(summary = "按条码查询记录")
    @GetMapping("/barcode/{barcode}")
    public Result<BarcodeRecord> getRecordByBarcode(@PathVariable String barcode) {
        return Result.success(labelService.getRecordByBarcode(barcode));
    }

    @Operation(summary = "按类型和业务主键查询条码记录")
    @GetMapping("/barcode/type-bizkey")
    public Result<List<BarcodeRecord>> getRecordsByTypeAndBizKey(
            @RequestParam String barcodeType, @RequestParam String bizKey) {
        return Result.success(labelService.getRecordsByTypeAndBizKey(barcodeType, bizKey));
    }

    @Operation(summary = "按仓库和类型查询最近条码记录")
    @GetMapping("/barcode/recent")
    public Result<List<BarcodeRecord>> getRecentRecordsByWarehouseAndType(
            @RequestParam String warehouseCode,
            @RequestParam String barcodeType,
            @RequestParam(defaultValue = "10") int limit) {
        return Result.success(
                labelService.getRecentRecordsByWarehouseAndType(warehouseCode, barcodeType, limit));
    }

    @Operation(summary = "记录打印")
    @PostMapping("/barcode/{recordId}/print")
    public Result<Integer> recordPrint(@PathVariable String recordId) {
        return Result.success(labelService.recordPrint(recordId));
    }

    @Operation(summary = "记录扫描")
    @PostMapping("/barcode/{recordId}/scan")
    public Result<Integer> recordScan(@PathVariable String recordId) {
        return Result.success(labelService.recordScan(recordId));
    }

    @Operation(summary = "更新条码记录状态")
    @PostMapping("/barcode/{recordId}/status")
    public Result<Integer> updateRecordStatus(
            @PathVariable String recordId, @RequestParam String status) {
        return Result.success(labelService.updateRecordStatus(recordId, status));
    }

    @Operation(summary = "分页查询条码记录")
    @GetMapping("/barcode/list")
    public Result<Page<BarcodeRecord>> pageRecords(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String barcodeType,
            @RequestParam(required = false) String warehouseCode,
            @RequestParam(required = false) String bizKey,
            @RequestParam(required = false) String status) {
        return Result.success(
                labelService.pageRecords(
                        new Page<>(page, size), barcodeType, warehouseCode, bizKey, status));
    }
}
