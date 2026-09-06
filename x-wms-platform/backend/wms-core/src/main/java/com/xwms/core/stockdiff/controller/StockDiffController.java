package com.xwms.core.stockdiff.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.common.core.Result;
import com.xwms.core.stockdiff.entity.*;
import com.xwms.core.stockdiff.service.StockDiffService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/** 库存盘点差异管理 Controller */
@Tag(name = "库存盘点差异管理", description = "差异记录/差异处理/差异审批/差异分析")
@RestController
@RequestMapping("/api/stockdiff")
@RequiredArgsConstructor
public class StockDiffController {

    private final StockDiffService diffService;

    // ============================================================

    // 差异记录
    // ============================================================

    @Operation(summary = "创建差异记录")
    @PostMapping("/diff")
    public Result<StockDiff> createDiff(@RequestBody StockDiff diff) {
        return Result.success(diffService.createDiff(diff));
    }

    @Operation(summary = "按ID查询差异")
    @GetMapping("/diff/{diffId}")
    public Result<StockDiff> getDiffById(@PathVariable String diffId) {
        return Result.success(diffService.getDiffById(diffId));
    }

    @Operation(summary = "按盘点单查询差异")
    @GetMapping("/diff/stocktake/{stocktakeNo}")
    public Result<List<StockDiff>> getDiffsByStocktake(@PathVariable String stocktakeNo) {
        return Result.success(diffService.getDiffsByStocktake(stocktakeNo));
    }

    @Operation(summary = "按状态查询差异")
    @GetMapping("/diff/status/{status}")
    public Result<List<StockDiff>> getDiffsByStatus(@PathVariable String status) {
        return Result.success(diffService.getDiffsByStatus(status));
    }

    @Operation(summary = "按SKU查询最近差异")
    @GetMapping("/diff/recent/sku/{skuCode}")
    public Result<List<StockDiff>> getRecentDiffsBySku(
            @PathVariable String skuCode, @RequestParam(defaultValue = "10") int limit) {
        return Result.success(diffService.getRecentDiffsBySku(skuCode, limit));
    }

    @Operation(summary = "按库位查询活跃差异")
    @GetMapping("/diff/active/location/{locationCode}")
    public Result<List<StockDiff>> getActiveDiffsByLocation(@PathVariable String locationCode) {
        return Result.success(diffService.getActiveDiffsByLocation(locationCode));
    }

    @Operation(summary = "统计活跃差异数量")
    @GetMapping("/diff/count/active")
    public Result<Integer> countActiveDiffs() {
        return Result.success(diffService.countActiveDiffs());
    }

    @Operation(summary = "按类型统计活跃差异")
    @GetMapping("/diff/count/active/type/{diffType}")
    public Result<Integer> countActiveDiffsByType(@PathVariable String diffType) {
        return Result.success(diffService.countActiveDiffsByType(diffType));
    }

