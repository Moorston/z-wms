package com.xwms.core.crossdock.controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.common.core.Result;
import com.xwms.core.crossdock.entity.*;
import com.xwms.core.crossdock.service.CrossdockService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/** 越库管理 Controller */
@Tag(name = "越库管理", description = "越库单/匹配/收货/分拣/发运")
@RestController
@RequestMapping("/api/crossdock")
@RequiredArgsConstructor
public class CrossdockController {

    private final CrossdockService crossdockService;

    // ============================================================

    // 越库单管理
    // ============================================================

    @Operation(summary = "创建越库单")
    @PostMapping
    public Result<Crossdock> createCrossdock(
            @RequestBody Crossdock crossdock,
            @RequestParam(required = false) List<CrossdockDetail> details,
            @RequestParam(required = false) String operator) {
        return Result.success(crossdockService.createCrossdock(crossdock, details, operator));
    }

    @Operation(summary = "分页查询越库单")
    @GetMapping
    public Result<Page<Crossdock>> pageCrossdocks(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String crossdockType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String warehouseCode,
            @RequestParam(required = false) String ownerCode) {
        return Result.success(
                crossdockService.pageCrossdocks(
                        new Page<>(page, size), crossdockType, status, warehouseCode, ownerCode));
    }

    @Operation(summary = "按越库单号查询")
    @GetMapping("/{crossdockNo}")
    public Result<Crossdock> getCrossdockByNo(@PathVariable String crossdockNo) {
        return Result.success(crossdockService.getCrossdockByNo(crossdockNo));
    }

    @Operation(summary = "查询越库明细")
    @GetMapping("/{crossdockNo}/details")
    public Result<List<CrossdockDetail>> getDetails(@PathVariable String crossdockNo) {
        return Result.success(crossdockService.getDetails(crossdockNo));
    }

    // ============================================================

    // 越库匹配
    // ============================================================

    @Operation(summary = "自动匹配入库与出库")
    @PostMapping("/{crossdockNo}/auto-match")
    public Result<List<CrossdockMatch>> autoMatch(@PathVariable String crossdockNo) {
        return Result.success(crossdockService.autoMatch(crossdockNo));
    }

    @Operation(summary = "手动匹配")
    @PostMapping("/{crossdockNo}/manual-match")
    public Result<CrossdockMatch> manualMatch(
            @PathVariable String crossdockNo,
            @RequestParam Integer inboundLineNo,
            @RequestParam Integer outboundLineNo,
            @RequestParam String skuCode,
            @RequestParam(required = false) String batchNo,
            @RequestParam BigDecimal matchQty,
            @RequestParam String matchType) {
        return Result.success(
                crossdockService.manualMatch(
                        crossdockNo,
                        inboundLineNo,
                        outboundLineNo,
                        skuCode,
                        batchNo,
                        matchQty,
                        matchType));
    }

    @Operation(summary = "查询匹配记录")
    @GetMapping("/{crossdockNo}/matches")
    public Result<List<CrossdockMatch>> getMatches(@PathVariable String crossdockNo) {
        return Result.success(crossdockService.getMatches(crossdockNo));
    }

    // ============================================================

    // 越库收货
    // ============================================================

    @Operation(summary = "越库收货")
    @PostMapping("/{crossdockNo}/receive")
    public Result<CrossdockTask> receive(
            @PathVariable String crossdockNo,
            @RequestParam String skuCode,
            @RequestParam(required = false) String batchNo,
            @RequestParam String fromLocation,
            @RequestParam BigDecimal receivedQty,
            @RequestParam(required = false) String operator) {
        return Result.success(
                crossdockService.receive(
                        crossdockNo, skuCode, batchNo, fromLocation, receivedQty, operator));
    }

    // ============================================================

    // 越库分拣
    // ============================================================

    @Operation(summary = "越库分拣")
    @PostMapping("/{crossdockNo}/sort")
    public Result<CrossdockTask> sort(
            @PathVariable String crossdockNo,
            @RequestParam String skuCode,
            @RequestParam(required = false) String batchNo,
            @RequestParam String fromLocation,
            @RequestParam String toLocation,
            @RequestParam BigDecimal sortQty,
            @RequestParam(required = false) String operator) {
        return Result.success(
                crossdockService.sort(
                        crossdockNo,
                        skuCode,
                        batchNo,
                        fromLocation,
                        toLocation,
                        sortQty,
                        operator));
    }

    // ============================================================

    // 越库发运
    // ============================================================

    @Operation(summary = "越库发运")
    @PostMapping("/{crossdockNo}/ship")
    public Result<CrossdockTask> ship(
            @PathVariable String crossdockNo,
            @RequestParam String skuCode,
            @RequestParam(required = false) String batchNo,
            @RequestParam String fromLocation,
            @RequestParam BigDecimal shipQty,
            @RequestParam(required = false) String operator) {
        return Result.success(
                crossdockService.ship(
                        crossdockNo, skuCode, batchNo, fromLocation, shipQty, operator));
    }

    // ============================================================

    // 作业记录
    // ============================================================

    @Operation(summary = "查询作业记录")
    @GetMapping("/{crossdockNo}/tasks")
    public Result<List<CrossdockTask>> getTasks(@PathVariable String crossdockNo) {
        return Result.success(crossdockService.getTasks(crossdockNo));
    }
}
