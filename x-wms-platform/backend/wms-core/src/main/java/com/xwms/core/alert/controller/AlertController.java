package com.xwms.core.alert.controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.common.core.Result;
import com.xwms.core.alert.entity.*;
import com.xwms.core.alert.service.AlertService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/** 棰勮绠＄悊 Controller */
@Tag(name = "棰勮绠＄悊", description = "棰勮瑙勫垯/棰勮璁板綍/棰勮閫氱煡/棰勮澶勭悊")
@RestController
@RequestMapping("/api/alert")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;

    // ============================================================

    // 棰勮瑙勫垯
    // ============================================================

    @Operation(summary = "鍒涘缓棰勮瑙勫垯")
    @PostMapping("/rule")
    public Result<AlertRule> createRule(@RequestBody AlertRule rule) {
        return Result.success(alertService.createRule(rule));
    }

    @Operation(summary = "鏇存柊棰勮瑙勫垯")
    @PutMapping("/rule")
    public Result<AlertRule> updateRule(@RequestBody AlertRule rule) {
        return Result.success(alertService.updateRule(rule));
    }

    @Operation(summary = "鍒嗛〉鏌ヨ棰勮瑙勫垯")
    @GetMapping("/rule")
    public Result<Page<AlertRule>> pageRules(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String alertCategory,
            @RequestParam(required = false) String alertType,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) Integer enabled) {
        return Result.success(
                alertService.pageRules(
                        new Page<>(page, size), alertCategory, alertType, severity, enabled));
    }

    @Operation(summary = "鎸夊垎绫绘煡璇㈠惎鐢ㄨ鍒")
    @GetMapping("/rule/category/{category}/enabled")
    public Result<List<AlertRule>> getEnabledRulesByCategory(@PathVariable String category) {
        return Result.success(alertService.getEnabledRulesByCategory(category));
    }

    @Operation(summary = "鏌ヨ鎵€鏈夊惎鐢ㄨ鍒")
    @GetMapping("/rule/enabled")
    public Result<List<AlertRule>> getAllEnabledRules() {
        return Result.success(alertService.getAllEnabledRules());
    }

    // ============================================================

    // 棰勮璁板綍
    // ============================================================

    @Operation(summary = "瑙﹀彂棰勮")
    @PostMapping("/trigger")
    public Result<AlertRecord> triggerAlert(
            @RequestParam String ruleCode,
            @RequestParam String title,
            @RequestParam(required = false) String content,
            @RequestParam(required = false) String businessType,
            @RequestParam(required = false) String businessNo,
            @RequestParam(required = false) String warehouseCode,
            @RequestParam(required = false) String locationCode,
            @RequestParam(required = false) String skuCode,
            @RequestParam(required = false) BigDecimal currentValue,
            @RequestParam(required = false) BigDecimal thresholdValue,
            @RequestParam(required = false) String severity) {
        return Result.success(
                alertService.triggerAlert(
                        ruleCode,
                        title,
                        content,
                        businessType,
                        businessNo,
                        warehouseCode,
                        locationCode,
                        skuCode,
                        currentValue,
                        thresholdValue,
                        severity));
    }

    @Operation(summary = "鍒嗛〉鏌ヨ棰勮")
    @GetMapping
    public Result<Page<AlertRecord>> pageAlerts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String alertCategory,
            @RequestParam(required = false) String alertType,
            @RequestParam(required = false) String warehouseCode) {
        return Result.success(
                alertService.pageAlerts(
                        new Page<>(page, size),
                        status,
                        severity,
                        alertCategory,
                        alertType,
                        warehouseCode));
    }

    @Operation(summary = "鏌ヨ寰呭鐞嗛璀")
    @GetMapping("/pending")
    public Result<List<AlertRecord>> getPendingAlerts() {
        return Result.success(alertService.getPendingAlerts());
    }

    @Operation(summary = "鏌ヨ棰勮璇︽儏")
    @GetMapping("/{id}")
    public Result<AlertRecord> getAlertById(@PathVariable Long id) {
        return Result.success(alertService.getAlertById(id));
    }

    @Operation(summary = "纭棰勮")
    @PutMapping("/{id}/ack")
    public Result<AlertRecord> ackAlert(
            @PathVariable Long id,
            @RequestParam String handleBy,
            @RequestParam String handleName,
            @RequestParam(required = false) String remark) {
        return Result.success(alertService.ackAlert(id, handleBy, handleName, remark));
    }

    @Operation(summary = "瑙ｅ喅棰勮")
    @PutMapping("/{id}/resolve")
    public Result<AlertRecord> resolveAlert(
            @PathVariable Long id,
            @RequestParam String handleBy,
            @RequestParam String handleName,
            @RequestParam(required = false) String remark) {
        return Result.success(alertService.resolveAlert(id, handleBy, handleName, remark));
    }

    @Operation(summary = "蹇界暐棰勮")
    @PutMapping("/{id}/ignore")
    public Result<AlertRecord> ignoreAlert(
            @PathVariable Long id,
            @RequestParam String handleBy,
            @RequestParam String handleName,
            @RequestParam(required = false) String remark) {
        return Result.success(alertService.ignoreAlert(id, handleBy, handleName, remark));
    }

    @Operation(summary = "鏌ヨ棰勮澶勭悊璁板綍")
    @GetMapping("/{id}/handles")
    public Result<List<AlertHandle>> getHandlesByAlertId(@PathVariable Long id) {
        return Result.success(alertService.getHandlesByAlertId(id));
    }
}
