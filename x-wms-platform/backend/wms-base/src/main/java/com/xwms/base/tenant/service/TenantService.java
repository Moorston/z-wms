package com.xwms.base.tenant.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.base.tenant.entity.*;
import com.xwms.base.tenant.enums.TenantStatus;
import com.xwms.base.tenant.mapper.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 多租户管理核心服务 包含: 租户管理/租户配置/资源配额/套餐管理 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TenantService {

    private final TenantMapper tenantMapper;
    private final TenantConfigMapper tenantConfigMapper;
    private final TenantQuotaMapper tenantQuotaMapper;
    private final TenantPackageMapper tenantPackageMapper;

    // ============================================================
    // 1. 租户管理
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public Tenant createTenant(Tenant tenant) {
        if (tenant.getStatus() == null) {
            tenant.setStatus(TenantStatus.ACTIVE.getCode());
        }
        tenantMapper.insert(tenant);
        log.info("创建租户: {}={}", tenant.getTenantCode(), tenant.getTenantName());

        // 初始化默认配置
        initDefaultConfig(tenant.getTenantCode());
        // 初始化默认配额
        initDefaultQuota(tenant.getTenantCode());
        return tenant;
    }

    @Transactional(rollbackFor = Exception.class)
    public Tenant updateTenant(Tenant tenant) {
        tenantMapper.updateById(tenant);
        return tenant;
    }

    public Page<Tenant> pageTenants(
            Page<Tenant> page, String tenantType, String status, String industry) {
        LambdaQueryWrapper<Tenant> wrapper = new LambdaQueryWrapper<>();
        if (tenantType != null) wrapper.eq(Tenant::getTenantType, tenantType);
        if (status != null) wrapper.eq(Tenant::getStatus, status);
        if (industry != null) wrapper.eq(Tenant::getIndustry, industry);
        wrapper.orderByAsc(Tenant::getTenantCode);
        return tenantMapper.selectPage(page, wrapper);
    }

    public Tenant getTenantByCode(String tenantCode) {
        return tenantMapper.selectByCode(tenantCode);
    }

    public List<Tenant> getActiveTenants() {
        return tenantMapper.selectActiveTenants();
    }

    @Transactional(rollbackFor = Exception.class)
    public Tenant updateTenantStatus(String tenantCode, String status) {
        Tenant tenant = tenantMapper.selectByCode(tenantCode);
        if (tenant == null) throw new RuntimeException("租户不存在: " + tenantCode);
        tenant.setStatus(status);
        tenantMapper.updateById(tenant);
        log.info("更新租户状态: {}={}", tenantCode, status);
        return tenant;
    }

    @Transactional(rollbackFor = Exception.class)
    public Tenant freezeTenant(String tenantCode, String reason) {
        return updateTenantStatus(tenantCode, TenantStatus.FROZEN.getCode());
    }

    @Transactional(rollbackFor = Exception.class)
    public Tenant unfreezeTenant(String tenantCode) {
        return updateTenantStatus(tenantCode, TenantStatus.ACTIVE.getCode());
    }

    /** 检查租户是否可用 */
    public boolean isTenantAvailable(String tenantCode) {
        Tenant tenant = tenantMapper.selectByCode(tenantCode);
        if (tenant == null) return false;
        if (!TenantStatus.ACTIVE.getCode().equals(tenant.getStatus())) return false;
        if (tenant.getExpireDate() != null && tenant.getExpireDate().isBefore(LocalDate.now())) {
            return false;
        }
        return true;
    }

    // ============================================================
    // 2. 租户配置
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public TenantConfig saveConfig(
            String tenantCode,
            String configKey,
            String configValue,
            String configType,
            String description) {
        TenantConfig existing = tenantConfigMapper.selectByTenantAndKey(tenantCode, configKey);
        if (existing != null) {
            existing.setConfigValue(configValue);
            existing.setConfigType(configType != null ? configType : existing.getConfigType());
            existing.setDescription(description != null ? description : existing.getDescription());
            tenantConfigMapper.updateById(existing);
            return existing;
        } else {
            TenantConfig config = new TenantConfig();
            config.setTenantCode(tenantCode);
            config.setConfigKey(configKey);
            config.setConfigValue(configValue);
            config.setConfigType(configType != null ? configType : "STRING");
            config.setDescription(description);
            config.setIsSystem(0);
            tenantConfigMapper.insert(config);
            return config;
        }
    }

    public List<TenantConfig> getTenantConfigs(String tenantCode) {
        return tenantConfigMapper.selectByTenant(tenantCode);
    }

    public String getConfigValue(String tenantCode, String configKey) {
        TenantConfig config = tenantConfigMapper.selectByTenantAndKey(tenantCode, configKey);
        return config != null ? config.getConfigValue() : null;
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteConfig(String tenantCode, String configKey) {
        TenantConfig config = tenantConfigMapper.selectByTenantAndKey(tenantCode, configKey);
        if (config != null && config.getIsSystem() != null && config.getIsSystem() == 1) {
            throw new RuntimeException("系统配置不可删除");
        }
        if (config != null) {
            tenantConfigMapper.deleteById(config.getId());
        }
    }

    private void initDefaultConfig(String tenantCode) {
        saveConfig(tenantCode, "default_warehouse", "", "STRING", "默认仓库");
        saveConfig(tenantCode, "auto_allocate", "true", "BOOLEAN", "自动分配库存");
        saveConfig(tenantCode, "batch_track", "false", "BOOLEAN", "批次追踪");
        saveConfig(tenantCode, "qc_required", "false", "BOOLEAN", "入库必检");
        saveConfig(tenantCode, "language", "zh_CN", "STRING", "默认语言");
        saveConfig(tenantCode, "timezone", "Asia/Shanghai", "STRING", "时区");
    }

    // ============================================================
    // 3. 资源配额
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public TenantQuota setQuota(
            String tenantCode,
            String resourceType,
            Long quotaLimit,
            String quotaUnit,
            Integer warningThreshold) {
        TenantQuota existing = tenantQuotaMapper.selectByTenantAndType(tenantCode, resourceType);
        if (existing != null) {
            existing.setQuotaLimit(quotaLimit);
            existing.setQuotaUnit(quotaUnit != null ? quotaUnit : existing.getQuotaUnit());
            existing.setWarningThreshold(
                    warningThreshold != null ? warningThreshold : existing.getWarningThreshold());
            updateQuotaStatus(existing);
            tenantQuotaMapper.updateById(existing);
            return existing;
        } else {
            TenantQuota quota = new TenantQuota();
            quota.setTenantCode(tenantCode);
            quota.setResourceType(resourceType);
            quota.setQuotaLimit(quotaLimit);
            quota.setQuotaUsed(0L);
            quota.setQuotaUnit(quotaUnit != null ? quotaUnit : "个");
            quota.setWarningThreshold(warningThreshold != null ? warningThreshold : 80);
            quota.setStatus("NORMAL");
            tenantQuotaMapper.insert(quota);
            return quota;
        }
    }

    public List<TenantQuota> getTenantQuotas(String tenantCode) {
        return tenantQuotaMapper.selectByTenant(tenantCode);
    }

    public TenantQuota getTenantQuota(String tenantCode, String resourceType) {
        return tenantQuotaMapper.selectByTenantAndType(tenantCode, resourceType);
    }

    /** 增加已使用配额 */
    @Transactional(rollbackFor = Exception.class)
    public boolean increaseQuotaUsed(String tenantCode, String resourceType, long amount) {
        TenantQuota quota = tenantQuotaMapper.selectByTenantAndType(tenantCode, resourceType);
        if (quota == null) return true; // 无配额限制
        if (quota.getQuotaLimit() != null
                && quota.getQuotaLimit() > 0
                && quota.getQuotaUsed() + amount > quota.getQuotaLimit()) {
            log.warn(
                    "租户配额超限: {} {} 已用={} 限额={}",
                    tenantCode,
                    resourceType,
                    quota.getQuotaUsed(),
                    quota.getQuotaLimit());
            return false;
        }
        quota.setQuotaUsed(quota.getQuotaUsed() + amount);
        updateQuotaStatus(quota);
        tenantQuotaMapper.updateById(quota);
        return true;
    }

    /** 减少已使用配额 */
    @Transactional(rollbackFor = Exception.class)
    public void decreaseQuotaUsed(String tenantCode, String resourceType, long amount) {
        TenantQuota quota = tenantQuotaMapper.selectByTenantAndType(tenantCode, resourceType);
        if (quota == null) return;
        quota.setQuotaUsed(Math.max(0, quota.getQuotaUsed() - amount));
        updateQuotaStatus(quota);
        tenantQuotaMapper.updateById(quota);
    }

    private void updateQuotaStatus(TenantQuota quota) {
        if (quota.getQuotaLimit() == null || quota.getQuotaLimit() == 0) {
            quota.setStatus("NORMAL");
            return;
        }
        double usageRate = (double) quota.getQuotaUsed() / quota.getQuotaLimit() * 100;
        if (usageRate >= 100) {
            quota.setStatus("OVER_LIMIT");
        } else if (usageRate >= quota.getWarningThreshold()) {
            quota.setStatus("WARNING");
        } else {
            quota.setStatus("NORMAL");
        }
    }

    private void initDefaultQuota(String tenantCode) {
        setQuota(tenantCode, "USER", 100L, "个", 80);
        setQuota(tenantCode, "WAREHOUSE", 5L, "个", 80);
        setQuota(tenantCode, "LOCATION", 10000L, "个", 80);
        setQuota(tenantCode, "SKU", 5000L, "个", 80);
        setQuota(tenantCode, "ORDER", 0L, "单/月", 80); // 0表示不限
        setQuota(tenantCode, "STORAGE", 100L, "GB", 80);
    }

    // ============================================================
    // 4. 套餐管理
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public TenantPackage createPackage(TenantPackage pkg) {
        tenantPackageMapper.insert(pkg);
        log.info("创建套餐: {}={}", pkg.getPackageCode(), pkg.getPackageName());
        return pkg;
    }

    public List<TenantPackage> getEnabledPackages() {
        return tenantPackageMapper.selectEnabledPackages();
    }

    @Transactional(rollbackFor = Exception.class)
    public TenantPackage updatePackage(TenantPackage pkg) {
        tenantPackageMapper.updateById(pkg);
        return pkg;
    }
}
