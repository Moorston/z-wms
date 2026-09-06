package com.xwms.core.combo.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.common.core.Result;
import com.xwms.core.combo.entity.*;
import com.xwms.core.combo.service.InventoryComboService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/** 库存组合管理 Controller */
@Tag(name = "库存组合管理", description = "组合规则/组合明细/组合组装/组合拆解")
@RestController
@RequestMapping("/api/combo")
@RequiredArgsConstructor
public class InventoryComboController {

    private final InventoryComboService comboService;

    // ============================================================

    // 组合规则
    // ============================================================

    @Operation(summary = "创建组合规则")
    @PostMapping("/rule")
    public Result<ComboRule> createRule(@RequestBody Map<String, Object> request) {
        ComboRule rule = new ComboRule();
        rule.setComboCode((String) request.get("comboCode"));
        rule.setComboName((String) request.get("comboName"));
        rule.setWarehouseCode((String) request.get("warehouseCode"));
        rule.setOwnerCode((String) request.get("ownerCode"));
        rule.setComboType((String) request.get("comboType"));
        rule.setCostCalcMethod((String) request.get("costCalcMethod"));
        if (request.get("fixedCost") != null) {
            rule.setFixedCost(new BigDecimal(request.get("fixedCost").toString()));
        }
        rule.setRemark((String) request.get("remark"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> itemsData = (List<Map<String, Object>>) request.get("items");
        List<ComboItem> items = new java.util.ArrayList<>();
        if (itemsData != null) {
            for (Map<String, Object> itemData : itemsData) {
                ComboItem item = new ComboItem();
                item.setItemSkuCode((String) itemData.get("itemSkuCode"));
                item.setItemSkuName((String) itemData.get("itemSkuName"));
                if (itemData.get("quantity") != null) {
                    item.setQuantity(new BigDecimal(itemData.get("quantity").toString()));
                }
                if (itemData.get("unitCost") != null) {
                    item.setUnitCost(new BigDecimal(itemData.get("unitCost").toString()));
                }
                item.setIsOptional((String) itemData.get("isOptional"));
                if (itemData.get("sortNo") != null) {
                    item.setSortNo(Integer.parseInt(itemData.get("sortNo").toString()));
                }
                item.setRemark((String) itemData.get("remark"));
                items.add(item);
            }
        }
        return Result.success(comboService.createRule(rule, items));
    }

    @Operation(summary = "按编码查询组合规则")
    @GetMapping("/rule/{comboCode}")
    public Result<ComboRule> getRuleByCode(@PathVariable String comboCode) {
        return Result.success(comboService.getRuleByCode(comboCode));
    }

    @Operation(summary = "查询组合明细")
    @GetMapping("/rule/{comboCode}/items")
    public Result<List<ComboItem>> getItemsByComboCode(@PathVariable String comboCode) {
        return Result.success(comboService.getItemsByComboCode(comboCode));
    }

    @Operation(summary = "查询包含某子品的组合")
    @GetMapping("/rule/item/{skuCode}")
    public Result<List<ComboRule>> getCombosByItemSku(@PathVariable String skuCode) {
        return Result.success(comboService.getCombosByItemSku(skuCode));
    }

    @Operation(summary = "分页查询组合规则")
    @GetMapping("/rule/list")
    public Result<Page<ComboRule>> pageRules(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String warehouseCode,
            @RequestParam(required = false) String comboType) {
        return Result.success(
                comboService.pageRules(new Page<>(page, size), warehouseCode, comboType));
    }

    // ============================================================

    // 组合成本与可用量
    // ============================================================

    @Operation(summary = "计算组合品成本")
    @GetMapping("/cost/{comboCode}")
    public Result<BigDecimal> calculateComboCost(@PathVariable String comboCode) {
        return Result.success(comboService.calculateComboCost(comboCode));
    }

    @Operation(summary = "计算组合品可组装数量")
    @PostMapping("/assemblable/{comboCode}")
    public Result<BigDecimal> calculateAssemblableQty(
            @PathVariable String comboCode, @RequestBody Map<String, BigDecimal> itemInventory) {
        return Result.success(comboService.calculateAssemblableQty(comboCode, itemInventory));
    }

    // ============================================================

    // 组合组装
    // ============================================================

    @Operation(summary = "创建组装单")
    @PostMapping("/assemble")
    public Result<ComboAssemble> createAssemble(
            @RequestParam String comboCode,
            @RequestParam String warehouseCode,
            @RequestParam(required = false) String ownerCode,
            @RequestParam BigDecimal assembleQty,
            @RequestParam(defaultValue = "MANUAL") String sourceType,
            @RequestParam(required = false) String sourceNo,
            @RequestParam String operator,
            @RequestParam(required = false) String remark) {
        return Result.success(
                comboService.createAssemble(
                        comboCode,
                        warehouseCode,
                        ownerCode,
                        assembleQty,
                        sourceType,
                        sourceNo,
                        operator,
                        remark));
    }

    @Operation(summary = "执行组装")
    @PostMapping("/assemble/{assembleNo}/execute")
    public Result<ComboAssemble> executeAssemble(@PathVariable String assembleNo) {
        return Result.success(comboService.executeAssemble(assembleNo));
    }

    @Operation(summary = "取消组装单")
    @PostMapping("/assemble/{assembleNo}/cancel")
    public Result<ComboAssemble> cancelAssemble(
            @PathVariable String assembleNo, @RequestParam String reason) {
        return Result.success(comboService.cancelAssemble(assembleNo, reason));
    }

    @Operation(summary = "按单号查询组装单")
    @GetMapping("/assemble/{assembleNo}")
    public Result<ComboAssemble> getAssembleByNo(@PathVariable String assembleNo) {
        return Result.success(comboService.getAssembleByNo(assembleNo));
    }

    @Operation(summary = "分页查询组装单")
    @GetMapping("/assemble/list")
    public Result<Page<ComboAssemble>> pageAssembles(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String warehouseCode,
            @RequestParam(required = false) String comboCode,
            @RequestParam(required = false) String status) {
        return Result.success(
                comboService.pageAssembles(
                        new Page<>(page, size), warehouseCode, comboCode, status));
    }

    // ============================================================

    // 组合拆解
    // ============================================================

    @Operation(summary = "创建拆解单")
    @PostMapping("/disassemble")
    public Result<ComboDisassemble> createDisassemble(
            @RequestParam String comboCode,
            @RequestParam String warehouseCode,
            @RequestParam(required = false) String ownerCode,
            @RequestParam BigDecimal disassembleQty,
            @RequestParam(defaultValue = "MANUAL") String sourceType,
            @RequestParam(required = false) String sourceNo,
            @RequestParam String operator,
            @RequestParam(required = false) String remark) {
        return Result.success(
                comboService.createDisassemble(
                        comboCode,
                        warehouseCode,
                        ownerCode,
                        disassembleQty,
                        sourceType,
                        sourceNo,
                        operator,
                        remark));
    }

    @Operation(summary = "执行拆解")
    @PostMapping("/disassemble/{disassembleNo}/execute")
    public Result<ComboDisassemble> executeDisassemble(@PathVariable String disassembleNo) {
        return Result.success(comboService.executeDisassemble(disassembleNo));
    }

    @Operation(summary = "取消拆解单")
    @PostMapping("/disassemble/{disassembleNo}/cancel")
    public Result<ComboDisassemble> cancelDisassemble(
            @PathVariable String disassembleNo, @RequestParam String reason) {
        return Result.success(comboService.cancelDisassemble(disassembleNo, reason));
    }

    @Operation(summary = "按单号查询拆解单")
    @GetMapping("/disassemble/{disassembleNo}")
    public Result<ComboDisassemble> getDisassembleByNo(@PathVariable String disassembleNo) {
        return Result.success(comboService.getDisassembleByNo(disassembleNo));
    }

    @Operation(summary = "分页查询拆解单")
    @GetMapping("/disassemble/list")
    public Result<Page<ComboDisassemble>> pageDisassembles(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String warehouseCode,
            @RequestParam(required = false) String comboCode,
            @RequestParam(required = false) String status) {
        return Result.success(
                comboService.pageDisassembles(
                        new Page<>(page, size), warehouseCode, comboCode, status));
    }
}
