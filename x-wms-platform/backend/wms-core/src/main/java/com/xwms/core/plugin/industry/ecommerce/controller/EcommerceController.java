package com.xwms.core.plugin.industry.ecommerce.controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.common.core.Result;
import com.xwms.core.plugin.industry.ecommerce.entity.*;
import com.xwms.core.plugin.industry.ecommerce.service.EcommerceService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/** 电商行业控制器 */
@Tag(name = "电商行业管理", description = "预售订单/秒杀活动/波次配置")
@RestController
@RequestMapping("/api/ecommerce")
@RequiredArgsConstructor
public class EcommerceController {

    private final EcommerceService ecommerceService;

    // ============================================================

    // 1. 预售订单管理
    // ============================================================

    @Operation(summary = "创建预售订单")
    @PostMapping("/pre-sale")
    public Result<PreSaleOrder> createPreSaleOrder(@RequestBody PreSaleOrder order) {
        return Result.success(ecommerceService.createPreSaleOrder(order));
    }

    @Operation(summary = "支付定金")
    @PutMapping("/pre-sale/{preSaleNo}/deposit/pay")
    public Result<PreSaleOrder> payDeposit(
            @PathVariable String preSaleNo, @RequestParam String operator) {
        return Result.success(ecommerceService.payDeposit(preSaleNo, operator));
    }

    @Operation(summary = "支付尾款")
    @PutMapping("/pre-sale/{preSaleNo}/balance/pay")
    public Result<PreSaleOrder> payBalance(
            @PathVariable String preSaleNo, @RequestParam String operator) {
        return Result.success(ecommerceService.payBalance(preSaleNo, operator));
    }

    @Operation(summary = "预售订单发货")
    @PutMapping("/pre-sale/{preSaleNo}/ship")
    public Result<PreSaleOrder> shipPreSaleOrder(
            @PathVariable String preSaleNo,
            @RequestParam BigDecimal shipQty,
            @RequestParam String operator) {
        return Result.success(ecommerceService.shipPreSaleOrder(preSaleNo, shipQty, operator));
    }

    @Operation(summary = "取消预售订单")
    @PutMapping("/pre-sale/{preSaleNo}/cancel")
    public Result<PreSaleOrder> cancelPreSaleOrder(
            @PathVariable String preSaleNo,
            @RequestParam(required = false) String reason,
            @RequestParam String operator) {
        return Result.success(ecommerceService.cancelPreSaleOrder(preSaleNo, reason, operator));
    }

    @Operation(summary = "查询预售订单详情")
    @GetMapping("/pre-sale/{id}")
    public Result<PreSaleOrder> getPreSaleOrder(@PathVariable Long id) {
        return Result.success(ecommerceService.getPreSaleOrder(id));
    }

    @Operation(summary = "根据编号查询预售订单")
    @GetMapping("/pre-sale/no/{preSaleNo}")
    public Result<PreSaleOrder> getPreSaleOrderByNo(@PathVariable String preSaleNo) {
        return Result.success(ecommerceService.getPreSaleOrderByNo(preSaleNo));
    }

    @Operation(summary = "根据订单号查询预售订单")
    @GetMapping("/pre-sale/order/{orderNo}")
    public Result<PreSaleOrder> getPreSaleOrderByOrderNo(@PathVariable String orderNo) {
        return Result.success(ecommerceService.getPreSaleOrderByOrderNo(orderNo));
    }

    @Operation(summary = "查询待付尾款的预售单")
    @GetMapping("/pre-sale/pending-balance")
    public Result<List<PreSaleOrder>> getPendingBalanceOrders() {
        return Result.success(ecommerceService.getPendingBalanceOrders());
    }

    @Operation(summary = "查询待发货的预售单")
    @GetMapping("/pre-sale/pending-ship")
    public Result<List<PreSaleOrder>> getPendingShipOrders() {
        return Result.success(ecommerceService.getPendingShipOrders());
    }

    @Operation(summary = "分页查询预售订单")
    @GetMapping("/pre-sale")
    public Result<Page<PreSaleOrder>> pagePreSaleOrders(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String shopCode,
            @RequestParam(required = false) String platformCode,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String shipStatus) {
        return Result.success(
                ecommerceService.pagePreSaleOrders(
                        new Page<>(page, size), shopCode, platformCode, status, shipStatus));
    }

    // ============================================================

    // 2. 秒杀活动管理
    // ============================================================

    @Operation(summary = "创建秒杀活动")
    @PostMapping("/flash-sale")
    public Result<FlashSaleActivity> createFlashSaleActivity(
            @RequestBody FlashSaleActivity activity) {
        return Result.success(ecommerceService.createFlashSaleActivity(activity));
    }

    @Operation(summary = "发布秒杀活动")
    @PutMapping("/flash-sale/{id}/publish")
    public Result<FlashSaleActivity> publishActivity(@PathVariable Long id) {
        return Result.success(ecommerceService.publishActivity(id));
    }

    @Operation(summary = "开始秒杀活动")
    @PutMapping("/flash-sale/{id}/start")
    public Result<FlashSaleActivity> startActivity(@PathVariable Long id) {
        return Result.success(ecommerceService.startActivity(id));
    }

    @Operation(summary = "结束秒杀活动")
    @PutMapping("/flash-sale/{id}/end")
    public Result<FlashSaleActivity> endActivity(@PathVariable Long id) {
        return Result.success(ecommerceService.endActivity(id));
    }

    @Operation(summary = "锁定秒杀库存")
    @PutMapping("/flash-sale/{id}/stock/lock")
    public Result<FlashSaleActivity> lockStock(@PathVariable Long id, @RequestParam String waveNo) {
        return Result.success(ecommerceService.lockStock(id, waveNo));
    }

