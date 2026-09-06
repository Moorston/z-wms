package com.xwms.core.pallet.controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.common.core.Result;
import com.xwms.core.pallet.entity.*;
import com.xwms.core.pallet.service.PalletService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/** 托盘/LPN管理控制器 */
@Tag(name = "托盘/LPN管理", description = "LPN管理/码盘/拆盘/合并/移动/封存/码盘预约")
@RestController
@RequestMapping("/api/pallet")
@RequiredArgsConstructor
public class PalletController {

    private final PalletService palletService;

    // ============================================================

    // 1. LPN管理
    // ============================================================

    @Operation(summary = "生成空托盘")
    @PostMapping
    public Result<Pallet> createEmptyPallet(
            @RequestParam(required = false) String palletType,
            @RequestParam String warehouseCode,
            @RequestParam(required = false) String locationCode,
            @RequestParam String operator) {
        return Result.success(
                palletService.createEmptyPallet(palletType, warehouseCode, locationCode, operator));
    }

    @Operation(summary = "批量生成空托盘")
    @PostMapping("/batch")
    public Result<List<Pallet>> batchCreateEmptyPallets(
            @RequestParam int count,
            @RequestParam(required = false) String palletType,
            @RequestParam String warehouseCode,
            @RequestParam(required = false) String locationCode,
            @RequestParam String operator) {
        return Result.success(
                palletService.batchCreateEmptyPallets(
                        count, palletType, warehouseCode, locationCode, operator));
    }

    @Operation(summary = "根据LPN号查询托盘")
    @GetMapping("/{lpnNo}")
    public Result<Pallet> getPalletByLpn(@PathVariable String lpnNo) {
        return Result.success(palletService.getPalletByLpn(lpnNo));
    }

    @Operation(summary = "查询托盘明细")
    @GetMapping("/{lpnNo}/details")
    public Result<List<PalletDetail>> getPalletDetails(@PathVariable String lpnNo) {
        return Result.success(palletService.getPalletDetails(lpnNo));
    }

    @Operation(summary = "分页查询托盘")
    @GetMapping
    public Result<Page<Pallet>> pagePallets(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String palletType,
            @RequestParam(required = false) String warehouseCode,
            @RequestParam(required = false) String locationCode) {
        return Result.success(
                palletService.pagePallets(
                        new Page<>(page, size), status, palletType, warehouseCode, locationCode));
    }

    // ============================================================

    // 2. 码盘操作
    // ============================================================

    @Operation(summary = "收货码盘")
    @PostMapping("/palletize")
    public Result<PalletDetail> palletize(
            @RequestParam String lpnNo,
            @RequestParam(required = false) String asnNo,
            @RequestParam(required = false) String inboundNo,
            @RequestParam(required = false) String inboundDetailNo,
            @RequestParam String skuCode,
            @RequestParam String skuName,
            @RequestParam BigDecimal qty,
            @RequestParam(required = false) String batchNo,
            @RequestParam(required = false) String serialNo,
            @RequestParam String operator) {
        return Result.success(
                palletService.palletize(
                        lpnNo,
                        asnNo,
                        inboundNo,
                        inboundDetailNo,
                        skuCode,
                        skuName,
                        qty,
                        batchNo,
                        serialNo,
                        operator));
    }

    // ============================================================

    // 3. 拆盘操作
    // ============================================================

    @Operation(summary = "拆盘")
    @PostMapping("/depalletize")
    public Result<PalletDetail> depalletize(
            @RequestParam String lpnNo,
            @RequestParam String detailNo,
            @RequestParam BigDecimal qty,
            @RequestParam(required = false) String reason,
            @RequestParam String operator) {
        return Result.success(palletService.depalletize(lpnNo, detailNo, qty, reason, operator));
    }

    // ============================================================

    // 4. 托盘合并
    // ============================================================

    @Operation(summary = "托盘合并")
    @PostMapping("/merge")
    public Result<Pallet> mergePallet(
            @RequestParam String sourceLpn,
            @RequestParam String targetLpn,
            @RequestParam String operator) {
        return Result.success(palletService.mergePallet(sourceLpn, targetLpn, operator));
    }

    // ============================================================

    // 5. 托盘移动
    // ============================================================

