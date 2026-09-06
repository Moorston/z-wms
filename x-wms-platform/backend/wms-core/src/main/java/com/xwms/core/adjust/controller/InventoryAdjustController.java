package com.xwms.core.adjust.controller;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.common.core.Result;
import com.xwms.core.adjust.entity.*;
import com.xwms.core.adjust.service.InventoryAdjustService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/** 搴撳瓨璋冩暣绠＄悊 Controller */
@Tag(name = "搴撳瓨璋冩暣绠＄悊", description = "搴撳瓨璋冩暣鍗?搴撳瓨鍐荤粨/瑙ｅ喕/璋冩暣娴佹按")
@RestController
@RequestMapping("/api/inventory-adjust")
@RequiredArgsConstructor
public class InventoryAdjustController {

    private final InventoryAdjustService adjustService;

    // ============================================================

    // 搴撳瓨璋冩暣鍗?
    // ============================================================

    @Operation(summary = "鍒涘缓搴撳瓨璋冩暣鍗")
    @PostMapping
    public Result<InventoryAdjust> createAdjust(
            @RequestBody InventoryAdjust adjust, @RequestParam(required = false) String operator) {
        return Result.success(
                adjustService.createAdjust(
                        adjust,
                        adjust.getDetails() != null ? adjust.getDetails() : List.of(),
                        operator));
    }

    @Operation(summary = "鎻愪氦搴撳瓨璋冩暣鍗")
    @PostMapping("/{adjustNo}/submit")
    public Result<InventoryAdjust> submitAdjust(
            @PathVariable String adjustNo, @RequestParam(required = false) String operator) {
        return Result.success(adjustService.submitAdjust(adjustNo, operator));
    }

    @Operation(summary = "瀹℃壒搴撳瓨璋冩暣鍗")
    @PostMapping("/{adjustNo}/approve")
    public Result<InventoryAdjust> approveAdjust(
            @PathVariable String adjustNo, @RequestParam(required = false) String operator) {
        return Result.success(adjustService.approveAdjust(adjustNo, operator));
    }

    @Operation(summary = "鎵ц搴撳瓨璋冩暣鍗")
    @PostMapping("/{adjustNo}/execute")
    public Result<InventoryAdjust> executeAdjust(
            @PathVariable String adjustNo, @RequestParam(required = false) String operator) {
        return Result.success(adjustService.executeAdjust(adjustNo, operator));
    }

    @Operation(summary = "鍙栨秷搴撳瓨璋冩暣鍗")
    @PostMapping("/{adjustNo}/cancel")
    public Result<InventoryAdjust> cancelAdjust(
            @PathVariable String adjustNo, @RequestParam(required = false) String operator) {
        return Result.success(adjustService.cancelAdjust(adjustNo, operator));
    }

    @Operation(summary = "鍒嗛〉鏌ヨ搴撳瓨璋冩暣鍗")
    @GetMapping
    public Result<Page<InventoryAdjust>> pageAdjusts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String warehouseCode,
            @RequestParam(required = false) String adjustType,
            @RequestParam(required = false) String status) {
        return Result.success(
                adjustService.pageAdjusts(
                        new Page<>(page, size), warehouseCode, adjustType, status));
    }

    @Operation(summary = "鎸夊崟鍙锋煡璇㈠簱瀛樿皟鏁村崟")
    @GetMapping("/{adjustNo}")
    public Result<InventoryAdjust> getAdjustByNo(@PathVariable String adjustNo) {
        return Result.success(adjustService.getAdjustByNo(adjustNo));
    }

    @Operation(summary = "鏌ヨ搴撳瓨璋冩暣鏄庣粏")
    @GetMapping("/{adjustNo}/details")
    public Result<List<InventoryAdjustDetail>> getAdjustDetails(@PathVariable String adjustNo) {
        return Result.success(adjustService.getAdjustDetails(adjustNo));
    }

    // ============================================================

    // ============================================================
    // 搴撳瓨璋冩暣娴佹按
    // ============================================================

    @Operation(summary = "鎸塖KU鏌ヨ璋冩暣娴佹按")
    @GetMapping("/log/sku")
    public Result<List<InventoryAdjustLog>> getAdjustLogsBySku(
            @RequestParam String skuCode,
            @RequestParam(required = false) String batchNo,
            @RequestParam(required = false) String locationCode) {
        return Result.success(adjustService.getAdjustLogsBySku(skuCode, batchNo, locationCode));
    }

    @Operation(summary = "鎸夎皟鏁村崟鏌ヨ娴佹按")
    @GetMapping("/log/adjust/{adjustNo}")
    public Result<List<InventoryAdjustLog>> getAdjustLogsByAdjustNo(@PathVariable String adjustNo) {
        return Result.success(adjustService.getAdjustLogsByAdjustNo(adjustNo));
    }
}