    @Operation(summary = "分页查询差异")
    @GetMapping("/diff/list")
    public Result<Page<StockDiff>> pageDiffs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String stocktakeNo,
            @RequestParam(required = false) String diffType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String warehouseCode,
            @RequestParam(required = false) String skuCode) {
        return Result.success(
                diffService.pageDiffs(
                        new Page<>(page, size),
                        stocktakeNo,
                        diffType,
                        status,
                        warehouseCode,
                        skuCode));
    }

    // ============================================================

    // 差异处理
    // ============================================================

    @Operation(summary = "处理差异")
    @PostMapping("/handle/{diffId}")
    public Result<StockDiff> handleDiff(
            @PathVariable String diffId,
            @RequestParam String handleType,
            @RequestParam String handleAction,
            @RequestParam(required = false) String handleNote,
            @RequestParam(required = false) String adjustNo,
            @RequestParam(required = false) BigDecimal adjustQty,
            @RequestParam String operator) {
        return Result.success(
                diffService.handleDiff(
                        diffId,
                        handleType,
                        handleAction,
                        handleNote,
                        adjustNo,
                        adjustQty,
                        operator));
    }

    @Operation(summary = "复盘差异")
    @PostMapping("/recount/{diffId}")
    public Result<StockDiff> recountDiff(
            @PathVariable String diffId,
            @RequestParam BigDecimal newCountedQty,
            @RequestParam String operator) {
        return Result.success(diffService.recountDiff(diffId, newCountedQty, operator));
    }

    @Operation(summary = "取消差异")
    @PostMapping("/cancel/{diffId}")
    public Result<StockDiff> cancelDiff(
            @PathVariable String diffId,
            @RequestParam String reason,
            @RequestParam String operator) {
        return Result.success(diffService.cancelDiff(diffId, reason, operator));
    }

    @Operation(summary = "查询差异处理记录")
    @GetMapping("/handle/{diffId}")
    public Result<List<StockDiffHandle>> getHandlesByDiff(@PathVariable String diffId) {
        return Result.success(diffService.getHandlesByDiff(diffId));
    }

    // ============================================================

    // 差异审批
    // ============================================================

    @Operation(summary = "提交审批")
    @PostMapping("/approve/submit/{diffId}")
    public Result<StockDiff> submitApprove(
            @PathVariable String diffId, @RequestParam String operator) {
        return Result.success(diffService.submitApprove(diffId, operator));
    }

    @Operation(summary = "审批差异")
    @PostMapping("/approve/{diffId}")
    public Result<StockDiff> approveDiff(
            @PathVariable String diffId,
            @RequestParam String approveNode,
            @RequestParam(required = false) String approveRole,
            @RequestParam String approver,
            @RequestParam String approveResult,
            @RequestParam(required = false) String approveNote) {
        return Result.success(
                diffService.approveDiff(
                        diffId, approveNode, approveRole, approver, approveResult, approveNote));
    }

    @Operation(summary = "查询差异审批记录")
    @GetMapping("/approve/{diffId}")
    public Result<List<StockDiffApprove>> getApprovesByDiff(@PathVariable String diffId) {
        return Result.success(diffService.getApprovesByDiff(diffId));
    }

    // ============================================================

    // 差异分析
    // ============================================================

    @Operation(summary = "生成差异分析")
    @PostMapping("/analysis/generate")
    public Result<StockDiffAnalysis> generateAnalysis(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate analysisDate,
            @RequestParam String analysisType,
            @RequestParam(required = false) String warehouseCode,
            @RequestBody List<StockDiff> diffs) {
        return Result.success(
                diffService.generateAnalysis(analysisDate, analysisType, warehouseCode, diffs));
    }

    @Operation(summary = "按ID查询差异分析")
    @GetMapping("/analysis/{analysisId}")
    public Result<StockDiffAnalysis> getAnalysisById(@PathVariable String analysisId) {
        return Result.success(diffService.getAnalysisById(analysisId));
    }

    @Operation(summary = "按日期和类型查询差异分析")
    @GetMapping("/analysis/date")
    public Result<List<StockDiffAnalysis>> getAnalysisByDateAndType(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate analysisDate,
            @RequestParam String analysisType) {
        return Result.success(diffService.getAnalysisByDateAndType(analysisDate, analysisType));
    }

    @Operation(summary = "按仓库和日期范围查询差异分析")
    @GetMapping("/analysis/warehouse")
    public Result<List<StockDiffAnalysis>> getAnalysisByWarehouseAndDateRange(
            @RequestParam String warehouseCode,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return Result.success(
                diffService.getAnalysisByWarehouseAndDateRange(warehouseCode, startDate, endDate));
    }

    @Operation(summary = "分页查询差异分析")
    @GetMapping("/analysis/list")
    public Result<Page<StockDiffAnalysis>> pageAnalysis(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String analysisType,
            @RequestParam(required = false) String warehouseCode) {
        return Result.success(
                diffService.pageAnalysis(new Page<>(page, size), analysisType, warehouseCode));
    }

    // ============================================================

    // 差异统计
    // ============================================================

    @Operation(summary = "获取差异统计")
    @GetMapping("/statistics")
    public Result<Map<String, Object>> getDiffStatistics() {
        return Result.success(diffService.getDiffStatistics());
    }
}
