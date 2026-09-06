package com.xwms.core.plugin.industry.gsp.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.common.core.Result;
import com.xwms.core.plugin.industry.gsp.entity.*;
import com.xwms.core.plugin.industry.gsp.service.GspService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/** GSP医药行业控制器 */
@Tag(name = "GSP医药行业管理", description = "批次追溯/质检记录/温度日志/证照管理")
@RestController
@RequestMapping("/api/gsp")
@RequiredArgsConstructor
public class GspController {

    private final GspService gspService;

    // ============================================================

    // 1. GSP批次追溯
    // ============================================================

    @Operation(summary = "创建GSP批次记录")
    @PostMapping("/batch")
    public Result<GspBatchRecord> createBatchRecord(@RequestBody GspBatchRecord record) {
        return Result.success(gspService.createBatchRecord(record));
    }

    @Operation(summary = "更新批次质量状态")
    @PutMapping("/batch/{id}/quality-status")
    public Result<GspBatchRecord> updateQualityStatus(
            @PathVariable Long id,
            @RequestParam String qualityStatus,
            @RequestParam String operator) {
        return Result.success(gspService.updateQualityStatus(id, qualityStatus, operator));
    }

    @Operation(summary = "更新批次养护状态")
    @PutMapping("/batch/{id}/maintenance-status")
    public Result<GspBatchRecord> updateMaintenanceStatus(
            @PathVariable Long id,
            @RequestParam String maintenanceStatus,
            @RequestParam String operator) {
        return Result.success(gspService.updateMaintenanceStatus(id, maintenanceStatus, operator));
    }

    @Operation(summary = "GSP批次出库")
    @PutMapping("/batch/{id}/outbound")
    public Result<GspBatchRecord> outboundBatch(
            @PathVariable Long id,
            @RequestParam BigDecimal outboundQty,
            @RequestParam String operator) {
        return Result.success(gspService.outboundBatch(id, outboundQty, operator));
    }

    @Operation(summary = "查询批次记录详情")
    @GetMapping("/batch/{id}")
    public Result<GspBatchRecord> getBatchRecord(@PathVariable Long id) {
        return Result.success(gspService.getBatchRecord(id));
    }

    @Operation(summary = "根据编号查询批次记录")
    @GetMapping("/batch/no/{recordNo}")
    public Result<GspBatchRecord> getBatchRecordByNo(@PathVariable String recordNo) {
        return Result.success(gspService.getBatchRecordByNo(recordNo));
    }

    @Operation(summary = "根据批次号查询批次记录列表")
    @GetMapping("/batch/batch-no/{batchNo}")
    public Result<List<GspBatchRecord>> getBatchRecordsByBatchNo(@PathVariable String batchNo) {
        return Result.success(gspService.getBatchRecordsByBatchNo(batchNo));
    }

    @Operation(summary = "根据SKU编码查询批次记录列表")
    @GetMapping("/batch/sku/{skuCode}")
    public Result<List<GspBatchRecord>> getBatchRecordsBySkuCode(@PathVariable String skuCode) {
        return Result.success(gspService.getBatchRecordsBySkuCode(skuCode));
    }

    @Operation(summary = "查询近效期批次")
    @GetMapping("/batch/near-expiry")
    public Result<List<GspBatchRecord>> getNearExpiryBatches(
            @RequestParam(defaultValue = "30") Integer days) {
        return Result.success(gspService.getNearExpiryBatches(days));
    }

    @Operation(summary = "查询已过期批次")
    @GetMapping("/batch/expired")
    public Result<List<GspBatchRecord>> getExpiredBatches() {
        return Result.success(gspService.getExpiredBatches());
    }

    @Operation(summary = "根据追溯码查询批次记录")
    @GetMapping("/batch/trace/{traceCode}")
    public Result<GspBatchRecord> getBatchRecordByTraceCode(@PathVariable String traceCode) {
        return Result.success(gspService.getBatchRecordByTraceCode(traceCode));
    }

    @Operation(summary = "分页查询批次记录")
    @GetMapping("/batch")
    public Result<Page<GspBatchRecord>> pageBatchRecords(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String warehouseCode,
            @RequestParam(required = false) String skuCode,
            @RequestParam(required = false) String batchNo,
            @RequestParam(required = false) String qualityStatus,
            @RequestParam(required = false) String maintenanceStatus) {
        return Result.success(
                gspService.pageBatchRecords(
                        new Page<>(page, size),
                        warehouseCode,
                        skuCode,
                        batchNo,
                        qualityStatus,
                        maintenanceStatus));
    }

    // ============================================================

    // 2. GSP质检记录
    // ============================================================

    @Operation(summary = "创建GSP质检记录")
    @PostMapping("/quality-check")
    public Result<GspQualityCheck> createQualityCheck(@RequestBody GspQualityCheck check) {
        return Result.success(gspService.createQualityCheck(check));
    }

    @Operation(summary = "开始质检")
    @PutMapping("/quality-check/{id}/start")
    public Result<GspQualityCheck> startQualityCheck(
            @PathVariable Long id, @RequestParam String checker) {
        return Result.success(gspService.startQualityCheck(id, checker));
    }

