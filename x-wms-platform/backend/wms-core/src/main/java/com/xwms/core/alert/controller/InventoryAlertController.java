package com.xwms.core.alert.controller;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.common.core.Result;
import com.xwms.core.alert.entity.*;
import com.xwms.core.alert.service.InventoryAlertService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/** 库存预警/告警管理 Controller */
@Tag(name = "库存预警/告警管理", description = "预警规则/预警检查/预警处理/预警通知")
@RestController
@RequestMapping("/api/alert")
@RequiredArgsConstructor
public class InventoryAlertController {

    private final InventoryAlertService alertService;

    // ============================================================

    // 预警规则
    // ============================================================

    @Operation(summary = "创建预警规则")
    @PostMapping("/rule")
    public Result<AlertRule> createRule(@RequestBody AlertRule rule) {
        return Result.success(alertService.createRule(rule));
    }

    @Operation(summary = "按编码查询预警规则")
    @GetMapping("/rule/{ruleCode}")
    public Result<AlertRule> getRuleByCode(@PathVariable String ruleCode) {
        return Result.success(alertService.getRuleByCode(ruleCode));
    }

    @Operation(summary = "按类型查询预警规则")
    @GetMapping("/rule/type/{alertType}")
    public Result<List<AlertRule>> getRulesByType(@PathVariable String alertType) {
        return Result.success(alertService.getRulesByType(alertType));
    }

    @Operation(summary = "按仓库查询预警规则")
    @GetMapping("/rule/warehouse/{warehouseCode}")
    public Result<List<AlertRule>> getRulesByWarehouse(@PathVariable String warehouseCode) {
        return Result.success(alertService.getRulesByWarehouse(warehouseCode));
    }

    @Operation(summary = "按频率查询预警规则")
    @GetMapping("/rule/frequency/{frequency}")
    public Result<List<AlertRule>> getRulesByFrequency(@PathVariable String frequency) {
        return Result.success(alertService.getRulesByFrequency(frequency));
    }

    @Operation(summary = "按SKU查询预警规则")
    @GetMapping("/rule/sku/{skuCode}")
    public Result<List<AlertRule>> getRulesBySku(@PathVariable String skuCode) {
        return Result.success(alertService.getRulesBySku(skuCode));
    }

    @Operation(summary = "分页查询预警规则")
    @GetMapping("/rule/list")
    public Result<Page<AlertRule>> pageRules(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String alertType,
            @RequestParam(required = false) String warehouseCode) {
        return Result.success(
                alertService.pageRules(new Page<>(page, size), alertType, warehouseCode));
    }

    // ============================================================

    // 预警检查
    // ============================================================

    @Operation(summary = "执行预警检查")
    @PostMapping("/check")
    public Result<List<AlertRecord>> checkAlerts(
            @RequestParam(defaultValue = "DAILY") String frequency,
            @RequestBody Map<String, Object> inventoryData) {
        return Result.success(alertService.checkAlerts(frequency, inventoryData));
    }

    // ============================================================

    // 预警处理
    // ============================================================

    @Operation(summary = "处理预警")
    @PostMapping("/handle/{alertId}")
    public Result<AlertRecord> handleAlert(
            @PathVariable String alertId,
            @RequestParam String action,
            @RequestParam String note,
            @RequestParam String operator) {
        return Result.success(alertService.handleAlert(alertId, action, note, operator));
    }

    @Operation(summary = "忽略预警")
    @PostMapping("/ignore/{alertId}")
    public Result<AlertRecord> ignoreAlert(
            @PathVariable String alertId,
            @RequestParam String reason,
            @RequestParam String operator) {
        return Result.success(alertService.ignoreAlert(alertId, reason, operator));
    }

    @Operation(summary = "升级预警")
    @PostMapping("/escalate/{alertId}")
    public Result<AlertRecord> escalateAlert(
            @PathVariable String alertId,
            @RequestParam String reason,
            @RequestParam String operator) {
        return Result.success(alertService.escalateAlert(alertId, reason, operator));
    }

    // ============================================================

    // 预警查询
    // ============================================================

    @Operation(summary = "按ID查询预警")
    @GetMapping("/{alertId}")
    public Result<AlertRecord> getAlertById(@PathVariable String alertId) {
        return Result.success(alertService.getAlertById(alertId));
    }

    @Operation(summary = "按状态查询预警")
    @GetMapping("/status/{status}")
    public Result<List<AlertRecord>> getAlertsByStatus(@PathVariable String status) {
        return Result.success(alertService.getAlertsByStatus(status));
    }

    @Operation(summary = "按类型查询活跃预警")
    @GetMapping("/active/type/{alertType}")
    public Result<List<AlertRecord>> getActiveAlertsByType(@PathVariable String alertType) {
        return Result.success(alertService.getActiveAlertsByType(alertType));
    }

    @Operation(summary = "按SKU查询最近预警")
    @GetMapping("/recent/sku/{skuCode}")
    public Result<List<AlertRecord>> getRecentAlertsBySku(
            @PathVariable String skuCode, @RequestParam(defaultValue = "10") int limit) {
        return Result.success(alertService.getRecentAlertsBySku(skuCode, limit));
    }

    @Operation(summary = "统计活跃预警数量")
    @GetMapping("/count/active")
    public Result<Integer> countActiveAlerts() {
        return Result.success(alertService.countActiveAlerts());
    }

    @Operation(summary = "按级别统计活跃预警")
    @GetMapping("/count/active/level/{level}")
    public Result<Integer> countActiveAlertsByLevel(@PathVariable String level) {
        return Result.success(alertService.countActiveAlertsByLevel(level));
    }

    @Operation(summary = "分页查询预警")
    @GetMapping("/list")
    public Result<Page<AlertRecord>> pageAlerts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String alertType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String warehouseCode,
            @RequestParam(required = false) String skuCode) {
        return Result.success(
                alertService.pageAlerts(
                        new Page<>(page, size), alertType, status, warehouseCode, skuCode));
    }

    @Operation(summary = "查询预警处理记录")
    @GetMapping("/handle/{alertId}")
    public Result<List<AlertHandle>> getHandlesByAlert(@PathVariable String alertId) {
        return Result.success(alertService.getHandlesByAlert(alertId));
    }

    // ============================================================

    // 预警统计
    // ============================================================

    @Operation(summary = "获取预警统计")
    @GetMapping("/statistics")
    public Result<Map<String, Object>> getAlertStatistics() {
        return Result.success(alertService.getAlertStatistics());
    }
}