    @Operation(summary = "释放秒杀库存")
    @PutMapping("/flash-sale/{id}/stock/release")
    public Result<FlashSaleActivity> releaseStock(@PathVariable Long id) {
        return Result.success(ecommerceService.releaseStock(id));
    }

    @Operation(summary = "秒杀售出")
    @PutMapping("/flash-sale/{id}/sell")
    public Result<FlashSaleActivity> sellStock(
            @PathVariable Long id, @RequestParam BigDecimal qty) {
        return Result.success(ecommerceService.sellStock(id, qty));
    }

    @Operation(summary = "查询秒杀活动详情")
    @GetMapping("/flash-sale/{id}")
    public Result<FlashSaleActivity> getActivity(@PathVariable Long id) {
        return Result.success(ecommerceService.getActivity(id));
    }

    @Operation(summary = "根据编号查询秒杀活动")
    @GetMapping("/flash-sale/no/{activityNo}")
    public Result<FlashSaleActivity> getActivityByNo(@PathVariable String activityNo) {
        return Result.success(ecommerceService.getActivityByNo(activityNo));
    }

    @Operation(summary = "查询进行中的秒杀活动")
    @GetMapping("/flash-sale/active")
    public Result<List<FlashSaleActivity>> getActiveActivities() {
        return Result.success(ecommerceService.getActiveActivities());
    }

    @Operation(summary = "查询待开始的秒杀活动")
    @GetMapping("/flash-sale/pending")
    public Result<List<FlashSaleActivity>> getPendingActivities() {
        return Result.success(ecommerceService.getPendingActivities());
    }

    @Operation(summary = "查询库存未锁定的秒杀活动")
    @GetMapping("/flash-sale/unlocked-stock")
    public Result<List<FlashSaleActivity>> getUnlockedStockActivities() {
        return Result.success(ecommerceService.getUnlockedStockActivities());
    }

    @Operation(summary = "分页查询秒杀活动")
    @GetMapping("/flash-sale")
    public Result<Page<FlashSaleActivity>> pageActivities(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String shopCode,
            @RequestParam(required = false) String platformCode,
            @RequestParam(required = false) String activityType,
            @RequestParam(required = false) String status) {
        return Result.success(
                ecommerceService.pageActivities(
                        new Page<>(page, size), shopCode, platformCode, activityType, status));
    }

    // ============================================================

    // 3. 电商波次配置管理
    // ============================================================

    @Operation(summary = "创建波次配置")
    @PostMapping("/wave-config")
    public Result<EcommerceWaveConfig> createWaveConfig(@RequestBody EcommerceWaveConfig config) {
        return Result.success(ecommerceService.createWaveConfig(config));
    }

    @Operation(summary = "更新波次配置")
    @PutMapping("/wave-config/{id}")
    public Result<EcommerceWaveConfig> updateWaveConfig(
            @PathVariable Long id, @RequestBody EcommerceWaveConfig config) {
        config.setId(id);
        return Result.success(ecommerceService.updateWaveConfig(config));
    }

    @Operation(summary = "查询波次配置详情")
    @GetMapping("/wave-config/{id}")
    public Result<EcommerceWaveConfig> getWaveConfig(@PathVariable Long id) {
        return Result.success(ecommerceService.getWaveConfig(id));
    }

    @Operation(summary = "根据编号查询波次配置")
    @GetMapping("/wave-config/code/{configCode}")
    public Result<EcommerceWaveConfig> getWaveConfigByCode(@PathVariable String configCode) {
        return Result.success(ecommerceService.getWaveConfigByCode(configCode));
    }

    @Operation(summary = "查询启用的波次配置")
    @GetMapping("/wave-config/enabled")
    public Result<List<EcommerceWaveConfig>> getEnabledWaveConfigs() {
        return Result.success(ecommerceService.getEnabledWaveConfigs());
    }

    @Operation(summary = "根据店铺和活动类型查询波次配置")
    @GetMapping("/wave-config/shop/{shopCode}/activity/{activityType}")
    public Result<List<EcommerceWaveConfig>> getWaveConfigsByShopAndActivityType(
            @PathVariable String shopCode, @PathVariable String activityType) {
        return Result.success(
                ecommerceService.getWaveConfigsByShopAndActivityType(shopCode, activityType));
    }

    @Operation(summary = "根据活动类型查询启用的波次配置")
    @GetMapping("/wave-config/activity/{activityType}/enabled")
    public Result<List<EcommerceWaveConfig>> getEnabledWaveConfigsByActivityType(
            @PathVariable String activityType) {
        return Result.success(ecommerceService.getEnabledWaveConfigsByActivityType(activityType));
    }

    @Operation(summary = "分页查询波次配置")
    @GetMapping("/wave-config")
    public Result<Page<EcommerceWaveConfig>> pageWaveConfigs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String shopCode,
            @RequestParam(required = false) String platformCode,
            @RequestParam(required = false) String activityType,
            @RequestParam(required = false) String enabled) {
        return Result.success(
                ecommerceService.pageWaveConfigs(
                        new Page<>(page, size), shopCode, platformCode, activityType, enabled));
    }

    @Operation(summary = "启用波次配置")
    @PutMapping("/wave-config/{id}/enable")
    public Result<EcommerceWaveConfig> enableWaveConfig(@PathVariable Long id) {
        return Result.success(ecommerceService.enableWaveConfig(id));
    }

    @Operation(summary = "禁用波次配置")
    @PutMapping("/wave-config/{id}/disable")
    public Result<EcommerceWaveConfig> disableWaveConfig(@PathVariable Long id) {
        return Result.success(ecommerceService.disableWaveConfig(id));
    }
}