    @Operation(summary = "完成质检")
    @PutMapping("/quality-check/{id}/complete")
    public Result<GspQualityCheck> completeQualityCheck(
            @PathVariable Long id,
            @RequestParam String checkResult,
            @RequestParam BigDecimal qualifiedQty,
            @RequestParam BigDecimal unqualifiedQty,
            @RequestParam(required = false) String unqualifiedReason,
            @RequestParam String checker) {
        return Result.success(
                gspService.completeQualityCheck(
                        id, checkResult, qualifiedQty, unqualifiedQty, unqualifiedReason, checker));
    }

    @Operation(summary = "质检复核")
    @PutMapping("/quality-check/{id}/review")
    public Result<GspQualityCheck> reviewQualityCheck(
            @PathVariable Long id,
            @RequestParam String reviewer,
            @RequestParam String reviewOpinion) {
        return Result.success(gspService.reviewQualityCheck(id, reviewer, reviewOpinion));
    }

    @Operation(summary = "质检审批")
    @PutMapping("/quality-check/{id}/approve")
    public Result<GspQualityCheck> approveQualityCheck(
            @PathVariable Long id,
            @RequestParam String approver,
            @RequestParam String handleOpinion) {
        return Result.success(gspService.approveQualityCheck(id, approver, handleOpinion));
    }

    @Operation(summary = "查询质检记录详情")
    @GetMapping("/quality-check/{id}")
    public Result<GspQualityCheck> getQualityCheck(@PathVariable Long id) {
        return Result.success(gspService.getQualityCheck(id));
    }

    @Operation(summary = "根据编号查询质检记录")
    @GetMapping("/quality-check/no/{checkNo}")
    public Result<GspQualityCheck> getQualityCheckByNo(@PathVariable String checkNo) {
        return Result.success(gspService.getQualityCheckByNo(checkNo));
    }

    @Operation(summary = "根据关联单号查询质检记录列表")
    @GetMapping("/quality-check/ref/{refNo}")
    public Result<List<GspQualityCheck>> getQualityChecksByRefNo(@PathVariable String refNo) {
        return Result.success(gspService.getQualityChecksByRefNo(refNo));
    }

    @Operation(summary = "根据批次号查询质检记录列表")
    @GetMapping("/quality-check/batch/{batchNo}")
    public Result<List<GspQualityCheck>> getQualityChecksByBatchNo(@PathVariable String batchNo) {
        return Result.success(gspService.getQualityChecksByBatchNo(batchNo));
    }

    @Operation(summary = "查询待处理质检记录")
    @GetMapping("/quality-check/pending")
    public Result<List<GspQualityCheck>> getPendingQualityChecks() {
        return Result.success(gspService.getPendingQualityChecks());
    }

    @Operation(summary = "分页查询质检记录")
    @GetMapping("/quality-check")
    public Result<Page<GspQualityCheck>> pageQualityChecks(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String warehouseCode,
            @RequestParam(required = false) String checkType,
            @RequestParam(required = false) String batchNo,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String checkResult) {
        return Result.success(
                gspService.pageQualityChecks(
                        new Page<>(page, size),
                        warehouseCode,
                        checkType,
                        batchNo,
                        status,
                        checkResult));
    }

    // ============================================================

    // 3. GSP温度日志
    // ============================================================

    @Operation(summary = "采集GSP温度数据")
    @PostMapping("/temperature/collect")
    public Result<GspTemperatureLog> collectTemperature(
            @RequestParam String warehouseCode,
            @RequestParam(required = false) String areaCode,
            @RequestParam(required = false) String locationCode,
            @RequestParam(required = false) String equipmentCode,
            @RequestParam BigDecimal temperature,
            @RequestParam(required = false) BigDecimal humidity,
            @RequestParam(required = false) String storageCondition,
            @RequestParam(defaultValue = "AUTO") String collectType,
            @RequestParam(required = false) String collector) {
        return Result.success(
                gspService.collectTemperature(
                        warehouseCode,
                        areaCode,
                        locationCode,
                        equipmentCode,
                        temperature,
                        humidity,
                        storageCondition,
                        collectType,
                        collector));
    }

    @Operation(summary = "处理温度超标报警")
    @PutMapping("/temperature/{id}/handle")
    public Result<GspTemperatureLog> handleTemperatureAlert(
            @PathVariable Long id,
            @RequestParam String handleMeasure,
            @RequestParam String handleResult,
            @RequestParam String handledBy) {
        return Result.success(
                gspService.handleTemperatureAlert(id, handleMeasure, handleResult, handledBy));
    }

    @Operation(summary = "查询温度日志详情")
    @GetMapping("/temperature/{id}")
    public Result<GspTemperatureLog> getTemperatureLog(@PathVariable Long id) {
        return Result.success(gspService.getTemperatureLog(id));
    }

    @Operation(summary = "根据编号查询温度日志")
    @GetMapping("/temperature/no/{logNo}")
    public Result<GspTemperatureLog> getTemperatureLogByNo(@PathVariable String logNo) {
        return Result.success(gspService.getTemperatureLogByNo(logNo));
    }

