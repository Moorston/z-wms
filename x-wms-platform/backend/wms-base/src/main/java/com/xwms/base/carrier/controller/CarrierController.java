package com.xwms.base.carrier.controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.base.carrier.entity.*;
import com.xwms.base.carrier.service.CarrierServiceImpl;
import com.xwms.common.core.Result;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/** 承运商管理 Controller */
@Tag(name = "承运商管理", description = "承运商档案/服务/价格/账户")
@RestController
@RequestMapping("/api/carrier")
@RequiredArgsConstructor
public class CarrierController {

    private final CarrierServiceImpl carrierService;

    // ============================================================
    // 承运商档案
    // ============================================================

    @Operation(summary = "创建承运商")
    @PostMapping
    public Result<Carrier> createCarrier(
            @RequestBody Carrier carrier, @RequestParam(required = false) String operator) {
        return Result.success(carrierService.createCarrier(carrier, operator));
    }

    @Operation(summary = "更新承运商")
    @PutMapping
    public Result<Carrier> updateCarrier(@RequestBody Carrier carrier) {
        return Result.success(carrierService.updateCarrier(carrier));
    }

    @Operation(summary = "分页查询承运商")
    @GetMapping
    public Result<Page<Carrier>> pageCarriers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String carrierType,
            @RequestParam(required = false) String status) {
        return Result.success(
                carrierService.pageCarriers(new Page<>(page, size), carrierType, status));
    }

    @Operation(summary = "按编码查询承运商")
    @GetMapping("/{carrierCode}")
    public Result<Carrier> getCarrierByCode(@PathVariable String carrierCode) {
        return Result.success(carrierService.getCarrierByCode(carrierCode));
    }

    @Operation(summary = "查询所有启用承运商")
    @GetMapping("/active")
    public Result<List<Carrier>> getActiveCarriers() {
        return Result.success(carrierService.getActiveCarriers());
    }

    // ============================================================
    // 承运商服务
    // ============================================================

    @Operation(summary = "创建承运商服务")
    @PostMapping("/service")
    public Result<CarrierService> createService(@RequestBody CarrierService service) {
        return Result.success(carrierService.createService(service));
    }

    @Operation(summary = "查询承运商服务列表")
    @GetMapping("/{carrierCode}/services")
    public Result<List<CarrierService>> getServicesByCarrier(@PathVariable String carrierCode) {
        return Result.success(carrierService.getServicesByCarrier(carrierCode));
    }

    @Operation(summary = "查询承运商服务详情")
    @GetMapping("/{carrierCode}/service/{serviceCode}")
    public Result<CarrierService> getService(
            @PathVariable String carrierCode, @PathVariable String serviceCode) {
        return Result.success(carrierService.getService(carrierCode, serviceCode));
    }

    // ============================================================
    // 运费计算
    // ============================================================

    @Operation(summary = "计算运费")
    @PostMapping("/calculate-fee")
    public Result<BigDecimal> calculateShippingFee(
            @RequestParam String carrierCode,
            @RequestParam String serviceCode,
            @RequestParam(required = false) String regionCode,
            @RequestParam BigDecimal weight,
            @RequestParam(required = false) BigDecimal volume) {
        return Result.success(
                carrierService.calculateShippingFee(
                        carrierCode, serviceCode, regionCode, weight, volume));
    }

    @Operation(summary = "比价选择最优承运商")
    @PostMapping("/select-best")
    public Result<Carrier> selectBestCarrier(
            @RequestParam(required = false) String regionCode,
            @RequestParam BigDecimal weight,
            @RequestParam(required = false) String serviceType,
            @RequestParam(required = false) String ownerCode) {
        return Result.success(
                carrierService.selectBestCarrier(regionCode, weight, serviceType, ownerCode));
    }

    // ============================================================
    // 价格管理
    // ============================================================

    @Operation(summary = "创建价格配置")
    @PostMapping("/price")
    public Result<CarrierPrice> createPrice(@RequestBody CarrierPrice price) {
        return Result.success(carrierService.createPrice(price));
    }

    @Operation(summary = "查询价格配置")
    @GetMapping("/{carrierCode}/service/{serviceCode}/prices")
    public Result<List<CarrierPrice>> getPrices(
            @PathVariable String carrierCode, @PathVariable String serviceCode) {
        return Result.success(
                carrierService.getPricesByCarrierAndService(carrierCode, serviceCode));
    }

    // ============================================================
    // 账户管理
    // ============================================================

    @Operation(summary = "创建账户")
    @PostMapping("/account")
    public Result<CarrierAccount> createAccount(@RequestBody CarrierAccount account) {
        return Result.success(carrierService.createAccount(account));
    }

    @Operation(summary = "扣减账户余额")
    @PostMapping("/account/deduct")
    public Result<Boolean> deductBalance(
            @RequestParam String carrierCode,
            @RequestParam String accountNo,
            @RequestParam BigDecimal amount) {
        return Result.success(carrierService.deductBalance(carrierCode, accountNo, amount));
    }

    @Operation(summary = "账户充值")
    @PostMapping("/account/recharge")
    public Result<Boolean> recharge(
            @RequestParam String carrierCode,
            @RequestParam String accountNo,
            @RequestParam BigDecimal amount) {
        return Result.success(carrierService.recharge(carrierCode, accountNo, amount));
    }

    @Operation(summary = "查询账户列表")
    @GetMapping("/{carrierCode}/accounts")
    public Result<List<CarrierAccount>> getAccountsByCarrier(@PathVariable String carrierCode) {
        return Result.success(carrierService.getAccountsByCarrier(carrierCode));
    }

    @Operation(summary = "查询账户详情")
    @GetMapping("/{carrierCode}/account/{accountNo}")
    public Result<CarrierAccount> getAccount(
            @PathVariable String carrierCode, @PathVariable String accountNo) {
        return Result.success(carrierService.getAccount(carrierCode, accountNo));
    }
}
