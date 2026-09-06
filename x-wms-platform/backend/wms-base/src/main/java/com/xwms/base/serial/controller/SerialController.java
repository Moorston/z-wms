package com.xwms.base.serial.controller;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.base.serial.entity.*;
import com.xwms.base.serial.service.SerialService;
import com.xwms.common.core.Result;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/** 序列号管理 Controller */
@Tag(name = "序列号管理", description = "序列号规则/生成/状态流转/追踪")
@RestController
@RequestMapping("/api/serial")
@RequiredArgsConstructor
public class SerialController {

    private final SerialService serialService;

    // ============================================================
    // 序列号规则
    // ============================================================

    @Operation(summary = "创建序列号规则")
    @PostMapping("/rule")
    public Result<SerialRule> createRule(@RequestBody SerialRule rule) {
        return Result.success(serialService.createRule(rule));
    }

    @Operation(summary = "分页查询序列号规则")
    @GetMapping("/rule")
    public Result<Page<SerialRule>> pageRules(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String skuCode) {
        return Result.success(serialService.pageRules(new Page<>(page, size), skuCode));
    }

    @Operation(summary = "按编码查询序列号规则")
    @GetMapping("/rule/{ruleCode}")
    public Result<SerialRule> getRuleByCode(@PathVariable String ruleCode) {
        return Result.success(serialService.getRuleByCode(ruleCode));
    }

    // ============================================================
    // 序列号生成
    // ============================================================

    @Operation(summary = "批量生成序列号")
    @PostMapping("/generate")
    public Result<List<Serial>> generateSerials(
            @RequestParam String ruleCode,
            @RequestParam String skuCode,
            @RequestParam(required = false) String batchNo,
            @RequestParam int count,
            @RequestParam(required = false) String ownerCode,
            @RequestParam(required = false) String warehouseCode) {
        return Result.success(
                serialService.generateSerials(
                        ruleCode, skuCode, batchNo, count, ownerCode, warehouseCode));
    }

    // ============================================================
    // 序列号档案
    // ============================================================

    @Operation(summary = "分页查询序列号")
    @GetMapping
    public Result<Page<Serial>> pageSerials(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String skuCode,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String batchNo,
            @RequestParam(required = false) String locationCode,
            @RequestParam(required = false) String warehouseCode) {
        return Result.success(
                serialService.pageSerials(
                        new Page<>(page, size),
                        skuCode,
                        status,
                        batchNo,
                        locationCode,
                        warehouseCode));
    }

    @Operation(summary = "按序列号查询")
    @GetMapping("/{serialNo}")
    public Result<Serial> getSerialByNo(@PathVariable String serialNo) {
        return Result.success(serialService.getSerialByNo(serialNo));
    }

    @Operation(summary = "按批号查询序列号")
    @GetMapping("/batch/{batchNo}")
    public Result<List<Serial>> getSerialsByBatch(@PathVariable String batchNo) {
        return Result.success(serialService.getSerialsByBatch(batchNo));
    }

    @Operation(summary = "按出库单查询序列号")
    @GetMapping("/outbound/{outboundNo}")
    public Result<List<Serial>> getSerialsByOutbound(@PathVariable String outboundNo) {
        return Result.success(serialService.getSerialsByOutbound(outboundNo));
    }

    @Operation(summary = "统计SKU在库序列号数量")
    @GetMapping("/count/{skuCode}")
    public Result<Integer> countInStockBySku(@PathVariable String skuCode) {
        return Result.success(serialService.countInStockBySku(skuCode));
    }

    // ============================================================
    // 序列号状态流转
    // ============================================================

    @Operation(summary = "序列号入库确认")
    @PostMapping("/{serialNo}/inbound")
    public Result<Serial> inboundSerial(
            @PathVariable String serialNo,
            @RequestParam String inboundNo,
            @RequestParam String locationCode,
            @RequestParam(required = false) String containerNo,
            @RequestParam(required = false) String operator) {
        return Result.success(
                serialService.inboundSerial(
                        serialNo, inboundNo, locationCode, containerNo, operator));
    }

