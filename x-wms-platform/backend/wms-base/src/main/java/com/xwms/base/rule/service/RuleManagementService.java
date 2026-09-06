package com.xwms.base.rule.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.base.rule.engine.RuleEngine;
import com.xwms.base.rule.entity.*;
import com.xwms.base.rule.mapper.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 业务规则管理服务 包含: 规则定义/规则参数/规则执行日志/规则版本 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RuleManagementService {

    private final RuleDefinitionMapper ruleDefinitionMapper;
    private final RuleParamMapper ruleParamMapper;
    private final RuleExecLogMapper ruleExecLogMapper;
    private final RuleVersionMapper ruleVersionMapper;
    private final RuleEngine ruleEngine;

    // ============================================================
    // 1. 规则定义管理
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public RuleDefinition createRule(RuleDefinition rule) {
        if (rule.getEnabled() == null) rule.setEnabled(1);
        if (rule.getPriority() == null) rule.setPriority(100);
        if (rule.getVersion() == null) rule.setVersion(1);
        ruleDefinitionMapper.insert(rule);
        // 保存版本
        saveRuleVersion(rule, "创建规则");
        // 刷新缓存
        ruleEngine.refreshCache(rule.getRuleType());
        log.info("创建规则: {}={}", rule.getRuleCode(), rule.getRuleName());
        return rule;
    }

    @Transactional(rollbackFor = Exception.class)
    public RuleDefinition updateRule(RuleDefinition rule) {
        RuleDefinition existing = ruleDefinitionMapper.selectLatestByCode(rule.getRuleCode());
        if (existing != null) {
            // 版本号+1
            rule.setVersion(existing.getVersion() + 1);
            // 保存旧版本
            saveRuleVersion(existing, "更新规则");
        }
        ruleDefinitionMapper.updateById(rule);
        // 刷新缓存
        ruleEngine.refreshCache(rule.getRuleType());
        log.info("更新规则: {} v{}", rule.getRuleCode(), rule.getVersion());
        return rule;
    }

    public Page<RuleDefinition> pageRules(
            Page<RuleDefinition> page, String ruleType, String ruleCategory, Integer enabled) {
        LambdaQueryWrapper<RuleDefinition> wrapper = new LambdaQueryWrapper<>();
        if (ruleType != null) wrapper.eq(RuleDefinition::getRuleType, ruleType);
        if (ruleCategory != null) wrapper.eq(RuleDefinition::getRuleCategory, ruleCategory);
        if (enabled != null) wrapper.eq(RuleDefinition::getEnabled, enabled);
        wrapper.orderByAsc(RuleDefinition::getRuleType).orderByAsc(RuleDefinition::getPriority);
        return ruleDefinitionMapper.selectPage(page, wrapper);
    }

    public List<RuleDefinition> getRulesByType(String ruleType) {
        return ruleEngine.getRulesByType(ruleType);
    }

    public RuleDefinition getRuleByCode(String ruleCode) {
        return ruleDefinitionMapper.selectLatestByCode(ruleCode);
    }

    /** 启用/禁用规则 */
    @Transactional(rollbackFor = Exception.class)
    public RuleDefinition toggleRule(String ruleCode, boolean enabled) {
        RuleDefinition rule = ruleDefinitionMapper.selectLatestByCode(ruleCode);
        if (rule == null) throw new RuntimeException("规则不存在: " + ruleCode);
        rule.setEnabled(enabled ? 1 : 0);
        ruleDefinitionMapper.updateById(rule);
        ruleEngine.refreshCache(rule.getRuleType());
        log.info("规则{}: {}", enabled ? "启用" : "禁用", ruleCode);
        return rule;
    }

    // ============================================================
    // 2. 规则参数管理
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public RuleParam createParam(RuleParam param) {
        ruleParamMapper.insert(param);
        log.info("创建规则参数: {}={}", param.getRuleCode(), param.getParamCode());
        return param;
    }

    @Transactional(rollbackFor = Exception.class)
    public RuleParam updateParam(RuleParam param) {
        ruleParamMapper.updateById(param);
        return param;
    }

    public List<RuleParam> getParamsByRule(String ruleCode) {
        return ruleEngine.getRuleParams(ruleCode);
    }

    // ============================================================
    // 3. 规则执行
    // ============================================================

    /** 执行规则 */
    public RuleEngine.RuleResult executeRule(String ruleType, Map<String, Object> input) {
        return ruleEngine.execute(ruleType, input);
    }

    /** 刷新规则缓存 */
    public void refreshCache(String ruleType) {
        ruleEngine.refreshCache(ruleType);
    }

    // ============================================================
    // 4. 规则执行日志
    // ============================================================

    public Page<RuleExecLog> pageExecLogs(
            Page<RuleExecLog> page,
            String ruleCode,
            String ruleType,
            String bizType,
            String bizNo) {
        LambdaQueryWrapper<RuleExecLog> wrapper = new LambdaQueryWrapper<>();
        if (ruleCode != null) wrapper.eq(RuleExecLog::getRuleCode, ruleCode);
        if (ruleType != null) wrapper.eq(RuleExecLog::getRuleType, ruleType);
        if (bizType != null) wrapper.eq(RuleExecLog::getBizType, bizType);
        if (bizNo != null) wrapper.eq(RuleExecLog::getBizNo, bizNo);
        wrapper.orderByDesc(RuleExecLog::getExecTime);
        return ruleExecLogMapper.selectPage(page, wrapper);
    }

    public List<RuleExecLog> getExecLogsByRule(String ruleCode) {
        return ruleExecLogMapper.selectByRuleCode(ruleCode);
    }

    public List<RuleExecLog> getExecLogsByBiz(String bizType, String bizNo) {
        return ruleExecLogMapper.selectByBiz(bizType, bizNo);
    }

    // ============================================================
    // 5. 规则版本
    // ============================================================

    public List<RuleVersion> getVersionsByRule(String ruleCode) {
        return ruleVersionMapper.selectByRuleCode(ruleCode);
    }

    private void saveRuleVersion(RuleDefinition rule, String changeDesc) {
        RuleVersion version = new RuleVersion();
        version.setRuleCode(rule.getRuleCode());
        version.setVersion(rule.getVersion());
        version.setRuleScript(rule.getRuleScript());
        version.setRuleConfig(rule.getRuleConfig());
        version.setChangeDesc(changeDesc);
        version.setIsCurrent(0);
        ruleVersionMapper.insert(version);
    }
}
