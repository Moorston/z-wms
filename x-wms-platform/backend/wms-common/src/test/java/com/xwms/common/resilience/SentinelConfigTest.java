package com.xwms.common.resilience;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SentinelConfig 回归测试
 *
 * <p>覆盖 R2: Resilience4j→Sentinel 升级。验证 {@code @PostConstruct init()}
 * 将限流规则注册到 {@link FlowRuleManager}、熔断规则注册到 {@link DegradeRuleManager}。
 */
class SentinelConfigTest {

    private SentinelConfig config;

    @BeforeEach
    void setUp() {
        config = new SentinelConfig();
    }

    @AfterEach
    void tearDown() {
        FlowRuleManager.loadRules(Collections.emptyList());
        DegradeRuleManager.loadRules(Collections.emptyList());
    }

    // ============================================================
    // T1.1: 6 条限流规则资源名与阈值正确
    // ============================================================

    @Test
    void init_registers6FlowRules_withCorrectResourcesAndThresholds() {
        config.init();

        List<FlowRule> rules = FlowRuleManager.getRules();
        assertEquals(6, rules.size(), "应注册 6 条限流规则");

        Map<String, Double> resourceThresholds =
                rules.stream()
                        .collect(
                                Collectors.toMap(
                                        FlowRule::getResource, FlowRule::getCount));

        assertEquals(100.0, resourceThresholds.get("wms-common"));
        assertEquals(200.0, resourceThresholds.get("inventoryService"));
        assertEquals(100.0, resourceThresholds.get("outboundService"));
        assertEquals(10.0, resourceThresholds.get("expressApi"));
        assertEquals(500.0, resourceThresholds.get("apiGateway"));
        assertEquals(50.0, resourceThresholds.get("adapterService"));
    }

    // ============================================================
    // T1.2: 3 条熔断规则配置正确
    // ============================================================

    @Test
    void init_registers3DegradeRules_withCorrectConfig() {
        config.init();

        List<DegradeRule> rules = DegradeRuleManager.getRules();
        assertEquals(3, rules.size(), "应注册 3 条熔断规则");

        for (DegradeRule rule : rules) {
            if (RuleConstant.DEGRADE_GRADE_RT == rule.getGrade()) {
                // slowCallRule: RT > 500ms, 熔断 30s
                assertEquals(500, rule.getCount());
                assertEquals(30, rule.getTimeWindow());
            } else if ("wms-common".equals(rule.getResource())
                    && RuleConstant.DEGRADE_GRADE_EXCEPTION_RATIO == rule.getGrade()) {
                // exceptionRatioRule: 异常率 > 50%, 熔断 30s
                assertEquals(0.5, rule.getCount());
                assertEquals(30, rule.getTimeWindow());
            } else if ("expressApi".equals(rule.getResource())
                    && RuleConstant.DEGRADE_GRADE_EXCEPTION_RATIO == rule.getGrade()) {
                // expressApiRule: 异常率 > 30%, 熔断 60s
                assertEquals(0.3, rule.getCount());
                assertEquals(60, rule.getTimeWindow());
            }
        }
    }

    // ============================================================
    // T1.3: 所有限流规则使用 QPS 级别
    // ============================================================

    @Test
    void init_flowRulesUseQPSGrade() {
        config.init();

        List<FlowRule> rules = FlowRuleManager.getRules();
        assertFalse(rules.isEmpty(), "限流规则列表不应为空");

        rules.forEach(
                rule ->
                        assertEquals(
                                RuleConstant.FLOW_GRADE_QPS,
                                rule.getGrade(),
                                "所有限流规则应使用 QPS 级别"));
    }

    // ============================================================
    // T1.4: init() 幂等性（loadRules 替换语义，规则数不翻倍）
    // ============================================================

    @Test
    void init_isIdempotent_onSecondCall() {
        config.init();
        int flowCountAfterFirst = FlowRuleManager.getRules().size();
        int degradeCountAfterFirst = DegradeRuleManager.getRules().size();

        config.init();
        int flowCountAfterSecond = FlowRuleManager.getRules().size();
        int degradeCountAfterSecond = DegradeRuleManager.getRules().size();

        assertEquals(
                flowCountAfterFirst, flowCountAfterSecond, "二次调用后限流规则数不应变化");
        assertEquals(
                degradeCountAfterFirst,
                degradeCountAfterSecond,
                "二次调用后熔断规则数不应变化");
    }
}
