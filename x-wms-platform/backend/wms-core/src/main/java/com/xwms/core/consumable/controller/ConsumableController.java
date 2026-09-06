package com.xwms.core.consumable.controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.xwms.core.consumable.entity.*;
import com.xwms.core.consumable.service.ConsumableService;

import lombok.RequiredArgsConstructor;

/** 耗材管理Controller */
@RestController
@RequestMapping("/api/consumable")
@RequiredArgsConstructor
public class ConsumableController {

    private final ConsumableService consumableService;

    // ==================== 耗材SKU维护 ====================

    /** 创建耗材 */
    @PostMapping("/create")
    public Consumable createConsumable(
            @RequestBody Consumable consumable,
            @RequestParam(required = false, defaultValue = "system") String creator) {
        return consumableService.createConsumable(consumable, creator);
    }

    /** 更新耗材 */
    @PutMapping("/update")
    public Consumable updateConsumable(
            @RequestBody Consumable consumable,
            @RequestParam(required = false, defaultValue = "system") String updater) {
        return consumableService.updateConsumable(consumable, updater);
    }

    /** 耗材入库 */
    @PostMapping("/{consumableCode}/stock-in")
    public Consumable stockIn(
            @PathVariable String consumableCode,
            @RequestParam BigDecimal qty,
            @RequestParam(required = false) String refNo,
            @RequestParam(required = false, defaultValue = "system") String operator) {
        return consumableService.stockIn(consumableCode, qty, refNo, operator);
    }

    /** 查询耗材详情 */
    @GetMapping("/{consumableCode}")
    public Consumable getConsumable(@PathVariable String consumableCode) {
        return consumableService.getConsumable(consumableCode);
    }

    /** 查询所有耗材 */
    @GetMapping("/list")
    public List<Consumable> getAllConsumables() {
        return consumableService.getAllConsumables();
    }

    /** 查询库存不足的耗材 */
    @GetMapping("/low-stock")
    public List<Consumable> getLowStockConsumables() {
        return consumableService.getLowStockConsumables();
    }

    // ==================== 产品包装关联 ====================

    /** 创建产品包装关联 */
    @PostMapping("/packaging/create")
    public ProductPackaging createProductPackaging(
            @RequestBody ProductPackaging packaging,
            @RequestParam(required = false, defaultValue = "system") String creator) {
        return consumableService.createProductPackaging(packaging, creator);
    }

    /** 根据商品查询包装关联 */
    @GetMapping("/packaging/sku/{skuCode}")
    public List<ProductPackaging> getProductPackagings(@PathVariable String skuCode) {
        return consumableService.getProductPackagings(skuCode);
    }

    // ==================== 入库耗材扣减 ====================

    /** 入库耗材扣减 */
    @PostMapping("/deduct/inbound")
    public List<ConsumableRecord> deductInboundConsumables(
            @RequestParam String inboundNo,
            @RequestParam(required = false) String asnNo,
            @RequestParam String skuCode,
            @RequestParam String skuName,
            @RequestParam BigDecimal productQty,
            @RequestParam String ownerCode,
            @RequestParam String warehouseCode,
            @RequestParam(required = false, defaultValue = "system") String operator) {
        return consumableService.deductInboundConsumables(
                inboundNo, asnNo, skuCode, skuName, productQty, ownerCode, warehouseCode, operator);
    }

    // ==================== 出库耗材扣减 ====================

    /** 出库耗材扣减 */
    @PostMapping("/deduct/outbound")
    public List<ConsumableRecord> deductOutboundConsumables(
            @RequestParam String outboundNo,
            @RequestParam String skuCode,
            @RequestParam String skuName,
            @RequestParam BigDecimal productQty,
            @RequestParam String ownerCode,
            @RequestParam String warehouseCode,
            @RequestParam(required = false, defaultValue = "system") String operator) {
        return consumableService.deductOutboundConsumables(
                outboundNo, skuCode, skuName, productQty, ownerCode, warehouseCode, operator);
    }

    // ==================== 查询方法 ====================

    /** 查询耗材扣减记录 */
    @GetMapping("/records/{refNo}")
    public List<ConsumableRecord> getRecordsByRefNo(@PathVariable String refNo) {
        return consumableService.getRecordsByRefNo(refNo);
    }
}
