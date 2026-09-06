package com.xwms.core.freeze.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.common.core.Result;
import com.xwms.core.freeze.entity.*;
import com.xwms.core.freeze.service.InventoryFreezeService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/** 库存冻结管理 Controller */
@Tag(name = "库存冻结管理", description = "冻结原因/冻结执行/解冻/取消冻结")
@RestController
@RequestMapping("/api/freeze")
@RequiredArgsConstructor
public class InventoryFreezeController {

    private final InventoryFreezeService freezeService;

    // ============================================================

    // 冻结原因配置
    // ============================================================

    @Operation(summary = "创建冻结原因")
    @PostMapping("/reason")
    public Result<FreezeReason> createReason(@RequestBody FreezeReason reason) {
        return Result.success(freezeService.createReason(reason));
    }

    @Operation(summary = "按编码查询冻结原因")
    @GetMapping("/reason/{reasonCode}")
    public Result<FreezeReason> getReasonByCode(@PathVariable String reasonCode) {
        return Result.success(freezeService.getReasonByCode(reasonCode));
    }

    @Operation(summary = "分页查询冻结原因")
    @GetMapping("/reason/list")
    public Result<Page<FreezeReason>> pageReasons(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String freezeType) {
        return Result.success(freezeService.pageReasons(new Page<>(page, size), freezeType));
    }

    // ============================================================

    // 冻结执行
    // ============================================================

    @Operation(summary = "执行库存冻结")
    @PostMapping("/execute")
    public Result<InventoryFreeze> freezeInventory(
            @RequestParam String freezeType,
            @RequestParam String freezeReason,
            @RequestParam String warehouseCode,
            @RequestParam(required = false) String ownerCode,
            @RequestParam String operator,
            @RequestParam(required = false) String refNo,
            @RequestBody List<Map<String, Object>> items) {
        return Result.success(
                freezeService.freezeInventory(
                        freezeType,
                        freezeReason,
                        warehouseCode,
                        ownerCode,
                        operator,
                        refNo,
                        items));
    }

    // ============================================================

    // 解冻
    // ============================================================

    @Operation(summary = "全部解冻")
    @PostMapping("/{freezeNo}/unfreeze-all")
    public Result<InventoryFreeze> unfreezeAll(
            @PathVariable String freezeNo,
            @RequestParam String unfreezeReason,
            @RequestParam String operator) {
        return Result.success(freezeService.unfreezeAll(freezeNo, unfreezeReason, operator));
    }

    @Operation(summary = "部分解冻")
    @PostMapping("/detail/{detailId}/unfreeze-part")
    public Result<InventoryFreezeDetail> unfreezePart(
            @PathVariable Long detailId,
            @RequestParam BigDecimal unfreezeQty,
            @RequestParam String unfreezeReason,
            @RequestParam String operator) {
        return Result.success(
                freezeService.unfreezePart(detailId, unfreezeQty, unfreezeReason, operator));
    }

    @Operation(summary = "自动解冻（定时任务）")
    @PostMapping("/auto-unfreeze")
    public Result<Integer> autoUnfreeze() {
        return Result.success(freezeService.autoUnfreeze());
    }

    // ============================================================

    // 取消冻结
    // ============================================================

    @Operation(summary = "取消冻结")
    @PostMapping("/{freezeNo}/cancel")
    public Result<InventoryFreeze> cancelFreeze(
            @PathVariable String freezeNo,
            @RequestParam String reason,
            @RequestParam String operator) {
        return Result.success(freezeService.cancelFreeze(freezeNo, reason, operator));
    }

    // ============================================================

    // 查询
    // ============================================================

    @Operation(summary = "按冻结单号查询")
    @GetMapping("/{freezeNo}")
    public Result<InventoryFreeze> getByFreezeNo(@PathVariable String freezeNo) {
        return Result.success(freezeService.getByFreezeNo(freezeNo));
    }

    @Operation(summary = "查询冻结明细")
    @GetMapping("/{freezeNo}/details")
    public Result<List<InventoryFreezeDetail>> getDetailsByFreezeNo(@PathVariable String freezeNo) {
        return Result.success(freezeService.getDetailsByFreezeNo(freezeNo));
    }

    @Operation(summary = "按SKU查询冻结明细")
    @GetMapping("/sku/{skuCode}")
    public Result<List<InventoryFreezeDetail>> getFrozenBySku(@PathVariable String skuCode) {
        return Result.success(freezeService.getFrozenBySku(skuCode));
    }

    @Operation(summary = "按库位查询冻结明细")
    @GetMapping("/location/{locationCode}")
    public Result<List<InventoryFreezeDetail>> getFrozenByLocation(
            @PathVariable String locationCode) {
        return Result.success(freezeService.getFrozenByLocation(locationCode));
    }

    @Operation(summary = "按批次查询冻结明细")
    @GetMapping("/batch/{batchNo}")
    public Result<List<InventoryFreezeDetail>> getFrozenByBatch(@PathVariable String batchNo) {
        return Result.success(freezeService.getFrozenByBatch(batchNo));
    }

    @Operation(summary = "分页查询冻结单")
    @GetMapping("/list")
    public Result<Page<InventoryFreeze>> pageFreezes(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String warehouseCode,
            @RequestParam(required = false) String freezeType,
            @RequestParam(required = false) String status) {
        return Result.success(
                freezeService.pageFreezes(
                        new Page<>(page, size), warehouseCode, freezeType, status));
    }

    @Operation(summary = "查询解冻日志")
    @GetMapping("/{freezeNo}/unfreeze-logs")
    public Result<List<InventoryUnfreezeLog>> getUnfreezeLogsByFreezeNo(
            @PathVariable String freezeNo) {
        return Result.success(freezeService.getUnfreezeLogsByFreezeNo(freezeNo));
    }

    @Operation(summary = "按解冻单号查询")
    @GetMapping("/unfreeze/{unfreezeNo}")
    public Result<InventoryUnfreezeLog> getUnfreezeLogByNo(@PathVariable String unfreezeNo) {
        return Result.success(freezeService.getUnfreezeLogByNo(unfreezeNo));
    }
}
