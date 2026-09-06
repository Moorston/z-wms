package com.xwms.core.outbound.controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.common.core.Result;
import com.xwms.core.outbound.entity.*;
import com.xwms.core.outbound.service.OutboundService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/** 出库管理 Controller */
@Tag(name = "出库管理", description = "出库单/分配/拣货/打包/发运")
@RestController
@RequestMapping("/api/outbound")
@RequiredArgsConstructor
public class OutboundController {

    private final OutboundService outboundService;

    // ============================================================

    // 出库单管理
    // ============================================================

    @Operation(summary = "创建出库单")
    @PostMapping("/order")
    public Result<OutboundOrder> createOutboundOrder(
            @RequestBody OutboundOrder order,
            @RequestParam(required = false) List<OutboundDetail> details) {
        return Result.success(outboundService.createOutboundOrder(order, details));
    }

    @Operation(summary = "更新出库单")
    @PutMapping("/order")
    public Result<OutboundOrder> updateOutboundOrder(@RequestBody OutboundOrder order) {
        return Result.success(outboundService.updateOutboundOrder(order));
    }

    @Operation(summary = "分页查询出库单")
    @GetMapping("/order")
    public Result<Page<OutboundOrder>> pageOutboundOrders(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String outboundType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String warehouseCode,
            @RequestParam(required = false) String customerCode,
            @RequestParam(required = false) String waveNo) {
        return Result.success(
                outboundService.pageOutboundOrders(
                        new Page<>(page, size),
                        outboundType,
                        status,
                        warehouseCode,
                        customerCode,
                        waveNo));
    }

    @Operation(summary = "按单号查询出库单")
    @GetMapping("/order/{outboundNo}")
    public Result<OutboundOrder> getOutboundOrderByNo(@PathVariable String outboundNo) {
        return Result.success(outboundService.getOutboundOrderByNo(outboundNo));
    }

    @Operation(summary = "查询出库明细")
    @GetMapping("/order/{outboundNo}/details")
    public Result<List<OutboundDetail>> getOutboundDetails(@PathVariable String outboundNo) {
        return Result.success(outboundService.getOutboundDetails(outboundNo));
    }

    // ============================================================

    // 库存分配
    // ============================================================

    @Operation(summary = "执行库存分配")
    @PostMapping("/allocate/{outboundNo}")
    public Result<OutboundOrder> allocate(
            @PathVariable String outboundNo, @RequestParam(required = false) String operator) {
        return Result.success(outboundService.allocate(outboundNo, operator));
    }

    @Operation(summary = "取消分配")
    @PutMapping("/allocate/{outboundNo}/cancel")
    public Result<OutboundOrder> cancelAllocation(
            @PathVariable String outboundNo, @RequestParam(required = false) String operator) {
        return Result.success(outboundService.cancelAllocation(outboundNo, operator));
    }

    // ============================================================

    // 拣货管理
    // ============================================================

    @Operation(summary = "执行拣货")
    @PostMapping("/pick")
    public Result<PickRecord> pick(
            @RequestParam String outboundNo,
            @RequestParam String detailNo,
            @RequestParam String skuCode,
            @RequestParam(required = false) String batchNo,
            @RequestParam String fromLocation,
            @RequestParam BigDecimal pickQty,
            @RequestParam(required = false) String pickType,
            @RequestParam(required = false) String operator) {
        return Result.success(
                outboundService.pick(
                        outboundNo,
                        detailNo,
                        skuCode,
                        batchNo,
                        fromLocation,
                        pickQty,
                        pickType,
                        operator));
    }

    @Operation(summary = "查询拣货记录")
    @GetMapping("/pick/{outboundNo}")
    public Result<List<PickRecord>> getPickRecords(@PathVariable String outboundNo) {
        return Result.success(outboundService.getPickRecords(outboundNo));
    }

    // ============================================================

    // 复核打包
    // ============================================================

    @Operation(summary = "执行打包")
    @PostMapping("/pack")
    public Result<OutboundOrder> pack(
            @RequestParam String outboundNo,
            @RequestParam BigDecimal packQty,
            @RequestParam(required = false) Integer packageCount,
            @RequestParam(required = false) BigDecimal weight,
            @RequestParam(required = false) BigDecimal volume,
            @RequestParam(required = false) String operator) {
        return Result.success(
                outboundService.pack(outboundNo, packQty, packageCount, weight, volume, operator));
    }

    // ============================================================

    // 发运管理
    // ============================================================

    @Operation(summary = "执行发运")
    @PostMapping("/ship")
    public Result<ShipRecord> ship(
            @RequestParam String outboundNo,
            @RequestParam String carrier,
            @RequestParam String trackingNo,
            @RequestParam BigDecimal shipQty,
            @RequestParam(required = false) Integer packageCount,
            @RequestParam(required = false) BigDecimal weight,
            @RequestParam(required = false) BigDecimal volume,
            @RequestParam(required = false) String operator) {
        return Result.success(
                outboundService.ship(
                        outboundNo,
                        carrier,
                        trackingNo,
                        shipQty,
                        packageCount,
                        weight,
                        volume,
                        operator));
    }

    @Operation(summary = "查询发运记录")
    @GetMapping("/ship/{outboundNo}")
    public Result<List<ShipRecord>> getShipRecords(@PathVariable String outboundNo) {
        return Result.success(outboundService.getShipRecords(outboundNo));
    }
}
