package com.xwms.core.pack.controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.common.core.Result;
import com.xwms.core.pack.entity.*;
import com.xwms.core.pack.service.PackService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/** 打包管理 Controller */
@Tag(name = "打包管理", description = "打包单/复核/打包/包裹/称重")
@RestController
@RequestMapping("/api/pack")
@RequiredArgsConstructor
public class PackController {

    private final PackService packService;

    // ============================================================

    // 打包单管理
    // ============================================================

    @Operation(summary = "创建打包单")
    @PostMapping
    public Result<Pack> createPack(
            @RequestBody Pack pack,
            @RequestParam(required = false) List<PackDetail> details,
            @RequestParam(required = false) String operator) {
        return Result.success(packService.createPack(pack, details, operator));
    }

    @Operation(summary = "分页查询打包单")
    @GetMapping
    public Result<Page<Pack>> pagePacks(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String outboundNo,
            @RequestParam(required = false) String waveNo,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String warehouseCode) {
        return Result.success(
                packService.pagePacks(
                        new Page<>(page, size), outboundNo, waveNo, status, warehouseCode));
    }

    @Operation(summary = "按打包单号查询")
    @GetMapping("/{packNo}")
    public Result<Pack> getPackByNo(@PathVariable String packNo) {
        return Result.success(packService.getPackByNo(packNo));
    }

    @Operation(summary = "查询打包明细")
    @GetMapping("/{packNo}/details")
    public Result<List<PackDetail>> getPackDetails(@PathVariable String packNo) {
        return Result.success(packService.getPackDetails(packNo));
    }

    // ============================================================

    // 复核
    // ============================================================

    @Operation(summary = "执行复核")
    @PostMapping("/{packNo}/check")
    public Result<CheckRecord> check(
            @PathVariable String packNo,
            @RequestParam String skuCode,
            @RequestParam(required = false) String batchNo,
            @RequestParam BigDecimal expectedQty,
            @RequestParam BigDecimal actualQty,
            @RequestParam String checker) {
        return Result.success(
                packService.check(packNo, skuCode, batchNo, expectedQty, actualQty, checker));
    }

    @Operation(summary = "查询复核记录")
    @GetMapping("/{packNo}/check-records")
    public Result<List<CheckRecord>> getCheckRecords(@PathVariable String packNo) {
        return Result.success(packService.getCheckRecords(packNo));
    }

    // ============================================================

    // 打包
    // ============================================================

    @Operation(summary = "执行打包")
    @PostMapping("/{packNo}/pack")
    public Result<PackageEntity> pack(
            @PathVariable String packNo,
            @RequestParam String packageType,
            @RequestParam(required = false) String packageSize,
            @RequestParam(required = false) BigDecimal weight,
            @RequestParam(required = false) BigDecimal volume,
            @RequestParam(required = false) BigDecimal length,
            @RequestParam(required = false) BigDecimal width,
            @RequestParam(required = false) BigDecimal height,
            @RequestParam(required = false) Integer itemCount,
            @RequestParam(required = false) Integer skuCount,
            @RequestParam String packer) {
        return Result.success(
                packService.pack(
                        packNo,
                        packageType,
                        packageSize,
                        weight,
                        volume,
                        length,
                        width,
                        height,
                        itemCount,
                        skuCount,
                        packer));
    }

    // ============================================================

    // 包裹管理
    // ============================================================

    @Operation(summary = "查询包裹列表")
    @GetMapping("/{packNo}/packages")
    public Result<List<PackageEntity>> getPackages(@PathVariable String packNo) {
        return Result.success(packService.getPackages(packNo));
    }

    @Operation(summary = "按包裹号查询")
    @GetMapping("/package/{packageNo}")
    public Result<PackageEntity> getPackageByNo(@PathVariable String packageNo) {
        return Result.success(packService.getPackageByNo(packageNo));
    }

    @Operation(summary = "包裹称重")
    @PostMapping("/package/{packageNo}/weigh")
    public Result<PackageEntity> weighPackage(
            @PathVariable String packageNo,
            @RequestParam BigDecimal weight,
            @RequestParam(required = false) BigDecimal volume) {
        return Result.success(packService.weighPackage(packageNo, weight, volume));
    }

    @Operation(summary = "包裹贴标")
    @PostMapping("/package/{packageNo}/label")
    public Result<PackageEntity> labelPackage(
            @PathVariable String packageNo,
            @RequestParam String trackingNo,
            @RequestParam String carrier) {
        return Result.success(packService.labelPackage(packageNo, trackingNo, carrier));
    }
}
