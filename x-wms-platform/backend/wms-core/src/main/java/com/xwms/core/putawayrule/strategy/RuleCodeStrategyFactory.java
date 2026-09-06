package com.xwms.core.putawayrule.strategy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.annotation.PostConstruct;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 规则代码策略工厂 基于Spring自动注入所有RuleCodeStrategy实现，按规则代码路由 新增规则代码只需实现RuleCodeStrategy接口并加@Component注解 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RuleCodeStrategyFactory {

    private final List<RuleCodeStrategy> strategies;

    private final Map<String, RuleCodeStrategy> strategyMap = new HashMap<>();

    @PostConstruct
    public void init() {
        for (RuleCodeStrategy strategy : strategies) {
            strategyMap.put(strategy.getRuleCode(), strategy);
            log.info("注册上架规则代码策略: {} - {}", strategy.getRuleCode(), strategy.getDescription());
        }
        log.info("上架规则代码策略工厂初始化完成，共注册 {} 种策略", strategyMap.size());
    }

    /** 根据规则代码获取策略 */
    public RuleCodeStrategy getStrategy(String ruleCode) {
        RuleCodeStrategy strategy = strategyMap.get(ruleCode);
        if (strategy == null) {
            throw new RuntimeException("未找到规则代码策略: " + ruleCode);
        }
        return strategy;
    }

    /** 是否支持该规则代码 */
    public boolean supports(String ruleCode) {
        return strategyMap.containsKey(ruleCode);
    }

    /** 获取所有已注册的规则代码 */
    public Map<String, String> getAllRuleCodes() {
        Map<String, String> result = new HashMap<>();
        strategyMap.forEach((code, strategy) -> result.put(code, strategy.getDescription()));
        return result;
    }
}
