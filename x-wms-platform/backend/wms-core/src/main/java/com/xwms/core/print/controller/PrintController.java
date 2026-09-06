package com.xwms.core.print.controller;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.common.core.Result;
import com.xwms.core.print.entity.*;
import com.xwms.core.print.service.PrintService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/** 报表打印 Controller */
@Tag(name = "报表打印", description = "打印模板/打印任务/打印机/打印队列")
@RestController
@RequestMapping("/api/print")
@RequiredArgsConstructor
public class PrintController {

    private final PrintService printService;

    // ============================================================

    // 打印模板
    // ============================================================

    @Operation(summary = "创建打印模板")
    @PostMapping("/template")
    public Result<PrintTemplate> createTemplate(@RequestBody PrintTemplate template) {
        return Result.success(printService.createTemplate(template));
    }

    @Operation(summary = "更新打印模板")
    @PutMapping("/template")
    public Result<PrintTemplate> updateTemplate(@RequestBody PrintTemplate template) {
        return Result.success(printService.updateTemplate(template));
    }

    @Operation(summary = "分页查询打印模板")
    @GetMapping("/template")
    public Result<Page<PrintTemplate>> pageTemplates(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String templateType,
            @RequestParam(required = false) Integer enabled) {
        return Result.success(
                printService.pageTemplates(new Page<>(page, size), templateType, enabled));
    }

    @Operation(summary = "按类型查询模板")
    @GetMapping("/template/type/{type}")
    public Result<List<PrintTemplate>> getTemplatesByType(@PathVariable String type) {
        return Result.success(printService.getTemplatesByType(type));
    }

    @Operation(summary = "按编码查询模板")
    @GetMapping("/template/{code}")
    public Result<PrintTemplate> getTemplateByCode(@PathVariable String code) {
        return Result.success(printService.getTemplateByCode(code));
    }

    // ============================================================

    // 打印任务
    // ============================================================

    @Operation(summary = "创建打印任务")
    @PostMapping("/task")
    public Result<PrintTask> createPrintTask(@RequestBody PrintTask task) {
        return Result.success(
                printService.createPrintTask(
                        task.getTemplateCode(),
                        task.getBusinessType(),
                        task.getBusinessNo(),
                        task.getPrinterCode(),
                        task.getCopies(),
                        task.getPrintData(),
                        task.getCreatedBy()));
    }

    @Operation(summary = "分页查询打印任务")
    @GetMapping("/task")
    public Result<Page<PrintTask>> pagePrintTasks(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String businessType,
            @RequestParam(required = false) String businessNo,
            @RequestParam(required = false) String printerCode) {
        return Result.success(
                printService.pagePrintTasks(
                        new Page<>(page, size), status, businessType, businessNo, printerCode));
    }

    @Operation(summary = "查询业务打印任务")
    @GetMapping("/task/business")
    public Result<List<PrintTask>> getPrintTasksByBusiness(
            @RequestParam String businessType, @RequestParam String businessNo) {
        return Result.success(printService.getPrintTasksByBusiness(businessType, businessNo));
    }

    @Operation(summary = "取消打印任务")
    @PutMapping("/task/{id}/cancel")
    public Result<PrintTask> cancelPrintTask(@PathVariable Long id) {
        return Result.success(printService.cancelPrintTask(id));
    }

    @Operation(summary = "重试打印任务")
    @PutMapping("/task/{id}/retry")
    public Result<PrintTask> retryPrintTask(@PathVariable Long id) {
        return Result.success(printService.retryPrintTask(id));
    }

    // ============================================================

    // 打印机
    // ============================================================

    @Operation(summary = "创建打印机")
    @PostMapping("/printer")
    public Result<Printer> createPrinter(@RequestBody Printer printer) {
        return Result.success(printService.createPrinter(printer));
    }

    @Operation(summary = "分页查询打印机")
    @GetMapping("/printer")
    public Result<Page<Printer>> pagePrinters(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String warehouseCode,
            @RequestParam(required = false) String status) {
        return Result.success(
                printService.pagePrinters(new Page<>(page, size), warehouseCode, status));
    }

    @Operation(summary = "按仓库查询打印机")
    @GetMapping("/printer/warehouse/{whCode}")
    public Result<List<Printer>> getPrintersByWarehouse(@PathVariable String whCode) {
        return Result.success(printService.getPrintersByWarehouse(whCode));
    }

    @Operation(summary = "查询在线打印机")
    @GetMapping("/printer/online")
    public Result<List<Printer>> getOnlinePrinters() {
        return Result.success(printService.getOnlinePrinters());
    }

    @Operation(summary = "更新打印机状态")
    @PutMapping("/printer/{code}/status")
    public Result<Printer> updatePrinterStatus(
            @PathVariable String code, @RequestParam String status) {
        return Result.success(printService.updatePrinterStatus(code, status));
    }

    // ============================================================

    // 打印队列
    // ============================================================

    @Operation(summary = "查询打印机等待队列")
    @GetMapping("/queue/waiting/{printerCode}")
    public Result<List<PrintQueue>> getWaitingQueue(@PathVariable String printerCode) {
        return Result.success(printService.getWaitingQueue(printerCode));
    }

    @Operation(summary = "开始打印")
    @PutMapping("/queue/{id}/start")
    public Result<PrintQueue> startPrint(@PathVariable Long id) {
        return Result.success(printService.startPrint(id));
    }

    @Operation(summary = "完成打印")
    @PutMapping("/queue/{id}/finish")
    public Result<PrintQueue> finishPrint(
            @PathVariable Long id,
            @RequestParam(defaultValue = "true") boolean success,
            @RequestParam(required = false) String failReason) {
        return Result.success(printService.finishPrint(id, success, failReason));
    }
}