    @Operation(summary = "根据库区和时间范围查询温度日志")
    @GetMapping("/temperature/area/{areaCode}")
    public Result<List<GspTemperatureLog>> getTemperatureLogsByAreaAndTime(
            @PathVariable String areaCode,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        return Result.success(
                gspService.getTemperatureLogsByAreaAndTime(areaCode, startTime, endTime));
    }

    @Operation(summary = "查询温度超标记录")
    @GetMapping("/temperature/exceeded")
    public Result<List<GspTemperatureLog>> getExceededTemperatureLogs() {
        return Result.success(gspService.getExceededTemperatureLogs());
    }

    @Operation(summary = "查询待处理温度报警")
    @GetMapping("/temperature/pending-handle")
    public Result<List<GspTemperatureLog>> getPendingHandleTemperatureLogs() {
        return Result.success(gspService.getPendingHandleTemperatureLogs());
    }

    @Operation(summary = "分页查询温度日志")
    @GetMapping("/temperature")
    public Result<Page<GspTemperatureLog>> pageTemperatureLogs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String warehouseCode,
            @RequestParam(required = false) String areaCode,
            @RequestParam(required = false) String equipmentCode,
            @RequestParam(required = false) String tempStatus,
            @RequestParam(required = false) String handleStatus) {
        return Result.success(
                gspService.pageTemperatureLogs(
                        new Page<>(page, size),
                        warehouseCode,
                        areaCode,
                        equipmentCode,
                        tempStatus,
                        handleStatus));
    }

    // ============================================================

    // 4. GSP证照管理
    // ============================================================

    @Operation(summary = "创建GSP证照")
    @PostMapping("/license")
    public Result<GspLicense> createLicense(@RequestBody GspLicense license) {
        return Result.success(gspService.createLicense(license));
    }

    @Operation(summary = "更新GSP证照")
    @PutMapping("/license/{id}")
    public Result<GspLicense> updateLicense(
            @PathVariable Long id, @RequestBody GspLicense license) {
        license.setId(id);
        return Result.success(gspService.updateLicense(license));
    }

    @Operation(summary = "GSP证照换证")
    @PutMapping("/license/{id}/renew")
    public Result<GspLicense> renewLicense(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate newExpiryDate,
            @RequestParam String renewalDate,
            @RequestParam String operator) {
        return Result.success(gspService.renewLicense(id, newExpiryDate, renewalDate, operator));
    }

    @Operation(summary = "证照到期预警")
    @PutMapping("/license/{id}/warn")
    public Result<GspLicense> warnLicenseExpiry(@PathVariable Long id) {
        return Result.success(gspService.warnLicenseExpiry(id));
    }

    @Operation(summary = "查询证照详情")
    @GetMapping("/license/{id}")
    public Result<GspLicense> getLicense(@PathVariable Long id) {
        return Result.success(gspService.getLicense(id));
    }

    @Operation(summary = "根据编号查询证照")
    @GetMapping("/license/no/{licenseNo}")
    public Result<GspLicense> getLicenseByNo(@PathVariable String licenseNo) {
        return Result.success(gspService.getLicenseByNo(licenseNo));
    }

    @Operation(summary = "根据类型查询证照列表")
    @GetMapping("/license/type/{licenseType}")
    public Result<List<GspLicense>> getLicensesByType(@PathVariable String licenseType) {
        return Result.success(gspService.getLicensesByType(licenseType));
    }

    @Operation(summary = "查询即将到期的证照")
    @GetMapping("/license/expiring")
    public Result<List<GspLicense>> getExpiringLicenses(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate expiryDate) {
        return Result.success(gspService.getExpiringLicenses(expiryDate));
    }

    @Operation(summary = "查询已过期证照")
    @GetMapping("/license/expired")
    public Result<List<GspLicense>> getExpiredLicenses() {
        return Result.success(gspService.getExpiredLicenses());
    }

    @Operation(summary = "根据仓库查询证照列表")
    @GetMapping("/license/warehouse/{warehouseCode}")
    public Result<List<GspLicense>> getLicensesByWarehouse(@PathVariable String warehouseCode) {
        return Result.success(gspService.getLicensesByWarehouse(warehouseCode));
    }

    @Operation(summary = "根据货主查询证照列表")
    @GetMapping("/license/owner/{ownerCode}")
    public Result<List<GspLicense>> getLicensesByOwner(@PathVariable String ownerCode) {
        return Result.success(gspService.getLicensesByOwner(ownerCode));
    }

    @Operation(summary = "分页查询证照")
    @GetMapping("/license")
    public Result<Page<GspLicense>> pageLicenses(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String licenseType,
            @RequestParam(required = false) String warehouseCode,
            @RequestParam(required = false) String ownerCode,
            @RequestParam(required = false) String status) {
        return Result.success(
                gspService.pageLicenses(
                        new Page<>(page, size), licenseType, warehouseCode, ownerCode, status));
    }
}
