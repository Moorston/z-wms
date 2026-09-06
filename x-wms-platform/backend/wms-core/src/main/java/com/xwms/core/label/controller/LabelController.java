package com.xwms.core.label.controller;

import java.math.BigDecimal;
import java.util.Map;

import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.common.core.Result;
import com.xwms.core.label.entity.*;
import com.xwms.core.label.service.LabelService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/** 鏍囩鎵撳嵃涓庢潯鐮佺鐞?Controller */
@Tag(name = "标签打印与条码管理", description = "条码规则/条码生成/标签模板/打印任务")
@RestController
@RequestMapping("/api/label")
@RequiredArgsConstructor
public class LabelController {

    private final LabelService labelService;

    // ============================================================

    // 鏉＄爜瑙勫垯
    // ============================================================

    @Operation(summary = "鍒涘缓鏉＄爜瑙勫垯")
    @PostMapping("/barcode-rules")
    public Result<BarcodeRule> createBarcodeRule(@RequestBody BarcodeRule rule) {
        return Result.success(labelService.createBarcodeRule(rule));
    }

    @Operation(summary = "鏌ヨ鏉＄爜瑙勫垯璇︽儏")
    @GetMapping("/barcode-rules/{id}")
    public Result<BarcodeRule> getBarcodeRule(@PathVariable Long id) {
        return Result.success(labelService.getBarcodeRule(id));
    }

    @Operation(summary = "鍒嗛〉鏌ヨ鏉＄爜瑙勫垯")
    @GetMapping("/barcode-rules")
    public Result<Page<BarcodeRule>> pageBarcodeRules(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String type) {
        return Result.success(labelService.pageBarcodeRules(new Page<>(page, size), type));
    }

    // ============================================================

    // 鏉＄爜鐢熸垚
    // ============================================================

    @Operation(summary = "鐢熸垚鏉＄爜")
    @PostMapping("/barcode/generate")
    public Result<String> generateBarcode(
            @RequestParam String barcodeType,
            @RequestParam(required = false) String businessType,
            @RequestParam(required = false) String businessNo,
            @RequestParam(required = false) String sku,
            @RequestParam(required = false) String batchNo,
            @RequestParam(required = false) BigDecimal quantity,
            @RequestParam String generatedBy) {
        return Result.success(
                labelService.generateBarcode(
                        barcodeType,
                        businessType,
                        businessNo,
                        sku,
                        batchNo,
                        quantity,
                        generatedBy));
    }

    @Operation(summary = "鏌ヨ鏉＄爜璁板綍")
    @GetMapping("/barcode/{barcode}")
    public Result<BarcodeRecord> getBarcodeRecord(@PathVariable String barcode) {
        return Result.success(labelService.getBarcodeRecord(barcode));
    }

    @Operation(summary = "浣跨敤鏉＄爜(鏍囪宸蹭娇鐢?")
    @PutMapping("/barcode/{barcode}/use")
    public Result<Void> useBarcode(@PathVariable String barcode) {
        labelService.useBarcode(barcode);
        return Result.success();
    }

    // ============================================================

    // 鏍囩妯℃澘
    // ============================================================

    @Operation(summary = "鍒涘缓鏍囩妯℃澘")
    @PostMapping("/templates")
    public Result<LabelTemplate> createLabelTemplate(@RequestBody LabelTemplate template) {
        return Result.success(labelService.createLabelTemplate(template));
    }

    @Operation(summary = "鏌ヨ鏍囩妯℃澘璇︽儏")
    @GetMapping("/templates/{id}")
    public Result<LabelTemplate> getLabelTemplate(@PathVariable Long id) {
        return Result.success(labelService.getLabelTemplate(id));
    }

    @Operation(summary = "鍒嗛〉鏌ヨ鏍囩妯℃澘")
    @GetMapping("/templates")
    public Result<Page<LabelTemplate>> pageLabelTemplates(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String type) {
        return Result.success(labelService.pageLabelTemplates(new Page<>(page, size), type));
    }

    @Operation(summary = "娓叉煋鏍囩妯℃澘")
    @PostMapping("/templates/{code}/render")
    public Result<String> renderLabel(
            @PathVariable String code, @RequestBody Map<String, Object> variables) {
        return Result.success(labelService.renderLabel(code, variables));
    }

    // ============================================================
}
