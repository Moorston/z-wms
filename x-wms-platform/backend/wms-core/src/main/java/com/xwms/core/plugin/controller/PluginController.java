package com.xwms.core.plugin.controller;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.common.core.Result;
import com.xwms.core.plugin.entity.*;
import com.xwms.core.plugin.service.PluginService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/** 行业插件 Controller */
@Tag(name = "行业插件", description = "插件管理/配置/执行/行业规则")
@RestController
@RequestMapping("/api/plugin")
@RequiredArgsConstructor
public class PluginController {

    private final PluginService pluginService;

    // ============================================================

    // 插件管理
    // ============================================================

    @Operation(summary = "安装插件")
    @PostMapping("/install")
    public Result<Plugin> installPlugin(@RequestBody Plugin plugin) {
        return Result.success(pluginService.installPlugin(plugin));
    }

    @Operation(summary = "启用插件")
    @PutMapping("/{code}/enable")
    public Result<Plugin> enablePlugin(@PathVariable String code) {
        return Result.success(pluginService.enablePlugin(code));
    }

    @Operation(summary = "禁用插件")
    @PutMapping("/{code}/disable")
    public Result<Plugin> disablePlugin(@PathVariable String code) {
        return Result.success(pluginService.disablePlugin(code));
    }

    @Operation(summary = "分页查询插件")
    @GetMapping
    public Result<Page<Plugin>> pagePlugins(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String pluginType,
            @RequestParam(required = false) String industry,
            @RequestParam(required = false) String status) {
        return Result.success(
                pluginService.pagePlugins(new Page<>(page, size), pluginType, industry, status));
    }

    @Operation(summary = "按编码查询插件")
    @GetMapping("/{code}")
    public Result<Plugin> getPluginByCode(@PathVariable String code) {
        return Result.success(pluginService.getPluginByCode(code));
    }

    @Operation(summary = "按行业查询启用插件")
    @GetMapping("/industry/{industry}/enabled")
    public Result<List<Plugin>> getEnabledPluginsByIndustry(@PathVariable String industry) {
        return Result.success(pluginService.getEnabledPluginsByIndustry(industry));
    }

    @Operation(summary = "查询所有启用插件")
    @GetMapping("/enabled")
    public Result<List<Plugin>> getAllEnabledPlugins() {
        return Result.success(pluginService.getAllEnabledPlugins());
    }

    // ============================================================

    // 插件配置
    // ============================================================

    @Operation(summary = "保存插件配置")
    @PostMapping("/{code}/config")
    public Result<PluginConfig> savePluginConfig(
            @PathVariable String code,
            @RequestParam String configKey,
            @RequestParam String configValue,
            @RequestParam(required = false) String configType,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) Integer isRequired) {
        return Result.success(
                pluginService.savePluginConfig(
                        code, configKey, configValue, configType, description, isRequired));
    }

    @Operation(summary = "查询插件配置")
    @GetMapping("/{code}/config")
    public Result<List<PluginConfig>> getPluginConfigs(@PathVariable String code) {
        return Result.success(pluginService.getPluginConfigs(code));
    }

    @Operation(summary = "查询插件配置值")
    @GetMapping("/{code}/config/{key}")
    public Result<String> getPluginConfigValue(
            @PathVariable String code, @PathVariable String key) {
        return Result.success(pluginService.getPluginConfigValue(code, key));
    }

    // ============================================================

    // 插件执行
    // ============================================================

    @Operation(summary = "执行插件")
    @PostMapping("/{code}/execute")
    public Result<Object> executePlugin(
            @PathVariable String code,
            @RequestParam String triggerPoint,
            @RequestParam(required = false) String businessType,
            @RequestParam(required = false) String businessNo,
            @RequestBody(required = false) Object inputData,
            @RequestParam(required = false) String traceId) {
        return Result.success(
                pluginService.executePlugin(
                        code, triggerPoint, businessType, businessNo, inputData, traceId));
    }

    @Operation(summary = "执行行业插件")
    @PostMapping("/industry/{industry}/execute")
    public Result<Void> executeIndustryPlugins(
            @PathVariable String industry,
            @RequestParam String triggerPoint,
            @RequestParam(required = false) String businessType,
            @RequestParam(required = false) String businessNo,
            @RequestBody(required = false) Object inputData,
            @RequestParam(required = false) String traceId) {
        pluginService.executeIndustryPlugins(
                industry, triggerPoint, businessType, businessNo, inputData, traceId);
        return Result.success();
    }

    @Operation(summary = "分页查询插件日志")
    @GetMapping("/log")
    public Result<Page<PluginLog>> pagePluginLogs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String pluginCode,
            @RequestParam(required = false) String businessType,
            @RequestParam(required = false) String businessNo,
            @RequestParam(required = false) String status) {
        return Result.success(
                pluginService.pagePluginLogs(
                        new Page<>(page, size), pluginCode, businessType, businessNo, status));
    }

    // ============================================================

    // 行业规则
    // ============================================================

    @Operation(summary = "创建行业规则")
    @PostMapping("/rule")
    public Result<IndustryRule> createIndustryRule(@RequestBody IndustryRule rule) {
        return Result.success(pluginService.createIndustryRule(rule));
    }

    @Operation(summary = "分页查询行业规则")
    @GetMapping("/rule")
    public Result<Page<IndustryRule>> pageIndustryRules(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String industry,
            @RequestParam(required = false) String ruleType,
            @RequestParam(required = false) String triggerEvent,
            @RequestParam(required = false) Integer enabled) {
        return Result.success(
                pluginService.pageIndustryRules(
                        new Page<>(page, size), industry, ruleType, triggerEvent, enabled));
    }

    @Operation(summary = "按事件查询行业规则")
    @GetMapping("/rule/industry/{industry}/event/{event}")
    public Result<List<IndustryRule>> getIndustryRulesByEvent(
            @PathVariable String industry, @PathVariable String event) {
        return Result.success(pluginService.getIndustryRulesByEvent(industry, event));
    }

    @Operation(summary = "执行行业规则校验")
    @PostMapping("/rule/validate")
    public Result<Boolean> validateIndustryRules(
            @RequestParam String industry,
            @RequestParam String triggerEvent,
            @RequestBody(required = false) Object data) {
        return Result.success(pluginService.validateIndustryRules(industry, triggerEvent, data));
    }
}
