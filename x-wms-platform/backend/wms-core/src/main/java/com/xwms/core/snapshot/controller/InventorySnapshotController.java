package com.xwms.core.snapshot.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.common.core.Result;
import com.xwms.core.snapshot.entity.*;
import com.xwms.core.snapshot.service.InventorySnapshotService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/** 库存快照管理 Controller */
@Tag(name = "库存快照管理", description = "快照生成/快照查询/快照对比/快照恢复")
@RestController
@RequestMapping("/api/snapshot")
@RequiredArgsConstructor
public class InventorySnapshotController {

    private final InventorySnapshotService snapshotService;

    // ============================================================

    // 快照生成
    // ============================================================

    @Operation(summary = "生成库存快照")
    @PostMapping("/create")
    public Result<InventorySnapshot> createSnapshot(
            @RequestParam String snapshotType,
            @RequestParam String warehouseCode,
            @RequestParam(required = false) String ownerCode,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate snapshotDate,
            @RequestParam(required = false) String operator,
            @RequestBody List<Map<String, Object>> inventoryData) {
        return Result.success(
                snapshotService.createSnapshot(
                        snapshotType,
                        warehouseCode,
                        ownerCode,
                        snapshotDate,
                        operator,
                        inventoryData));
    }

    // ============================================================

    // 快照查询
    // ============================================================

    @Operation(summary = "按快照号查询")
    @GetMapping("/{snapshotNo}")
    public Result<InventorySnapshot> getBySnapshotNo(@PathVariable String snapshotNo) {
        return Result.success(snapshotService.getBySnapshotNo(snapshotNo));
    }

    @Operation(summary = "按日期和类型查询快照")
    @GetMapping("/date/{snapshotDate}/{snapshotType}")
    public Result<InventorySnapshot> getByDateAndType(
            @RequestParam String warehouseCode,
            @PathVariable @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate snapshotDate,
            @PathVariable String snapshotType) {
        return Result.success(
                snapshotService.getByDateAndType(warehouseCode, snapshotDate, snapshotType));
    }

    @Operation(summary = "按日期范围查询快照")
    @GetMapping("/date-range")
    public Result<List<InventorySnapshot>> getByDateRange(
            @RequestParam String warehouseCode,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        return Result.success(snapshotService.getByDateRange(warehouseCode, startDate, endDate));
    }

    @Operation(summary = "查询快照明细")
    @GetMapping("/{snapshotNo}/details")
    public Result<List<InventorySnapshotDetail>> getDetailsBySnapshotNo(
            @PathVariable String snapshotNo) {
        return Result.success(snapshotService.getDetailsBySnapshotNo(snapshotNo));
    }

    @Operation(summary = "按SKU查询快照明细")
    @GetMapping("/{snapshotNo}/details/{skuCode}")
    public Result<List<InventorySnapshotDetail>> getDetailsBySnapshotNoAndSku(
            @PathVariable String snapshotNo, @PathVariable String skuCode) {
        return Result.success(snapshotService.getDetailsBySnapshotNoAndSku(snapshotNo, skuCode));
    }

    @Operation(summary = "分页查询快照")
    @GetMapping("/list")
    public Result<Page<InventorySnapshot>> pageSnapshots(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String warehouseCode,
            @RequestParam(required = false) String snapshotType,
            @RequestParam(required = false) String status) {
        return Result.success(
                snapshotService.pageSnapshots(
                        new Page<>(page, size), warehouseCode, snapshotType, status));
    }

    // ============================================================

    // 快照对比
    // ============================================================

    @Operation(summary = "对比两个快照")
    @PostMapping("/compare")
    public Result<SnapshotCompare> compareSnapshots(
            @RequestParam String snapshotNo1,
            @RequestParam String snapshotNo2,
            @RequestParam(defaultValue = "QUANTITY") String compareType,
            @RequestParam(required = false) String operator) {
        return Result.success(
                snapshotService.compareSnapshots(snapshotNo1, snapshotNo2, compareType, operator));
    }

    @Operation(summary = "按对比号查询")
    @GetMapping("/compare/{compareNo}")
    public Result<SnapshotCompare> getCompareByNo(@PathVariable String compareNo) {
        return Result.success(snapshotService.getCompareByNo(compareNo));
    }

    @Operation(summary = "查询对比明细")
    @GetMapping("/compare/{compareNo}/details")
    public Result<List<SnapshotCompareDetail>> getCompareDetails(@PathVariable String compareNo) {
        return Result.success(snapshotService.getCompareDetails(compareNo));
    }

    // ============================================================

    // 快照恢复
    // ============================================================

    @Operation(summary = "从快照恢复库存")
    @PostMapping("/restore")
    public Result<SnapshotRestoreLog> restoreFromSnapshot(
            @RequestParam String snapshotNo,
            @RequestParam(defaultValue = "PARTIAL") String restoreType,
            @RequestParam(required = false) List<String> skuCodes,
            @RequestParam String operator,
            @RequestParam(required = false) String remark) {
        return Result.success(
                snapshotService.restoreFromSnapshot(
                        snapshotNo, restoreType, skuCodes, operator, remark));
    }

    @Operation(summary = "按恢复号查询")
    @GetMapping("/restore/{restoreNo}")
    public Result<SnapshotRestoreLog> getRestoreByNo(@PathVariable String restoreNo) {
        return Result.success(snapshotService.getRestoreByNo(restoreNo));
    }

    @Operation(summary = "分页查询恢复记录")
    @GetMapping("/restore/list")
    public Result<Page<SnapshotRestoreLog>> pageRestoreLogs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String warehouseCode,
            @RequestParam(required = false) String status) {
        return Result.success(
                snapshotService.pageRestoreLogs(new Page<>(page, size), warehouseCode, status));
    }
}
