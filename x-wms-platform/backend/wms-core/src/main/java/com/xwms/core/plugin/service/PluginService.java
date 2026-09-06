package com.xwms.core.plugin.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.core.plugin.entity.*;
import com.xwms.core.plugin.enums.PluginStatus;
import com.xwms.core.plugin.mapper.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 行业插件核心服务 包含: 插件管理/插件配置/插件执行/行业规则 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PluginService {

    private final PluginMapper pluginMapper;
    private final PluginConfigMapper pluginConfigMapper;
    private final PluginLogMapper pluginLogMapper;
    private final IndustryRuleMapper industryRuleMapper;

    private static final AtomicInteger LOG_SEQ = new AtomicInteger(0);
    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    // 插件实例缓存
    private final Map<String, Object> pluginInstanceCache = new ConcurrentHashMap<>();

    // ============================================================

    // 1. 插件管理
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public Plugin installPlugin(Plugin plugin) {
        plugin.setStatus(PluginStatus.INSTALLED.getCode());
        plugin.setInstalledTime(LocalDateTime.now());
        pluginMapper.insert(plugin);
        log.info("安装插件: {}={}", plugin.getPluginCode(), plugin.getPluginName());
        return plugin;
    }

    @Transactional(rollbackFor = Exception.class)
    public Plugin enablePlugin(String pluginCode) {
        Plugin plugin = pluginMapper.selectByCode(pluginCode);
        if (plugin == null) throw new RuntimeException("插件不存在: " + pluginCode);
        if (!PluginStatus.INSTALLED.getCode().equals(plugin.getStatus())
                && !PluginStatus.DISABLED.getCode().equals(plugin.getStatus())) {
            throw new RuntimeException("插件状态不允许启用: " + plugin.getStatus());
        }
        plugin.setStatus(PluginStatus.ENABLED.getCode());
        plugin.setEnabledTime(LocalDateTime.now());
        pluginMapper.updateById(plugin);
        log.info("启用插件: {}", pluginCode);
        return plugin;
    }

    @Transactional(rollbackFor = Exception.class)
    public Plugin disablePlugin(String pluginCode) {
        Plugin plugin = pluginMapper.selectByCode(pluginCode);
        if (plugin == null) throw new RuntimeException("插件不存在: " + pluginCode);
        plugin.setStatus(PluginStatus.DISABLED.getCode());
        pluginMapper.updateById(plugin);
        log.info("禁用插件: {}", pluginCode);
        return plugin;
    }

    public Page<Plugin> pagePlugins(
            Page<Plugin> page, String pluginType, String industry, String status) {
        LambdaQueryWrapper<Plugin> wrapper = new LambdaQueryWrapper<>();
        if (pluginType != null) wrapper.eq(Plugin::getPluginType, pluginType);
        if (industry != null) wrapper.eq(Plugin::getIndustry, industry);
        if (status != null) wrapper.eq(Plugin::getStatus, status);
        wrapper.orderByAsc(Plugin::getPriority);
        return pluginMapper.selectPage(page, wrapper);
    }

    public Plugin getPluginByCode(String pluginCode) {
        return pluginMapper.selectByCode(pluginCode);
    }

    public List<Plugin> getEnabledPluginsByIndustry(String industry) {
        return pluginMapper.selectEnabledByIndustry(industry);
    }

    public List<Plugin> getAllEnabledPlugins() {
        return pluginMapper.selectAllEnabled();
    }

    // ============================================================

    // 2. 插件配置
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public PluginConfig savePluginConfig(
            String pluginCode,
            String configKey,
            String configValue,
            String configType,
            String description,
            Integer isRequired) {
        PluginConfig existing =
                pluginConfigMapper.selectOne(
                        new LambdaQueryWrapper<PluginConfig>()
                                .eq(PluginConfig::getPluginCode, pluginCode)
                                .eq(PluginConfig::getConfigKey, configKey));
        if (existing != null) {
            existing.setConfigValue(configValue);
            existing.setConfigType(configType != null ? configType : existing.getConfigType());
            existing.setDescription(description != null ? description : existing.getDescription());
            pluginConfigMapper.updateById(existing);
            return existing;
        } else {
            PluginConfig config = new PluginConfig();
            config.setPluginCode(pluginCode);
            config.setConfigKey(configKey);
            config.setConfigValue(configValue);
            config.setConfigType(configType != null ? configType : "STRING");
            config.setDescription(description);
            config.setIsRequired(isRequired != null ? isRequired : 0);
            pluginConfigMapper.insert(config);
            return config;
        }
    }

    public List<PluginConfig> getPluginConfigs(String pluginCode) {
        return pluginConfigMapper.selectByPlugin(pluginCode);
    }

    public String getPluginConfigValue(String pluginCode, String configKey) {
        PluginConfig config =
                pluginConfigMapper.selectOne(
                        new LambdaQueryWrapper<PluginConfig>()
                                .eq(PluginConfig::getPluginCode, pluginCode)
                                .eq(PluginConfig::getConfigKey, configKey));
        return config != null ? config.getConfigValue() : null;
    }

    // ============================================================

    // 3. 插件执行
    // ============================================================

    /** 执行行业插件 */
    public Object executePlugin(
            String pluginCode,
            String triggerPoint,
            String businessType,
            String businessNo,
            Object inputData,
            String traceId) {
        Plugin plugin = pluginMapper.selectByCode(pluginCode);
        if (plugin == null) {
            log.warn("插件不存在: {}", pluginCode);
            return null;
        }
        if (!PluginStatus.ENABLED.getCode().equals(plugin.getStatus())) {
            log.info("插件未启用: {}", pluginCode);
            return null;
        }

        long startTime = System.currentTimeMillis();
        String status = "SUCCESS";
        String errorMsg = null;
        Object outputData = null;

        try {
            // TODO: 通过反射或SPI加载插件实例并执行
            // Object pluginInstance = getPluginInstance(plugin);
            // outputData = pluginInstance.execute(triggerPoint, inputData);
            log.info("执行插件: {} 触发点: {}", pluginCode, triggerPoint);
        } catch (Exception e) {
            status = "FAILED";
            errorMsg = e.getMessage();
            log.error("插件执行失败: {} - {}", pluginCode, e.getMessage());
        }

        // 记录执行日志
        long costTime = System.currentTimeMillis() - startTime;
        recordPluginLog(
                pluginCode,
                plugin.getPluginName(),
                businessType,
                businessNo,
                triggerPoint,
                inputData != null ? inputData.toString() : null,
                outputData != null ? outputData.toString() : null,
                status,
                errorMsg,
                costTime,
                traceId);

        return outputData;
    }

    /** 按行业执行所有启用的插件 */
    public void executeIndustryPlugins(
            String industry,
            String triggerPoint,
            String businessType,
            String businessNo,
            Object inputData,
            String traceId) {
        List<Plugin> plugins = pluginMapper.selectEnabledByIndustry(industry);
        for (Plugin plugin : plugins) {
            try {
                executePlugin(
                        plugin.getPluginCode(),
                        triggerPoint,
                        businessType,
                        businessNo,
                        inputData,
                        traceId);
            } catch (Exception e) {
                log.error("行业插件执行异常: {} - {}", plugin.getPluginCode(), e.getMessage());
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public PluginLog recordPluginLog(
            String pluginCode,
            String pluginName,
            String businessType,
            String businessNo,
            String triggerPoint,
            String inputData,
            String outputData,
            String status,
            String errorMsg,
            Long costTime,
            String traceId) {
        PluginLog pluginLog = new PluginLog();
        pluginLog.setLogNo(generateLogNo());
        pluginLog.setPluginCode(pluginCode);
        pluginLog.setPluginName(pluginName);
        pluginLog.setBusinessType(businessType);
        pluginLog.setBusinessNo(businessNo);
        pluginLog.setTriggerPoint(triggerPoint);
        pluginLog.setInputData(inputData);
        pluginLog.setOutputData(outputData);
        pluginLog.setStatus(status);
        pluginLog.setErrorMsg(errorMsg);
        pluginLog.setCostTime(costTime);
        pluginLog.setTraceId(traceId);
        pluginLogMapper.insert(pluginLog);
        return pluginLog;
    }

    public Page<PluginLog> pagePluginLogs(
            Page<PluginLog> page,
            String pluginCode,
            String businessType,
            String businessNo,
            String status) {
        LambdaQueryWrapper<PluginLog> wrapper = new LambdaQueryWrapper<>();
        if (pluginCode != null) wrapper.eq(PluginLog::getPluginCode, pluginCode);
        if (businessType != null) wrapper.eq(PluginLog::getBusinessType, businessType);
        if (businessNo != null) wrapper.eq(PluginLog::getBusinessNo, businessNo);
        if (status != null) wrapper.eq(PluginLog::getStatus, status);
        wrapper.orderByDesc(PluginLog::getCreatedTime);
        return pluginLogMapper.selectPage(page, wrapper);
    }

    // ============================================================

    // 4. 行业规则
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public IndustryRule createIndustryRule(IndustryRule rule) {
        industryRuleMapper.insert(rule);
        log.info("创建行业规则: {}={}", rule.getRuleCode(), rule.getRuleName());
        return rule;
    }

    public Page<IndustryRule> pageIndustryRules(
            Page<IndustryRule> page,
            String industry,
            String ruleType,
            String triggerEvent,
            Integer enabled) {
        LambdaQueryWrapper<IndustryRule> wrapper = new LambdaQueryWrapper<>();
        if (industry != null) wrapper.eq(IndustryRule::getIndustry, industry);
        if (ruleType != null) wrapper.eq(IndustryRule::getRuleType, ruleType);
        if (triggerEvent != null) wrapper.eq(IndustryRule::getTriggerEvent, triggerEvent);
        if (enabled != null) wrapper.eq(IndustryRule::getEnabled, enabled);
        wrapper.orderByAsc(IndustryRule::getPriority);
        return industryRuleMapper.selectPage(page, wrapper);
    }

    public List<IndustryRule> getIndustryRulesByEvent(String industry, String triggerEvent) {
        return industryRuleMapper.selectByIndustryAndEvent(industry, triggerEvent);
    }

    /** 执行行业规则校验 */
    public boolean validateIndustryRules(String industry, String triggerEvent, Object data) {
        List<IndustryRule> rules =
                industryRuleMapper.selectByIndustryAndEvent(industry, triggerEvent);
        for (IndustryRule rule : rules) {
            try {
                // TODO: 执行规则表达式校验
                // boolean valid = RuleEngine.evaluate(rule.getRuleExpression(), data);
                // if (!valid) return false;
                log.info("执行行业规则校验: {} - {}", rule.getRuleCode(), rule.getRuleName());
            } catch (Exception e) {
                log.error("行业规则执行失败: {} - {}", rule.getRuleCode(), e.getMessage());
                return false;
            }
        }
        return true;
    }

    // ============================================================

    // 工具方法
    // ============================================================

    private String generateLogNo() {
        return "PLG"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", LOG_SEQ.incrementAndGet() % 1000);
    }
}