    @Operation(summary = "托盘移动")
    @PutMapping("/{lpnNo}/move")
    public Result<Pallet> movePallet(
            @PathVariable String lpnNo,
            @RequestParam String targetLocation,
            @RequestParam(required = false) String targetArea,
            @RequestParam String operator) {
        return Result.success(
                palletService.movePallet(lpnNo, targetLocation, targetArea, operator));
    }

    @Operation(summary = "确认托盘到达")
    @PutMapping("/{lpnNo}/confirm-arrival")
    public Result<Pallet> confirmPalletArrival(
            @PathVariable String lpnNo, @RequestParam String operator) {
        return Result.success(palletService.confirmPalletArrival(lpnNo, operator));
    }

    // ============================================================

    // 6. 托盘封存/解封
    // ============================================================

    @Operation(summary = "封存托盘")
    @PutMapping("/{lpnNo}/seal")
    public Result<Pallet> sealPallet(
            @PathVariable String lpnNo,
            @RequestParam(required = false) String reason,
            @RequestParam String operator) {
        return Result.success(palletService.sealPallet(lpnNo, reason, operator));
    }

    @Operation(summary = "解封托盘")
    @PutMapping("/{lpnNo}/unseal")
    public Result<Pallet> unsealPallet(
            @PathVariable String lpnNo,
            @RequestParam(required = false) String reason,
            @RequestParam String operator) {
        return Result.success(palletService.unsealPallet(lpnNo, reason, operator));
    }

    // ============================================================

    // 7. 码盘预约
    // ============================================================

    @Operation(summary = "创建码盘预约")
    @PostMapping("/reservation")
    public Result<PalletReservation> createReservation(
            @RequestParam(required = false) String asnNo,
            @RequestParam(required = false) String inboundNo,
            @RequestParam(required = false) String poNo,
            @RequestParam String warehouseCode,
            @RequestParam(required = false) String reservedReceiveArea,
            @RequestParam(required = false) String reservedDockNo,
            @RequestParam(required = false) String palletizeStrategy,
            @RequestParam(required = false) String putawayStrategy,
            @RequestParam String operator) {
        return Result.success(
                palletService.createReservation(
                        asnNo,
                        inboundNo,
                        poNo,
                        warehouseCode,
                        reservedReceiveArea,
                        reservedDockNo,
                        palletizeStrategy,
                        putawayStrategy,
                        operator));
    }

    @Operation(summary = "确认码盘预约")
    @PutMapping("/reservation/{reservationNo}/confirm")
    public Result<PalletReservation> confirmReservation(
            @PathVariable String reservationNo,
            @RequestParam int reservedPalletCount,
            @RequestParam BigDecimal reservedTotalQty,
            @RequestParam(required = false) String reservedPutawayLocations,
            @RequestParam String operator) {
        return Result.success(
                palletService.confirmReservation(
                        reservationNo,
                        reservedPalletCount,
                        reservedTotalQty,
                        reservedPutawayLocations,
                        operator));
    }

    @Operation(summary = "开始码盘预约")
    @PutMapping("/reservation/{reservationNo}/start")
    public Result<PalletReservation> startReservation(
            @PathVariable String reservationNo, @RequestParam String operator) {
        return Result.success(palletService.startReservation(reservationNo, operator));
    }

    @Operation(summary = "完成码盘预约")
    @PutMapping("/reservation/{reservationNo}/complete")
    public Result<PalletReservation> completeReservation(
            @PathVariable String reservationNo,
            @RequestParam int actualPalletCount,
            @RequestParam BigDecimal actualTotalQty,
            @RequestParam String operator) {
        return Result.success(
                palletService.completeReservation(
                        reservationNo, actualPalletCount, actualTotalQty, operator));
    }

    @Operation(summary = "查询码盘预约")
    @GetMapping("/reservation/{reservationNo}")
    public Result<PalletReservation> getReservationByNo(@PathVariable String reservationNo) {
        return Result.success(palletService.getReservationByNo(reservationNo));
    }

    @Operation(summary = "按状态查询码盘预约")
    @GetMapping("/reservation")
    public Result<List<PalletReservation>> getReservationsByStatus(@RequestParam String status) {
        return Result.success(palletService.getReservationsByStatus(status));
    }

    // ============================================================

    // 8. 操作记录
    // ============================================================

    @Operation(summary = "查询托盘操作历史")
    @GetMapping("/{lpnNo}/operations")
    public Result<List<PalletOperation>> getOperationHistory(@PathVariable String lpnNo) {
        return Result.success(palletService.getOperationHistory(lpnNo));
    }
}
