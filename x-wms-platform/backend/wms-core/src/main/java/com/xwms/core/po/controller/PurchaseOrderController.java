package com.xwms.core.po.controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.common.core.Result;
import com.xwms.core.inbound.entity.Asn;
import com.xwms.core.po.entity.*;
import com.xwms.core.po.service.PurchaseOrderService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/** 采购订单创建请求 */
@lombok.Data
class PurchaseOrderCreateRequest {
    private PurchaseOrder po;
    private List<PurchaseOrderDetail> details;
}

/** 采购订单（PO）控制器 */
@Tag(name = "采购订单管理", description = "PO管理/PO提取生成ASN/PO收货更新/状态控制")
@RestController
@RequestMapping("/api/po")
@RequiredArgsConstructor
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;

    // ============================================================

    // 1. PO管理
    // ============================================================

    @Operation(summary = "创建采购订单")
    @PostMapping
    public Result<PurchaseOrder> createPo(@RequestBody PurchaseOrderCreateRequest request) {
        return Result.success(purchaseOrderService.createPo(request.getPo(), request.getDetails()));
    }

    @Operation(summary = "更新采购订单")
    @PutMapping("/{id}")
    public Result<PurchaseOrder> updatePo(@PathVariable Long id, @RequestBody PurchaseOrder po) {
        po.setId(id);
        return Result.success(purchaseOrderService.updatePo(po));
    }

    @Operation(summary = "审批采购订单")
    @PutMapping("/{poNo}/approve")
    public Result<PurchaseOrder> approvePo(
            @PathVariable String poNo,
            @RequestParam String approver,
            @RequestParam(required = false) String approvalOpinion) {
        return Result.success(purchaseOrderService.approvePo(poNo, approver, approvalOpinion));
    }

    @Operation(summary = "取消采购订单")
    @PutMapping("/{poNo}/cancel")
    public Result<PurchaseOrder> cancelPo(
            @PathVariable String poNo,
            @RequestParam String cancelReason,
            @RequestParam String operator) {
        return Result.success(purchaseOrderService.cancelPo(poNo, cancelReason, operator));
    }

    @Operation(summary = "根据PO号查询")
    @GetMapping("/{poNo}")
    public Result<PurchaseOrder> getPoByNo(@PathVariable String poNo) {
        return Result.success(purchaseOrderService.getPoByNo(poNo));
    }

    @Operation(summary = "分页查询采购订单")
    @GetMapping
    public Result<Page<PurchaseOrder>> pagePos(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String poType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String supplierCode,
            @RequestParam(required = false) String warehouseCode,
            @RequestParam(required = false) String ownerCode) {
        return Result.success(
                purchaseOrderService.pagePos(
                        new Page<>(page, size),
                        poType,
                        status,
                        supplierCode,
                        warehouseCode,
                        ownerCode));
    }

    // ============================================================

    // 2. PO明细管理
    // ============================================================

    @Operation(summary = "查询PO明细列表")
    @GetMapping("/{poNo}/details")
    public Result<List<PurchaseOrderDetail>> getPoDetails(@PathVariable String poNo) {
        return Result.success(purchaseOrderService.getPoDetails(poNo));
    }

    @Operation(summary = "根据明细号查询PO明细")
    @GetMapping("/detail/{detailNo}")
    public Result<PurchaseOrderDetail> getPoDetailByNo(@PathVariable String detailNo) {
        return Result.success(purchaseOrderService.getPoDetailByNo(detailNo));
    }

    @Operation(summary = "查询可释放的PO明细")
    @GetMapping("/{poNo}/releasable")
    public Result<List<PurchaseOrderDetail>> getReleasableDetails(@PathVariable String poNo) {
        return Result.success(purchaseOrderService.getReleasableDetails(poNo));
    }

    // ============================================================

    // 3. PO提取生成ASN（核心功能）
    // ============================================================

    @Operation(summary = "从PO提取生成ASN")
    @PostMapping("/release-to-asn")
    public Result<Asn> releaseToAsn(
            @RequestParam String asnNo,
            @RequestParam String warehouseCode,
            @RequestParam String ownerCode,
            @RequestParam(required = false) String supplierCode,
            @RequestBody List<PurchaseOrderService.ReleaseItem> releaseItems) {
        Asn asnHeader = new Asn();
        asnHeader.setAsnNo(asnNo);
        asnHeader.setWarehouseCode(warehouseCode);
        asnHeader.setOwnerCode(ownerCode);
        asnHeader.setSupplierCode(supplierCode);
        return Result.success(purchaseOrderService.releaseToAsn(asnHeader, releaseItems));
    }

    // ============================================================

    // 4. PO收货数量更新
    // ============================================================

    @Operation(summary = "更新PO收货数量（ASN收货后调用）")
    @PutMapping("/received-qty")
    public Result<Void> updatePoReceivedQty(
            @RequestParam String asnNo,
            @RequestParam String skuCode,
            @RequestParam BigDecimal receivedQty) {
        purchaseOrderService.updatePoReceivedQty(asnNo, skuCode, receivedQty);
        return Result.success();
    }

    // ============================================================

    // 5. PO-ASN关联查询
    // ============================================================

    @Operation(summary = "根据PO号查询关联的ASN列表")
    @GetMapping("/{poNo}/relations")
    public Result<List<PoAsnRelation>> getRelationsByPoNo(@PathVariable String poNo) {
        return Result.success(purchaseOrderService.getRelationsByPoNo(poNo));
    }

    @Operation(summary = "根据ASN号查询关联的PO列表")
    @GetMapping("/asn/{asnNo}/relations")
    public Result<List<PoAsnRelation>> getRelationsByAsnNo(@PathVariable String asnNo) {
        return Result.success(purchaseOrderService.getRelationsByAsnNo(asnNo));
    }
}
