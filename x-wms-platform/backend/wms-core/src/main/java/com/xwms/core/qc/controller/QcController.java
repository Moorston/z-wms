package com.xwms.core.qc.controller;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.common.core.Result;
import com.xwms.core.qc.dto.QcConcessionRequest;
import com.xwms.core.qc.dto.QcCreateRequest;
import com.xwms.core.qc.dto.QcSubmitRequest;
import com.xwms.core.qc.entity.*;
import com.xwms.core.qc.service.QcOrderService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/** 质检管理 Controller */
@Tag(name = "质检管理", description = "质检单/质检规则/抽样方案/不合格品/让步接收")
@RestController
@RequestMapping("/api/qc")
@RequiredArgsConstructor
public class QcController {

    private final QcOrderService qcOrderService;

    // ============================================================

    // 质检单
    // ============================================================

    @Operation(summary = "创建质检单")
    @PostMapping("/orders")
    public Result<QcOrder> createQcOrder(@RequestBody QcCreateRequest request) {
        return Result.success(qcOrderService.createQcOrder(request));
    }

    @Operation(summary = "开始质检")
    @PutMapping("/orders/{id}/start")
    public Result<QcOrder> startQc(@PathVariable Long id, @RequestParam String inspector) {
        return Result.success(qcOrderService.startQc(id, inspector));
    }

    @Operation(summary = "提交质检结果")
    @PutMapping("/orders/submit")
    public Result<QcOrder> submitQcResult(@RequestBody QcSubmitRequest request) {
        return Result.success(qcOrderService.submitQcResult(request));
    }

    @Operation(summary = "查询质检单详情")
    @GetMapping("/orders/{id}")
    public Result<QcOrder> getQcOrder(@PathVariable Long id) {
        return Result.success(qcOrderService.getQcOrder(id));
    }

    @Operation(summary = "分页查询质检单")
    @GetMapping("/orders")
    public Result<Page<QcOrder>> pageQcOrders(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String sku,
            @RequestParam(required = false) String refNo) {
        return Result.success(
                qcOrderService.pageQcOrders(new Page<>(page, size), status, sku, refNo));
    }

    @Operation(summary = "查询质检明细")
    @GetMapping("/orders/{id}/items")
    public Result<List<QcItem>> getQcItems(@PathVariable Long id) {
        return Result.success(qcOrderService.getQcItems(id));
    }

    // ============================================================

    // 让步接收
    // ============================================================

    @Operation(summary = "申请让步接收")
    @PostMapping("/concession/apply")
    public Result<QcConcession> applyConcession(@RequestBody QcConcessionRequest request) {
        return Result.success(qcOrderService.applyConcession(request));
    }

    @Operation(summary = "审批让步接收")
    @PutMapping("/concession/{id}/approve")
    public Result<QcConcession> approveConcession(
            @PathVariable Long id,
            @RequestParam boolean approved,
            @RequestParam String approver,
            @RequestParam(required = false) String opinion) {
        return Result.success(qcOrderService.approveConcession(id, approved, approver, opinion));
    }

    // ============================================================

    // 不合格品
    // ============================================================

    @Operation(summary = "处理不合格品")
    @PutMapping("/unqualified/{id}/handle")
    public Result<QcUnqualified> handleUnqualified(
            @PathVariable Long id,
            @RequestParam String handleMethod,
            @RequestParam String handler,
            @RequestParam(required = false) String cert) {
        return Result.success(qcOrderService.handleUnqualified(id, handleMethod, handler, cert));
    }
}
