package com.xwms.base.rule.controller;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.base.rule.engine.RuleEngine;
import com.xwms.base.rule.entity.*;
import com.xwms.base.rule.service.RuleManagementService;
import com.xwms.common.core.Result;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/** 业务规则引擎 Controller */
@Tag(name = "业务规则引擎", description = "12类业务规则管理与执行")
@RestController
@RequestMapping("/api/rule")
@RequiredArgsConstructor
public class RuleManagementController {

    private final RuleManagementService ruleManagementService;

    // ============================================================
    // 规则定义
    // ============================================================

    @Operation(summary = "创建规则")
    @PostMapping("/definition")
    public Result<RuleDefinition> createRule(@RequestBody RuleDefinition rule) {
        return Result.success(ruleManagementService.createRule(rule));
    }

    @Operation(summary = "更新规则")
    @PutMapping("/definition")
    public Result<RuleDefinition> updateRule(@RequestBody RuleDefinition rule) {
        return Result.success(ruleManagementService.updateRule(rule));
    }

    @Operation(summary = "分页查询规则")
    @GetMapping("/definition")
    public Result<Page<RuleDefinition>> pageRules(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String ruleType,
            @RequestParam(required = false) String ruleCategory,
            @RequestParam(required = false) Integer enabled) {
        return Result.success(
                ruleManagementService.pageRules(
                        new Page<>(page, size), ruleType, ruleCategory, enabled));
    }

    @Operation(summary = "按类型查询规则")
    @GetMapping("/definition/type/{ruleType}")
    public Result<List<RuleDefinition>> getRulesByType(@PathVariable String ruleType) {
        return Result.success(ruleManagementService.getRulesByType(ruleType));
    }

    @Operation(summary = "按编码查询规则")
    @GetMapping("/definition/{code}")
    public Result<RuleDefinition> getRuleByCode(@PathVariable String code) {
        return Result.success(ruleManagementService.getRuleByCode(code));
    }

    @Operation(summary = "启用/禁用规则")
    @PutMapping("/definition/{code}/toggle")
    public Result<RuleDefinition> toggleRule(
            @PathVariable String code, @RequestParam boolean enabled) {
        return Result.success(ruleManagementService.toggleRule(code, enabled));
    }

    // ============================================================
    // 规则参数
    // ============================================================

    @Operation(summary = "创建规则参数")
    @PostMapping("/param")
    public Result<RuleParam> createParam(@RequestBody RuleParam param) {
        return Result.success(ruleManagementService.createParam(param));
    }

    @Operation(summary = "更新规则参数")
    @PutMapping("/param")
    public Result<RuleParam> updateParam(@RequestBody RuleParam param) {
        return Result.success(ruleManagementService.updateParam(param));
    }

    @Operation(summary = "按规则查询参数")
    @GetMapping("/param/rule/{ruleCode}")
    public Result<List<RuleParam>> getParamsByRule(@PathVariable String ruleCode) {
        return Result.success(ruleManagementService.getParamsByRule(ruleCode));
    }

    // ============================================================
    // 规则执行
    // ============================================================

    @Operation(summary = "执行规则")
    @PostMapping("/execute/{ruleType}")
    public Result<RuleEngine.RuleResult> executeRule(
            @PathVariable String ruleType, @RequestBody Map<String, Object> input) {
        return Result.success(ruleManagementService.executeRule(ruleType, input));
    }

    @Operation(summary = "刷新规则缓存")
    @PostMapping("/cache/refresh")
    public Result<Void> refreshCache(@RequestParam(required = false) String ruleType) {
        ruleManagementService.refreshCache(ruleType);
        return Result.success();
    }

    // ============================================================
    // 执行日志
    // ============================================================

    @Operation(summary = "分页查询执行日志")
    @GetMapping("/exec-log")
    public Result<Page<RuleExecLog>> pageExecLogs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String ruleCode,
            @RequestParam(required = false) String ruleType,
            @RequestParam(required = false) String bizType,
            @RequestParam(required = false) String bizNo) {
        return Result.success(
                ruleManagementService.pageExecLogs(
                        new Page<>(page, size), ruleCode, ruleType, bizType, bizNo));
    }

    @Operation(summary = "按规则查询执行日志")
    @GetMapping("/exec-log/rule/{ruleCode}")
    public Result<List<RuleExecLog>> getExecLogsByRule(@PathVariable String ruleCode) {
        return Result.success(ruleManagementService.getExecLogsByRule(ruleCode));
    }

    @Operation(summary = "按业务查询执行日志")
    @GetMapping("/exec-log/biz")
    public Result<List<RuleExecLog>> getExecLogsByBiz(
            @RequestParam String bizType, @RequestParam String bizNo) {
        return Result.success(ruleManagementService.getExecLogsByBiz(bizType, bizNo));
    }

    // ============================================================
    // 规则版本
    // ============================================================

    @Operation(summary = "按规则查询版本")
    @GetMapping("/version/{ruleCode}")
    public Result<List<RuleVersion>> getVersionsByRule(@PathVariable String ruleCode) {
        return Result.success(ruleManagementService.getVersionsByRule(ruleCode));
    }
}
