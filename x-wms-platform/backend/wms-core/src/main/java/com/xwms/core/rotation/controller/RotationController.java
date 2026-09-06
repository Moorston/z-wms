package com.xwms.core.rotation.controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.common.core.Result;
import com.xwms.core.rotation.entity.*;
import com.xwms.core.rotation.service.RotationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/** 搴撳瓨鍛ㄨ浆绠＄悊 Controller */
@Tag(name = "搴撳瓨鍛ㄨ浆绠＄悊", description = "鍛ㄨ浆瑙勫垯/鍛ㄨ浆闃熷垪/鏁堟湡棰勮/鍛ㄨ浆鍒嗘瀽")
@RestController
@RequestMapping("/api/rotation")
@RequiredArgsConstructor
public class RotationController {

    private final RotationService rotationService;

    // ============================================================

    // 鍛ㄨ浆瑙勫垯
    // ============================================================

    @Operation(summary = "鍒涘缓鍛ㄨ浆瑙勫垯")
    @PostMapping("/rule")
    public Result<RotationRule> createRule(@RequestBody RotationRule rule) {
        return Result.success(rotationService.createRule(rule));
    }

    @Operation(summary = "鍒嗛〉鏌ヨ鍛ㄨ浆瑙勫垯")
    @GetMapping("/rule")
    public Result<Page<RotationRule>> pageRules(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String skuCode,
            @RequestParam(required = false) String rotationType) {
        return Result.success(
                rotationService.pageRules(new Page<>(page, size), skuCode, rotationType));
    }

    @Operation(summary = "鎸夌紪鐮佹煡璇㈠懆杞鍒")
    @GetMapping("/rule/{ruleCode}")
    public Result<RotationRule> getRuleByCode(@PathVariable String ruleCode) {
        return Result.success(rotationService.getRuleByCode(ruleCode));
    }

    @Operation(summary = "鍖归厤SKU鍛ㄨ浆瑙勫垯")
    @GetMapping("/rule/match")
    public Result<RotationRule> matchRule(
            @RequestParam String skuCode,
            @RequestParam(required = false) String categoryCode,
            @RequestParam(required = false) String ownerCode) {
        return Result.success(rotationService.matchRule(skuCode, categoryCode, ownerCode));
    }

    // ============================================================

    // 鍛ㄨ浆闃熷垪
    // ============================================================

    @Operation(summary = "娣诲姞鍒板懆杞槦鍒")
    @PostMapping("/queue/add")
    public Result<RotationQueue> addToQueue(
            @RequestParam String skuCode,
            @RequestParam String batchNo,
            @RequestParam String locationCode,
            @RequestParam(required = false) String ownerCode,
            @RequestParam BigDecimal quantity,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
                    LocalDateTime productionDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
                    LocalDateTime expireDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
                    LocalDateTime receiveDate) {
        return Result.success(
                rotationService.addToQueue(
                        skuCode,
                        batchNo,
                        locationCode,
                        ownerCode,
                        quantity,
                        productionDate,
                        expireDate,
                        receiveDate));
    }

    @Operation(summary = "浠庡懆杞槦鍒楀垎閰")
    @PostMapping("/queue/allocate")
    public Result<List<RotationQueue>> allocateFromQueue(
            @RequestParam String skuCode,
            @RequestParam String locationCode,
            @RequestParam(required = false) String ownerCode,
            @RequestParam BigDecimal needQty) {
        return Result.success(
                rotationService.allocateFromQueue(skuCode, locationCode, ownerCode, needQty));
    }

    @Operation(summary = "鎸塖KU鏌ヨ鍛ㄨ浆闃熷垪")
    @GetMapping("/queue/sku/{skuCode}")
    public Result<List<RotationQueue>> getQueueBySku(@PathVariable String skuCode) {
        return Result.success(rotationService.getQueueBySku(skuCode));
    }

    @Operation(summary = "鎸夐槦鍒桰D鏌ヨ")
    @GetMapping("/queue/{queueId}")
    public Result<List<RotationQueue>> getQueueById(@PathVariable String queueId) {
        return Result.success(rotationService.getQueueById(queueId));
    }

    // ============================================================

    // ============================================================
    // 鍛ㄨ浆鍒嗘瀽
    // ============================================================

    @Operation(summary = "璁＄畻鍛ㄨ浆鍒嗘瀽")
    @PostMapping("/analysis/calculate")
    public Result<RotationAnalysis> calculateAnalysis(
            @RequestParam String skuCode,
            @RequestParam(required = false) String warehouseCode,
            @RequestParam(required = false) String ownerCode,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
                    LocalDateTime analysisDate,
            @RequestParam(defaultValue = "DAY") String periodType,
            @RequestParam BigDecimal openingQty,
            @RequestParam BigDecimal inboundQty,
            @RequestParam BigDecimal outboundQty,
            @RequestParam BigDecimal closingQty) {
        return Result.success(
                rotationService.calculateAnalysis(
                        skuCode,
                        warehouseCode,
                        ownerCode,
                        analysisDate,
                        periodType,
                        openingQty,
                        inboundQty,
                        outboundQty,
                        closingQty));
    }

    @Operation(summary = "鎸夋棩鏈熻寖鍥存煡璇㈠懆杞垎鏋")
    @GetMapping("/analysis")
    public Result<List<RotationAnalysis>> getAnalysisByDateRange(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endDate,
            @RequestParam(defaultValue = "DAY") String periodType) {
        return Result.success(
                rotationService.getAnalysisByDateRange(startDate, endDate, periodType));
    }
}
