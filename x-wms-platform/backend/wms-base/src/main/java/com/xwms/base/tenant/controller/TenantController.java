package com.xwms.base.tenant.controller;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.base.tenant.entity.*;
import com.xwms.base.tenant.service.TenantService;
import com.xwms.common.core.Result;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/** 多租户管理 Controller */
@Tag(name = "多租户管理", description = "租户/配置/配额/套餐")
@RestController
@RequestMapping("/api/tenant")
@RequiredArgsConstructor
public class TenantController {

    private final TenantService tenantService;

    // ============================================================
    // 租户管理
    // ============================================================

    @Operation(summary = "创建租户")
    @PostMapping
    public Result<Tenant> createTenant(@RequestBody Tenant tenant) {
        return Result.success(tenantService.createTenant(tenant));
    }

    @Operation(summary = "更新租户")
    @PutMapping
    public Result<Tenant> updateTenant(@RequestBody Tenant tenant) {
        return Result.success(tenantService.updateTenant(tenant));
    }

    @Operation(summary = "分页查询租户")
    @GetMapping
    public Result<Page<Tenant>> pageTenants(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String tenantType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String industry) {
        return Result.success(
                tenantService.pageTenants(new Page<>(page, size), tenantType, status, industry));
    }

    @Operation(summary = "按编码查询租户")
    @GetMapping("/{code}")
    public Result<Tenant> getTenantByCode(@PathVariable String code) {
        return Result.success(tenantService.getTenantByCode(code));
    }

    @Operation(summary = "查询活跃租户")
    @GetMapping("/active")
    public Result<List<Tenant>> getActiveTenants() {
        return Result.success(tenantService.getActiveTenants());
    }

    @Operation(summary = "更新租户状态")
    @PutMapping("/{code}/status")
    public Result<Tenant> updateTenantStatus(
            @PathVariable String code, @RequestParam String status) {
        return Result.success(tenantService.updateTenantStatus(code, status));
    }

    @Operation(summary = "冻结租户")
    @PutMapping("/{code}/freeze")
    public Result<Tenant> freezeTenant(
            @PathVariable String code, @RequestParam(required = false) String reason) {
        return Result.success(tenantService.freezeTenant(code, reason));
    }

    @Operation(summary = "解冻租户")
    @PutMapping("/{code}/unfreeze")
    public Result<Tenant> unfreezeTenant(@PathVariable String code) {
        return Result.success(tenantService.unfreezeTenant(code));
    }

    @Operation(summary = "检查租户可用性")
    @GetMapping("/{code}/available")
    public Result<Boolean> isTenantAvailable(@PathVariable String code) {
        return Result.success(tenantService.isTenantAvailable(code));
    }

    // ============================================================
    // 租户配置
    // ============================================================

    @Operation(summary = "保存租户配置")
    @PostMapping("/{code}/config")
    public Result<TenantConfig> saveConfig(
            @PathVariable String code,
            @RequestParam String configKey,
            @RequestParam String configValue,
            @RequestParam(required = false) String configType,
            @RequestParam(required = false) String description) {
        return Result.success(
                tenantService.saveConfig(code, configKey, configValue, configType, description));
    }

    @Operation(summary = "查询租户配置")
    @GetMapping("/{code}/config")
    public Result<List<TenantConfig>> getTenantConfigs(@PathVariable String code) {
        return Result.success(tenantService.getTenantConfigs(code));
    }

    @Operation(summary = "查询配置值")
    @GetMapping("/{code}/config/{key}")
    public Result<String> getConfigValue(@PathVariable String code, @PathVariable String key) {
        return Result.success(tenantService.getConfigValue(code, key));
    }

    @Operation(summary = "删除租户配置")
    @DeleteMapping("/{code}/config/{key}")
    public Result<Void> deleteConfig(@PathVariable String code, @PathVariable String key) {
        tenantService.deleteConfig(code, key);
        return Result.success();
    }

    // ============================================================
    // 资源配额
    // ============================================================

    @Operation(summary = "设置配额")
    @PostMapping("/{code}/quota")
    public Result<TenantQuota> setQuota(
            @PathVariable String code,
            @RequestParam String resourceType,
            @RequestParam Long quotaLimit,
            @RequestParam(required = false) String quotaUnit,
            @RequestParam(required = false) Integer warningThreshold) {
        return Result.success(
                tenantService.setQuota(
                        code, resourceType, quotaLimit, quotaUnit, warningThreshold));
    }

    @Operation(summary = "查询租户配额")
    @GetMapping("/{code}/quota")
    public Result<List<TenantQuota>> getTenantQuotas(@PathVariable String code) {
        return Result.success(tenantService.getTenantQuotas(code));
    }

    @Operation(summary = "查询指定资源配额")
    @GetMapping("/{code}/quota/{type}")
    public Result<TenantQuota> getTenantQuota(
            @PathVariable String code, @PathVariable String type) {
        return Result.success(tenantService.getTenantQuota(code, type));
    }

    @Operation(summary = "增加已使用配额")
    @PutMapping("/{code}/quota/{type}/increase")
    public Result<Boolean> increaseQuotaUsed(
            @PathVariable String code, @PathVariable String type, @RequestParam long amount) {
        return Result.success(tenantService.increaseQuotaUsed(code, type, amount));
    }

    @Operation(summary = "减少已使用配额")
    @PutMapping("/{code}/quota/{type}/decrease")
    public Result<Void> decreaseQuotaUsed(
            @PathVariable String code, @PathVariable String type, @RequestParam long amount) {
        tenantService.decreaseQuotaUsed(code, type, amount);
        return Result.success();
    }

    // ============================================================
    // 套餐管理
    // ============================================================

    @Operation(summary = "创建套餐")
    @PostMapping("/package")
    public Result<TenantPackage> createPackage(@RequestBody TenantPackage pkg) {
        return Result.success(tenantService.createPackage(pkg));
    }

    @Operation(summary = "查询启用套餐")
    @GetMapping("/package/enabled")
    public Result<List<TenantPackage>> getEnabledPackages() {
        return Result.success(tenantService.getEnabledPackages());
    }

    @Operation(summary = "更新套餐")
    @PutMapping("/package")
    public Result<TenantPackage> updatePackage(@RequestBody TenantPackage pkg) {
        return Result.success(tenantService.updatePackage(pkg));
    }
}
