package com.xwms.core.inbound.controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.common.core.Result;
import com.xwms.core.inbound.entity.*;
import com.xwms.core.inbound.service.InboundService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/** 入库管理 Controller */
@Tag(name = "入库管理", description = "ASN/入库单/收货/上架")
@RestController
@RequestMapping("/api/inbound")
@RequiredArgsConstructor
public class InboundController {

    private final InboundService inboundService;

    // ============================================================

    // ASN管理
    // ============================================================

    @Operation(summary = "创建ASN")
    @PostMapping("/asn")
    public Result<Asn> createAsn(@RequestBody Asn asn) {
        return Result.success(inboundService.createAsn(asn));
    }

    @Operation(summary = "更新ASN")
    @PutMapping("/asn")
    public Result<Asn> updateAsn(@RequestBody Asn asn) {
        return Result.success(inboundService.updateAsn(asn));
    }

    @Operation(summary = "分页查询ASN")
    @GetMapping("/asn")
    public Result<Page<Asn>> pageAsns(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String asnType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String supplierCode,
            @RequestParam(required = false) String warehouseCode) {
        return Result.success(
                inboundService.pageAsns(
                        new Page<>(page, size), asnType, status, supplierCode, warehouseCode));
    }

    @Operation(summary = "按单号查询ASN")
    @GetMapping("/asn/{asnNo}")
    public Result<Asn> getAsnByNo(@PathVariable String asnNo) {
        return Result.success(inboundService.getAsnByNo(asnNo));
    }

    @Operation(summary = "ASN到货确认")
    @PutMapping("/asn/{asnNo}/arrival")
    public Result<Asn> confirmAsnArrival(@PathVariable String asnNo) {
        return Result.success(inboundService.confirmAsnArrival(asnNo));
    }

    // ============================================================

    // 入库单管理
    // ============================================================

    @Operation(summary = "创建入库单")
    @PostMapping("/order")
    public Result<InboundOrder> createInboundOrder(
            @RequestBody InboundOrder order,
            @RequestParam(required = false) List<InboundDetail> details) {
        return Result.success(inboundService.createInboundOrder(order, details));
    }

    @Operation(summary = "更新入库单")
    @PutMapping("/order")
    public Result<InboundOrder> updateInboundOrder(@RequestBody InboundOrder order) {
        return Result.success(inboundService.updateInboundOrder(order));
    }

    @Operation(summary = "分页查询入库单")
    @GetMapping("/order")
    public Result<Page<InboundOrder>> pageInboundOrders(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String inboundType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String warehouseCode,
            @RequestParam(required = false) String supplierCode) {
        return Result.success(
                inboundService.pageInboundOrders(
                        new Page<>(page, size), inboundType, status, warehouseCode, supplierCode));
    }

    @Operation(summary = "按单号查询入库单")
    @GetMapping("/order/{inboundNo}")
    public Result<InboundOrder> getInboundOrderByNo(@PathVariable String inboundNo) {
        return Result.success(inboundService.getInboundOrderByNo(inboundNo));
    }

    @Operation(summary = "查询入库明细")
    @GetMapping("/order/{inboundNo}/details")
    public Result<List<InboundDetail>> getInboundDetails(@PathVariable String inboundNo) {
        return Result.success(inboundService.getInboundDetails(inboundNo));
    }

    // ============================================================

    // 收货管理
    // ============================================================

    @Operation(summary = "执行收货")
    @PostMapping("/receive")
    public Result<ReceiveRecord> receive(
            @RequestParam String inboundNo,
            @RequestParam String detailNo,
            @RequestParam String skuCode,
            @RequestParam(required = false) String batchNo,
            @RequestParam BigDecimal receiveQty,
            @RequestParam(required = false) String receiveLocation,
            @RequestParam(required = false) String receiveType,
            @RequestParam(required = false) String operator) {
        return Result.success(
                inboundService.receive(
                        inboundNo,
                        detailNo,
                        skuCode,
                        batchNo,
                        receiveQty,
                        receiveLocation,
                        receiveType,
                        operator));
    }

    @Operation(summary = "收货完成")
    @PutMapping("/receive/{inboundNo}/complete")
    public Result<InboundOrder> completeReceive(@PathVariable String inboundNo) {
        return Result.success(inboundService.completeReceive(inboundNo));
    }

    @Operation(summary = "查询收货记录")
    @GetMapping("/receive/{inboundNo}")
    public Result<List<ReceiveRecord>> getReceiveRecords(@PathVariable String inboundNo) {
        return Result.success(inboundService.getReceiveRecords(inboundNo));
    }

    // ============================================================

    // 上架管理
    // ============================================================

    @Operation(summary = "执行上架")
    @PostMapping("/putaway")
    public Result<InboundDetail> putaway(
            @RequestParam String inboundNo,
            @RequestParam String detailNo,
            @RequestParam String targetLocation,
            @RequestParam BigDecimal putawayQty,
            @RequestParam(required = false) String operator) {
        return Result.success(
                inboundService.putaway(inboundNo, detailNo, targetLocation, putawayQty, operator));
    }
}
