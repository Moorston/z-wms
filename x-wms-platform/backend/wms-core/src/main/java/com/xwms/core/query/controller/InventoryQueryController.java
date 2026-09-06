package com.xwms.core.query.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.common.core.Result;
import com.xwms.core.query.entity.*;
import com.xwms.core.query.service.InventoryQueryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/** 搴撳瓨鏌ヨ/鎶ヨ〃绠＄悊 Controller */
@Tag(name = "搴撳瓨鏌ヨ/鎶ヨ〃绠＄悊", description = "瀹炴椂搴撳瓨/搴撳瓨蹇収/搴撳瓨鏃ユ姤/搴撳瓨鏈堟姤/搴撳瓨鍒嗘瀽")
@RestController
@RequestMapping("/api/inventory/query")
@RequiredArgsConstructor
public class InventoryQueryController {

    private final InventoryQueryService queryService;

    // ============================================================

    // 搴撳瓨鏃ユ姤
    // ============================================================

    @Operation(summary = "鐢熸垚搴撳瓨鏃ユ姤")
    @PostMapping("/daily/generate")
    public Result<Integer> generateDailyReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate reportDate,
            @RequestParam String warehouseCode,
            @RequestBody List<Map<String, Object>> dailyData) {
        return Result.success(
                queryService.generateDailyReport(reportDate, warehouseCode, dailyData));
    }

    @Operation(summary = "鎸夋棩鏈熷拰浠撳簱鏌ヨ鏃ユ姤")
    @GetMapping("/daily/date")
    public Result<List<InventoryDaily>> getDailyByDateAndWarehouse(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate reportDate,
            @RequestParam String warehouseCode) {
        return Result.success(queryService.getDailyByDateAndWarehouse(reportDate, warehouseCode));
    }

    @Operation(summary = "鎸塖KU鍜屾棩鏈熻寖鍥存煡璇㈡棩鎶")
    @GetMapping("/daily/sku")
    public Result<List<InventoryDaily>> getDailyBySkuAndDateRange(
            @RequestParam String skuCode,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return Result.success(queryService.getDailyBySkuAndDateRange(skuCode, startDate, endDate));
    }

    @Operation(summary = "鎸夋棩鏈熷拰SKU鏌ヨ鏃ユ姤")
    @GetMapping("/daily/detail")
    public Result<InventoryDaily> getDailyByDateAndSku(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate reportDate,
            @RequestParam String skuCode,
            @RequestParam String warehouseCode) {
        return Result.success(
                queryService.getDailyByDateAndSku(reportDate, skuCode, warehouseCode));
    }

    @Operation(summary = "鍒嗛〉鏌ヨ搴撳瓨鏃ユ姤")
    @GetMapping("/daily/list")
    public Result<Page<InventoryDaily>> pageDaily(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate reportDate,
            @RequestParam(required = false) String warehouseCode,
            @RequestParam(required = false) String skuCode) {
        return Result.success(
                queryService.pageDaily(new Page<>(page, size), reportDate, warehouseCode, skuCode));
    }

    // ============================================================

    // 搴撳瓨鏈堟姤
    // ============================================================

    @Operation(summary = "鐢熸垚搴撳瓨鏈堟姤")
    @PostMapping("/monthly/generate")
    public Result<Integer> generateMonthlyReport(
            @RequestParam String reportMonth,
            @RequestParam String warehouseCode,
            @RequestBody List<Map<String, Object>> monthlyData) {
        return Result.success(
                queryService.generateMonthlyReport(reportMonth, warehouseCode, monthlyData));
    }

    @Operation(summary = "鎸夋湀浠藉拰浠撳簱鏌ヨ鏈堟姤")
    @GetMapping("/monthly/month")
    public Result<List<InventoryMonthly>> getMonthlyByMonthAndWarehouse(
            @RequestParam String reportMonth, @RequestParam String warehouseCode) {
        return Result.success(
                queryService.getMonthlyByMonthAndWarehouse(reportMonth, warehouseCode));
    }

    @Operation(summary = "鎸塖KU鍜屾湀浠借寖鍥存煡璇㈡湀鎶")
    @GetMapping("/monthly/sku")
    public Result<List<InventoryMonthly>> getMonthlyBySkuAndMonthRange(
            @RequestParam String skuCode,
            @RequestParam String startMonth,
            @RequestParam String endMonth) {
        return Result.success(
                queryService.getMonthlyBySkuAndMonthRange(skuCode, startMonth, endMonth));
    }

    @Operation(summary = "鎸夋湀浠藉拰SKU鏌ヨ鏈堟姤")
    @GetMapping("/monthly/detail")
    public Result<InventoryMonthly> getMonthlyByMonthAndSku(
            @RequestParam String reportMonth,
            @RequestParam String skuCode,
            @RequestParam String warehouseCode) {
        return Result.success(
                queryService.getMonthlyByMonthAndSku(reportMonth, skuCode, warehouseCode));
    }

    @Operation(summary = "鍒嗛〉鏌ヨ搴撳瓨鏈堟姤")
    @GetMapping("/monthly/list")
    public Result<Page<InventoryMonthly>> pageMonthly(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String reportMonth,
            @RequestParam(required = false) String warehouseCode,
            @RequestParam(required = false) String skuCode) {
        return Result.success(
                queryService.pageMonthly(
                        new Page<>(page, size), reportMonth, warehouseCode, skuCode));
    }

    // ============================================================

    // 搴撳瓨鍒嗘瀽鎸囨爣
    // ============================================================

    @Operation(summary = "璁＄畻搴撳瓨鍛ㄨ浆鐜")
    @GetMapping("/metric/turnover-rate")
    public Result<BigDecimal> calculateTurnoverRate(
            @RequestParam BigDecimal outboundCost, @RequestParam BigDecimal avgInventoryCost) {
        return Result.success(queryService.calculateTurnoverRate(outboundCost, avgInventoryCost));
    }

    @Operation(summary = "璁＄畻搴撳瓨鍛ㄨ浆澶╂暟")
    @GetMapping("/metric/turnover-days")
    public Result<BigDecimal> calculateTurnoverDays(@RequestParam BigDecimal turnoverRate) {
        return Result.success(queryService.calculateTurnoverDays(turnoverRate));
    }

    @Operation(summary = "计算库存准确率")
    @GetMapping("/metric/accuracy")
    public Result<BigDecimal> calculateAccuracyRate(
            @RequestParam int totalSku, @RequestParam int diffSku) {
        return Result.success(queryService.calculateAccuracyRate(totalSku, diffSku));
    }

    @Operation(summary = "淇濆瓨搴撳瓨鍒嗘瀽鎸囨爣")
    @PostMapping("/metric/save")
    public Result<Integer> saveMetrics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate metricDate,
            @RequestParam String metricType,
            @RequestParam String warehouseCode,
            @RequestBody List<Map<String, Object>> metricsData) {
        return Result.success(
                queryService.saveMetrics(metricDate, metricType, warehouseCode, metricsData));
    }

    @Operation(summary = "鎸夋棩鏈熷拰绫诲瀷鏌ヨ鎸囨爣")
    @GetMapping("/metric/date")
    public Result<List<InventoryMetric>> getMetricsByDateAndType(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate metricDate,
            @RequestParam String metricType,
            @RequestParam String warehouseCode) {
        return Result.success(
                queryService.getMetricsByDateAndType(metricDate, metricType, warehouseCode));
    }

    @Operation(summary = "按SKU和日期范围查询指标")
    @GetMapping("/metric/sku")
    public Result<List<InventoryMetric>> getMetricsBySkuAndDateRange(
            @RequestParam String skuCode,
            @RequestParam String metricType,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return Result.success(
                queryService.getMetricsBySkuAndDateRange(skuCode, metricType, startDate, endDate));
    }

    @Operation(summary = "鎸夌瓑绾ф煡璇㈡寚鏍")
    @GetMapping("/metric/level")
    public Result<List<InventoryMetric>> getMetricsByLevel(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate metricDate,
            @RequestParam String metricType,
            @RequestParam String levelCode) {
        return Result.success(queryService.getMetricsByLevel(metricDate, metricType, levelCode));
    }

    @Operation(summary = "鍒嗛〉鏌ヨ搴撳瓨鎸囨爣")
    @GetMapping("/metric/list")
    public Result<Page<InventoryMetric>> pageMetrics(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate metricDate,
            @RequestParam(required = false) String metricType,
            @RequestParam(required = false) String warehouseCode,
            @RequestParam(required = false) String skuCode) {
        return Result.success(
                queryService.pageMetrics(
                        new Page<>(page, size), metricDate, metricType, warehouseCode, skuCode));
    }

    // ============================================================

    // 搴撳瓨姹囨€荤粺璁?
    // ============================================================

    @Operation(summary = "鎸夌淮搴︽眹鎬诲簱瀛")
    @PostMapping("/summarize")
    public Result<Map<String, Object>> summarizeInventory(
            @RequestParam String dimension, @RequestBody List<Map<String, Object>> inventoryList) {
        return Result.success(queryService.summarizeInventory(inventoryList, dimension));
    }

    @Operation(summary = "搴撳瓨ABC鍒嗙被")
    @PostMapping("/abc-classify")
    public Result<List<Map<String, Object>>> classifyABC(
            @RequestParam(defaultValue = "80") BigDecimal thresholdA,
            @RequestParam(defaultValue = "95") BigDecimal thresholdB,
            @RequestBody List<Map<String, Object>> skuList) {
        return Result.success(queryService.classifyABC(skuList, thresholdA, thresholdB));
    }
}
