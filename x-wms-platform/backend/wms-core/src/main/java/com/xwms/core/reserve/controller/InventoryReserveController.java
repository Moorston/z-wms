package com.xwms.core.reserve.controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.common.core.Result;
import com.xwms.core.reserve.entity.*;
import com.xwms.core.reserve.service.InventoryReserveService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/** 库存预占管理 Controller */
@Tag(name = "库存预占管理", description = "库存预占/释放/确认/过期释放")
@RestController
@RequestMapping("/api/reserve")
@RequiredArgsConstructor
public class InventoryReserveController {

    private final InventoryReserveService reserveService;

    // ============================================================

    // 库存预占
    // ============================================================

    @Operation(summary = "创建库存预占")
    @PostMapping
    public Result<InventoryReserve> createReserve(
            @RequestBody InventoryReserve reserve,
            @RequestParam(required = false) String operator) {
        return Result.success(
                reserveService.createReserve(
                        reserve,
                        reserve.getDetails() != null ? reserve.getDetails() : List.of(),
                        operator));
    }

    @Operation(summary = "释放库存预占")
    @PostMapping("/{reserveNo}/release")
    public Result<InventoryReserve> releaseReserve(
            @PathVariable String reserveNo,
            @RequestParam BigDecimal qty,
            @RequestParam(required = false) String operator) {
        return Result.success(reserveService.releaseReserve(reserveNo, qty, operator));
    }

    @Operation(summary = "确认库存预占")
    @PostMapping("/{reserveNo}/confirm")
    public Result<InventoryReserve> confirmReserve(
            @PathVariable String reserveNo,
            @RequestParam BigDecimal qty,
            @RequestParam(required = false) String operator) {
        return Result.success(reserveService.confirmReserve(reserveNo, qty, operator));
    }

    @Operation(summary = "过期预占自动释放")
    @PostMapping("/release-expired")
    public Result<Integer> releaseExpiredReserves() {
        return Result.success(reserveService.releaseExpiredReserves());
    }

    @Operation(summary = "分页查询库存预占")
    @GetMapping
    public Result<Page<InventoryReserve>> pageReserves(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String warehouseCode,
            @RequestParam(required = false) String reserveType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String refNo) {
        return Result.success(
                reserveService.pageReserves(
                        new Page<>(page, size), warehouseCode, reserveType, status, refNo));
    }

    @Operation(summary = "按预占单号查询")
    @GetMapping("/{reserveNo}")
    public Result<InventoryReserve> getReserveByNo(@PathVariable String reserveNo) {
        return Result.success(reserveService.getReserveByNo(reserveNo));
    }

    @Operation(summary = "按关联单据查询")
    @GetMapping("/ref")
    public Result<InventoryReserve> getReserveByRef(
            @RequestParam String refType, @RequestParam String refNo) {
        return Result.success(reserveService.getReserveByRef(refType, refNo));
    }

    @Operation(summary = "查询预占明细")
    @GetMapping("/{reserveNo}/details")
    public Result<List<InventoryReserveDetail>> getReserveDetails(@PathVariable String reserveNo) {
        return Result.success(reserveService.getReserveDetails(reserveNo));
    }

    @Operation(summary = "查询预占流水")
    @GetMapping("/{reserveNo}/logs")
    public Result<List<InventoryReserveLog>> getReserveLogs(@PathVariable String reserveNo) {
        return Result.success(reserveService.getReserveLogs(reserveNo));
    }
}