    @Operation(summary = "序列号上架")
    @PostMapping("/{serialNo}/putaway")
    public Result<Serial> putawaySerial(
            @PathVariable String serialNo,
            @RequestParam String fromLocation,
            @RequestParam String toLocation,
            @RequestParam(required = false) String operator) {
        return Result.success(
                serialService.putawaySerial(serialNo, fromLocation, toLocation, operator));
    }

    @Operation(summary = "序列号拣货")
    @PostMapping("/{serialNo}/pick")
    public Result<Serial> pickSerial(
            @PathVariable String serialNo,
            @RequestParam String outboundNo,
            @RequestParam String fromLocation,
            @RequestParam(required = false) String operator) {
        return Result.success(
                serialService.pickSerial(serialNo, outboundNo, fromLocation, operator));
    }

    @Operation(summary = "序列号打包")
    @PostMapping("/{serialNo}/pack")
    public Result<Serial> packSerial(
            @PathVariable String serialNo,
            @RequestParam(required = false) String containerNo,
            @RequestParam(required = false) String operator) {
        return Result.success(serialService.packSerial(serialNo, containerNo, operator));
    }

    @Operation(summary = "序列号发运")
    @PostMapping("/{serialNo}/ship")
    public Result<Serial> shipSerial(
            @PathVariable String serialNo, @RequestParam(required = false) String operator) {
        return Result.success(serialService.shipSerial(serialNo, operator));
    }

    @Operation(summary = "序列号退货")
    @PostMapping("/{serialNo}/return")
    public Result<Serial> returnSerial(
            @PathVariable String serialNo,
            @RequestParam String returnNo,
            @RequestParam String locationCode,
            @RequestParam(required = false) String operator) {
        return Result.success(
                serialService.returnSerial(serialNo, returnNo, locationCode, operator));
    }

    @Operation(summary = "序列号报废")
    @PostMapping("/{serialNo}/scrap")
    public Result<Serial> scrapSerial(
            @PathVariable String serialNo, @RequestParam(required = false) String operator) {
        return Result.success(serialService.scrapSerial(serialNo, operator));
    }

    @Operation(summary = "序列号移库")
    @PostMapping("/{serialNo}/move")
    public Result<Serial> moveSerial(
            @PathVariable String serialNo,
            @RequestParam String fromLocation,
            @RequestParam String toLocation,
            @RequestParam(required = false) String operator) {
        return Result.success(
                serialService.moveSerial(serialNo, fromLocation, toLocation, operator));
    }

    // ============================================================
    // 序列号绑定
    // ============================================================

    @Operation(summary = "序列号绑定")
    @PostMapping("/{serialNo}/bind")
    public Result<SerialBind> bindSerial(
            @PathVariable String serialNo,
            @RequestParam String refType,
            @RequestParam String refNo,
            @RequestParam(required = false) Integer refLineNo,
            @RequestParam(required = false) String containerNo,
            @RequestParam(required = false) String operator) {
        return Result.success(
                serialService.bindSerial(
                        serialNo, refType, refNo, refLineNo, containerNo, operator));
    }

    @Operation(summary = "序列号释放")
    @PostMapping("/{serialNo}/release")
    public Result<SerialBind> releaseSerial(
            @PathVariable String serialNo,
            @RequestParam String refType,
            @RequestParam String refNo,
            @RequestParam(required = false) String operator) {
        return Result.success(serialService.releaseSerial(serialNo, refType, refNo, operator));
    }

    @Operation(summary = "查询序列号绑定")
    @GetMapping("/{serialNo}/binds")
    public Result<List<SerialBind>> getSerialBinds(@PathVariable String serialNo) {
        return Result.success(serialService.getSerialBinds(serialNo));
    }

    @Operation(summary = "按单据查询绑定")
    @GetMapping("/bind/ref")
    public Result<List<SerialBind>> getBindsByRef(
            @RequestParam String refType, @RequestParam String refNo) {
        return Result.success(serialService.getBindsByRef(refType, refNo));
    }

    // ============================================================
    // 序列号追踪
    // ============================================================

    @Operation(summary = "查询序列号流转记录")
    @GetMapping("/{serialNo}/trace")
    public Result<List<SerialTrace>> getSerialTrace(@PathVariable String serialNo) {
        return Result.success(serialService.getSerialTrace(serialNo));
    }
}
