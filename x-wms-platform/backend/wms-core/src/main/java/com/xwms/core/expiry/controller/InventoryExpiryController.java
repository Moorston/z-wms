package com.xwms.core.expiry.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.common.core.Result;
import com.xwms.core.expiry.entity.*;
import com.xwms.core.expiry.service.InventoryExpiryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/** 库存效期管理 Controller */
@Tag(name = "库存效期管理", description = "效期规则/效期批次/临期预警/过期处理/FEFO")
@RestController
@RequestMapping("/api/expiry")
@RequiredArgsConstructor
public class InventoryExpiryController {

    private final InventoryExpiryService expiryService;

    // ============================================================

    // 效期规则
    // ============================================================

    @Operation(summary = "创建效期规则")
    @PostMapping("/rule")
    public Result<ExpiryRule> createRule(@RequestBody ExpiryRule rule) {
        return Result.success(expiryService.createRule(rule));
    }

    @Operation(summary = "按编码查询效期规则")
    @GetMapping("/rule/{ruleCode}")
    public Result<ExpiryRule> getRuleByCode(@PathVariable String ruleCode) {
        return Result.success(expiryService.getRuleByCode(ruleCode));
    }

    @Operation(summary = "匹配效期规则")
    @GetMapping("/rule/match")
    public Result<ExpiryRule> matchRule(
            @RequestParam String warehouseCode,
            @RequestParam(required = false) String ownerCode,
            @RequestParam(required = false) String categoryCode,
            @RequestParam(required = false) String skuCode) {
        return Result.success(
                expiryService.matchRule(warehouseCode, ownerCode, categoryCode, skuCode));
    }

    @Operation(summary = "分页查询效期规则")
    @GetMapping("/rule/list")
    public Result<Page<ExpiryRule>> pageRules(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String warehouseCode) {
        return Result.success(expiryService.pageRules(new Page<>(page, size), warehouseCode));
    }

    // ============================================================

    // 效期批次
    // ============================================================

    @Operation(summary = "注册效期批次")
    @PostMapping("/batch/register")
    public Result<ExpiryBatch> registerBatch(
            @RequestParam String batchNo,
            @RequestParam String warehouseCode,
            @RequestParam(required = false) String ownerCode,
            @RequestParam String skuCode,
            @RequestParam(required = false) String skuName,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd")
                    LocalDate productionDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd")
                    LocalDate expiryDate,
            @RequestParam BigDecimal totalQty,
            @RequestParam(required = false) String inboundNo,
            @RequestParam(required = false) String supplierCode,
            @RequestParam(required = false) String remark) {
        return Result.success(
                expiryService.registerBatch(
                        batchNo,
                        warehouseCode,
                        ownerCode,
                        skuCode,
                        skuName,
                        productionDate,
                        expiryDate,
                        totalQty,
                        inboundNo,
                        supplierCode,
                        remark));
    }

    @Operation(summary = "批量更新效期状态")
    @PostMapping("/batch/update-status")
    public Result<Integer> batchUpdateExpiryStatus(@RequestParam String warehouseCode) {
        return Result.success(expiryService.batchUpdateExpiryStatus(warehouseCode));
    }

    @Operation(summary = "查询效期批次")
    @GetMapping("/batch")
    public Result<ExpiryBatch> getBatch(
            @RequestParam String batchNo,
            @RequestParam String warehouseCode,
            @RequestParam String skuCode) {
        return Result.success(expiryService.getBatch(batchNo, warehouseCode, skuCode));
    }

    @Operation(summary = "查询即将过期的批次")
    @GetMapping("/batch/expiring/{warehouseCode}")
    public Result<List<ExpiryBatch>> getExpiringBatches(
            @PathVariable String warehouseCode, @RequestParam(defaultValue = "30") int withinDays) {
        return Result.success(expiryService.getExpiringBatches(warehouseCode, withinDays));
    }

    @Operation(summary = "查询预警批次")
    @GetMapping("/batch/warning/{warehouseCode}")
    public Result<List<ExpiryBatch>> getWarningBatches(@PathVariable String warehouseCode) {
        return Result.success(expiryService.getWarningBatches(warehouseCode));
    }

    @Operation(summary = "分页查询效期批次")
    @GetMapping("/batch/list")
    public Result<Page<ExpiryBatch>> pageBatches(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String warehouseCode,
            @RequestParam(required = false) String skuCode,
            @RequestParam(required = false) String expiryStatus) {
        return Result.success(
                expiryService.pageBatches(
                        new Page<>(page, size), warehouseCode, skuCode, expiryStatus));
    }

    // ============================================================

    // 临期预警
    // ============================================================

    @Operation(summary = "处理效期预警")
    @PostMapping("/alert/{alertNo}/handle")
    public Result<ExpiryAlert> handleAlert(
            @PathVariable String alertNo,
            @RequestParam String handleAction,
            @RequestParam String handledBy) {
        return Result.success(expiryService.handleAlert(alertNo, handleAction, handledBy));
    }

    @Operation(summary = "分页查询效期预警")
    @GetMapping("/alert/list")
    public Result<Page<ExpiryAlert>> pageAlerts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String warehouseCode,
            @RequestParam(required = false) String status) {
        return Result.success(
                expiryService.pageAlerts(new Page<>(page, size), warehouseCode, status));
    }

    @Operation(summary = "按预警单号查询")
    @GetMapping("/alert/{alertNo}")
    public Result<ExpiryAlert> getAlertByNo(@PathVariable String alertNo) {
        return Result.success(expiryService.getAlertByNo(alertNo));
    }

    // ============================================================

    // 过期处理
    // ============================================================

    @Operation(summary = "处理过期库存")
    @PostMapping("/handle")
    public Result<ExpiryHandleLog> handleExpiry(
            @RequestParam String batchNo,
            @RequestParam String warehouseCode,
            @RequestParam String skuCode,
            @RequestParam String handleType,
            @RequestParam BigDecimal handleQty,
            @RequestParam String handleReason,
            @RequestParam(required = false) String refNo,
            @RequestParam String operator) {
        return Result.success(
                expiryService.handleExpiry(
                        batchNo,
                        warehouseCode,
                        skuCode,
                        handleType,
                        handleQty,
                        handleReason,
                        refNo,
                        operator));
    }

    @Operation(summary = "按批次查询处理记录")
    @GetMapping("/handle/batch/{batchNo}")
    public Result<List<ExpiryHandleLog>> getHandleLogsByBatch(@PathVariable String batchNo) {
        return Result.success(expiryService.getHandleLogsByBatch(batchNo));
    }

    @Operation(summary = "按处理单号查询")
    @GetMapping("/handle/{handleNo}")
    public Result<ExpiryHandleLog> getHandleLogByNo(@PathVariable String handleNo) {
        return Result.success(expiryService.getHandleLogByNo(handleNo));
    }

    // ============================================================

    // FEFO
    // ============================================================

    @Operation(summary = "获取FEFO排序的批次")
    @GetMapping("/fefo/{skuCode}/{warehouseCode}")
    public Result<List<ExpiryBatch>> getFefoBatches(
            @PathVariable String skuCode, @PathVariable String warehouseCode) {
        return Result.success(expiryService.getFefoBatches(skuCode, warehouseCode));
    }

    @Operation(summary = "按FEFO分配库存")
    @PostMapping("/fefo/allocate")
    public Result<List<Map<String, Object>>> allocateByFefo(
            @RequestParam String skuCode,
            @RequestParam String warehouseCode,
            @RequestParam BigDecimal needQty) {
        return Result.success(expiryService.allocateByFefo(skuCode, warehouseCode, needQty));
    }
}
