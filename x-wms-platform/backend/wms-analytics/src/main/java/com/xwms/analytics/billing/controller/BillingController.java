package com.xwms.analytics.billing.controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.analytics.billing.dto.BillingCreateRequest;
import com.xwms.analytics.billing.entity.*;
import com.xwms.analytics.billing.service.BillingService;
import com.xwms.common.core.Result;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/** 费收计费 Controller */
@Tag(name = "费收计费", description = "计费规则/费用入账/账单管理/结算付款")
@RestController
@RequestMapping("/api/billing")
@RequiredArgsConstructor
public class BillingController {

    private final BillingService billingService;

    // ============================================================

    // 计费规则
    // ============================================================

    @Operation(summary = "创建计费规则")
    @PostMapping("/rules")
    public Result<BillingRule> createRule(@RequestBody BillingRule rule) {
        return Result.success(billingService.createRule(rule));
    }

    @Operation(summary = "查询计费规则详情")
    @GetMapping("/rules/{id}")
    public Result<BillingRule> getRule(@PathVariable Long id) {
        return Result.success(billingService.getRule(id));
    }

    @Operation(summary = "分页查询计费规则")
    @GetMapping("/rules")
    public Result<Page<BillingRule>> pageRules(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String feeType,
            @RequestParam(required = false) String status) {
        return Result.success(billingService.pageRules(new Page<>(page, size), feeType, status));
    }

    // ============================================================

    // 费用入账
    // ============================================================

    @Operation(summary = "计算并入账费用")
    @PostMapping("/fees")
    public Result<BillingFeeItem> createFee(@RequestBody BillingCreateRequest request) {
        return Result.success(
                billingService.calculateAndCreateFee(
                        request.getFeeType(),
                        request.getOwnerCode(),
                        request.getCustomerCode(),
                        request.getWarehouseCode(),
                        request.getRefType(),
                        request.getRefNo(),
                        request.getQuantity(),
                        request.getWeight(),
                        request.getVolume(),
                        request.getDays(),
                        request.getSku(),
                        request.getProductName()));
    }

    @Operation(summary = "分页查询费用流水")
    @GetMapping("/fees")
    public Result<Page<BillingFeeItem>> pageFees(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String ownerCode,
            @RequestParam(required = false) String period) {
        return Result.success(
                billingService.pageFees(new Page<>(page, size), status, ownerCode, period));
    }

    // ============================================================

    // 账单管理
    // ============================================================

    @Operation(summary = "生成月结账单")
    @PostMapping("/bills/generate")
    public Result<BillingBill> generateMonthlyBill(
            @RequestParam String ownerCode, @RequestParam String period) {
        return Result.success(billingService.generateMonthlyBill(ownerCode, period));
    }

    @Operation(summary = "确认账单")
    @PutMapping("/bills/{id}/confirm")
    public Result<BillingBill> confirmBill(@PathVariable Long id) {
        return Result.success(billingService.confirmBill(id));
    }

    @Operation(summary = "账单开票")
    @PutMapping("/bills/{id}/invoice")
    public Result<BillingBill> invoiceBill(@PathVariable Long id, @RequestParam String invoiceNo) {
        return Result.success(billingService.invoiceBill(id, invoiceNo));
    }

    @Operation(summary = "查询账单详情")
    @GetMapping("/bills/{id}")
    public Result<BillingBill> getBill(@PathVariable Long id) {
        return Result.success(billingService.getBill(id));
    }

    @Operation(summary = "分页查询账单")
    @GetMapping("/bills")
    public Result<Page<BillingBill>> pageBills(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String ownerCode,
            @RequestParam(required = false) String period) {
        return Result.success(
                billingService.pageBills(new Page<>(page, size), status, ownerCode, period));
    }

    @Operation(summary = "查询账单明细")
    @GetMapping("/bills/{id}/items")
    public Result<List<BillingBillItem>> getBillItems(@PathVariable Long id) {
        return Result.success(billingService.getBillItems(id));
    }

    // ============================================================

    // 结算付款
    // ============================================================

    @Operation(summary = "账单结算付款")
    @PostMapping("/bills/{id}/settle")
    public Result<BillingSettlement> settleBill(
            @PathVariable Long id,
            @RequestParam BigDecimal amount,
            @RequestParam String paymentMethod,
            @RequestParam(required = false) String paymentRef,
            @RequestParam String operator) {
        return Result.success(
                billingService.settleBill(id, amount, paymentMethod, paymentRef, operator));
    }

    @Operation(summary = "查询账单结算记录")
    @GetMapping("/bills/{id}/settlements")
    public Result<List<BillingSettlement>> getSettlements(@PathVariable Long id) {
        return Result.success(billingService.getSettlements(id));
    }
}
