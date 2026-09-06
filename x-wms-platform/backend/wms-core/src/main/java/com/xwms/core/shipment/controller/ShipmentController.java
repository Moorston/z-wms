package com.xwms.core.shipment.controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.common.core.Result;
import com.xwms.core.shipment.entity.*;
import com.xwms.core.shipment.service.ShipmentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/** 发运管理 Controller */
@Tag(name = "发运管理", description = "发运单/承运商选择/快递单获取/打印/发运确认")
@RestController
@RequestMapping("/api/shipment")
@RequiredArgsConstructor
public class ShipmentController {

    private final ShipmentService shipmentService;

    // ============================================================

    // 发运单管理
    // ============================================================

    @Operation(summary = "创建发运单")
    @PostMapping
    public Result<Shipment> createShipment(
            @RequestBody Shipment shipment,
            @RequestParam(required = false) List<ShipmentDetail> details,
            @RequestParam(required = false) String operator) {
        return Result.success(shipmentService.createShipment(shipment, details, operator));
    }

    @Operation(summary = "分页查询发运单")
    @GetMapping
    public Result<Page<Shipment>> pageShipments(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String outboundNo,
            @RequestParam(required = false) String carrier,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String warehouseCode) {
        return Result.success(
                shipmentService.pageShipments(
                        new Page<>(page, size), outboundNo, carrier, status, warehouseCode));
    }

    @Operation(summary = "按发运单号查询")
    @GetMapping("/{shipmentNo}")
    public Result<Shipment> getShipmentByNo(@PathVariable String shipmentNo) {
        return Result.success(shipmentService.getShipmentByNo(shipmentNo));
    }

    @Operation(summary = "查询发运明细")
    @GetMapping("/{shipmentNo}/details")
    public Result<List<ShipmentDetail>> getShipmentDetails(@PathVariable String shipmentNo) {
        return Result.success(shipmentService.getShipmentDetails(shipmentNo));
    }

    // ============================================================

    // 承运商选择
    // ============================================================

    @Operation(summary = "选择承运商")
    @PostMapping("/select-carrier")
    public Result<String> selectCarrier(
            @RequestParam String receiverAddress,
            @RequestParam BigDecimal weight,
            @RequestParam(required = false) String serviceType,
            @RequestParam(required = false) String ownerCode) {
        return Result.success(
                shipmentService.selectCarrier(receiverAddress, weight, serviceType, ownerCode));
    }

    // ============================================================

    // 快递单获取
    // ============================================================

    @Operation(summary = "获取快递单号")
    @PostMapping("/{shipmentNo}/get-tracking")
    public Result<ExpressOrder> getTrackingNo(
            @PathVariable String shipmentNo,
            @RequestParam String carrier,
            @RequestParam String serviceType,
            @RequestParam String senderName,
            @RequestParam String senderPhone,
            @RequestParam String senderAddress,
            @RequestParam String receiverName,
            @RequestParam String receiverPhone,
            @RequestParam String receiverAddress,
            @RequestParam BigDecimal weight,
            @RequestParam(required = false) BigDecimal volume) {
        return Result.success(
                shipmentService.getTrackingNo(
                        shipmentNo,
                        carrier,
                        serviceType,
                        senderName,
                        senderPhone,
                        senderAddress,
                        receiverName,
                        receiverPhone,
                        receiverAddress,
                        weight,
                        volume));
    }

    @Operation(summary = "批量获取快递单号")
    @PostMapping("/batch-get-tracking")
    public Result<List<String>> batchGetTrackingNo(
            @RequestBody List<String> shipmentNos, @RequestParam String carrier) {
        return Result.success(shipmentService.batchGetTrackingNo(shipmentNos, carrier));
    }

    // ============================================================

    // 快递单打印
    // ============================================================

    @Operation(summary = "打印快递单")
    @PostMapping("/express/{expressNo}/print")
    public Result<ExpressOrder> printExpress(
            @PathVariable String expressNo, @RequestParam(required = false) String operator) {
        return Result.success(shipmentService.printExpress(expressNo, operator));
    }

    @Operation(summary = "批量打印快递单")
    @PostMapping("/express/batch-print")
    public Result<List<String>> batchPrintExpress(
            @RequestBody List<String> expressNos, @RequestParam(required = false) String operator) {
        return Result.success(shipmentService.batchPrintExpress(expressNos, operator));
    }

    // ============================================================

    // 发运确认
    // ============================================================

    @Operation(summary = "发运确认")
    @PostMapping("/{shipmentNo}/confirm")
    public Result<Shipment> confirmShipment(
            @PathVariable String shipmentNo, @RequestParam(required = false) String operator) {
        return Result.success(shipmentService.confirmShipment(shipmentNo, operator));
    }

    // ============================================================

    // 快递状态回传
    // ============================================================

    @Operation(summary = "快递状态回传")
    @PutMapping("/express/{trackingNo}/status")
    public Result<ExpressOrder> updateExpressStatus(
            @PathVariable String trackingNo,
            @RequestParam String status,
            @RequestParam(required = false) String errorMsg) {
        return Result.success(shipmentService.updateExpressStatus(trackingNo, status, errorMsg));
    }

    // ============================================================

    // 重试机制
    // ============================================================

    @Operation(summary = "重试失败快递单")
    @PostMapping("/express/retry")
    public Result<Integer> retryFailedExpress(@RequestParam(required = false) String carrier) {
        return Result.success(shipmentService.retryFailedExpress(carrier));
    }

    // ============================================================

    // 查询
    // ============================================================

    @Operation(summary = "查询快递单列表")
    @GetMapping("/{shipmentNo}/express")
    public Result<List<ExpressOrder>> getExpressOrders(@PathVariable String shipmentNo) {
        return Result.success(shipmentService.getExpressOrders(shipmentNo));
    }

    @Operation(summary = "按运单号查询")
    @GetMapping("/express/tracking/{trackingNo}")
    public Result<ExpressOrder> getExpressByTrackingNo(@PathVariable String trackingNo) {
        return Result.success(shipmentService.getExpressByTrackingNo(trackingNo));
    }

    @Operation(summary = "查询发运作业记录")
    @GetMapping("/{shipmentNo}/tasks")
    public Result<List<ShipmentTask>> getShipmentTasks(@PathVariable String shipmentNo) {
        return Result.success(shipmentService.getShipmentTasks(shipmentNo));
    }
}
