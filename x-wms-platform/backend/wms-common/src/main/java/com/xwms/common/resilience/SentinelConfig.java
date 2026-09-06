package com.xwms.common.resilience;

import java.util.List;

import jakarta.annotation.PostConstruct;

import org.springframework.context.annotation.Configuration;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;

import lombok.extern.slf4j.Slf4j;

/**
 * Sentinel 统一配置
 *
 * <p>核心能力：
 *
 * <ol>
 *   <li>熔断降级规则注册（DegradeRule）
 *   <li>限流规则注册（FlowRule）
 *   <li>热点参数限流（ParamFlowRule，可选）
 * </ol>
 *
 * <p>使用方式（注解式）：
 *
 * <pre>
 * &#64;SentinelResource(value = "inventoryService", fallback = "inventoryFallback")
 * public Result&lt;Inventory&gt; getInventory(String sku) { ... }
 *
 * private Result&lt;Inventory&gt; inventoryFallback(String sku, BlockException e) {
 *     return FallbackHandler.handle("库存查询", e, () -> Result.fail("库存查询服务暂不可用"));
 * }
 * </pre>
 *
 * <p>配置在 application.yaml 中：
 *
 * <pre>
 * spring:
 *   cloud:
 *     sentinel:
 *       transport:
 *         dashboard: ${SENTINEL_DASHBOARD:localhost:8858}
 *         port: ${SENTINEL_PORT:8719}
 *       eager: true
 * </pre>
 *
 * <p>规则优先级：Dashboard 动态推送 > 本地代码注册。 本地注册的规则作为兜底，确保 Dashboard 未连接时仍有保护。
 */
@Slf4j
@Configuration
public class SentinelConfig {

    @PostConstruct
    public void init() {
        initFlowRules();
        initDegradeRules();
        log.info(
                "[Sentinel] 统一配置初始化完成: 限流规则={}, 熔断规则={}",
                FlowRuleManager.getRules().size(),
                DegradeRuleManager.getRules().size());
    }

    /** 初始化限流规则（兜底，Dashboard 可动态覆盖） */
    private void initFlowRules() {
        // 通用限流：每秒 100 QPS
        FlowRule commonRule = new FlowRule();
        commonRule.setResource("wms-common");
        commonRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        commonRule.setCount(100);
        commonRule.setLimitApp("default");

        // 库存服务限流
        FlowRule inventoryRule = new FlowRule();
        inventoryRule.setResource("inventoryService");
        inventoryRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        inventoryRule.setCount(200);
        inventoryRule.setLimitApp("default");

        // 出库服务限流
        FlowRule outboundRule = new FlowRule();
        outboundRule.setResource("outboundService");
        outboundRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        outboundRule.setCount(100);
        outboundRule.setLimitApp("default");

        // 快递API限流（外部接口最严格）
        FlowRule expressApiRule = new FlowRule();
        expressApiRule.setResource("expressApi");
        expressApiRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        expressApiRule.setCount(10);
        expressApiRule.setLimitApp("default");

        // API网关限流
        FlowRule apiGatewayRule = new FlowRule();
        apiGatewayRule.setResource("apiGateway");
        apiGatewayRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        apiGatewayRule.setCount(500);
        apiGatewayRule.setLimitApp("default");

        // 外部适配器限流
        FlowRule adapterServiceRule = new FlowRule();
        adapterServiceRule.setResource("adapterService");
        adapterServiceRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        adapterServiceRule.setCount(50);
        adapterServiceRule.setLimitApp("default");

        FlowRuleManager.loadRules(
                List.of(
                        commonRule,
                        inventoryRule,
                        outboundRule,
                        expressApiRule,
                        apiGatewayRule,
                        adapterServiceRule));
    }

    /** 初始化熔断降级规则（兜底，Dashboard 可动态覆盖） */
    private void initDegradeRules() {
        // 慢调用比例熔断：RT > 500ms 占比超 50% 时熔断 30s
        DegradeRule slowCallRule = new DegradeRule();
        slowCallRule.setResource("wms-common");
        slowCallRule.setGrade(RuleConstant.DEGRADE_GRADE_RT);
        slowCallRule.setCount(500);
        slowCallRule.setSlowRatioThreshold(0.5);
        slowCallRule.setMinRequestAmount(10);
        slowCallRule.setStatIntervalMs(10000);
        slowCallRule.setTimeWindow(30);

        // 异常比例熔断：异常率 > 50% 时熔断 30s
        DegradeRule exceptionRatioRule = new DegradeRule();
        exceptionRatioRule.setResource("wms-common");
        exceptionRatioRule.setGrade(RuleConstant.DEGRADE_GRADE_EXCEPTION_RATIO);
        exceptionRatioRule.setCount(0.5);
        exceptionRatioRule.setMinRequestAmount(10);
        exceptionRatioRule.setStatIntervalMs(10000);
        exceptionRatioRule.setTimeWindow(30);

        // 快递API 熔断（外部接口更严格）
        DegradeRule expressApiRule = new DegradeRule();
        expressApiRule.setResource("expressApi");
        expressApiRule.setGrade(RuleConstant.DEGRADE_GRADE_EXCEPTION_RATIO);
        expressApiRule.setCount(0.3);
        expressApiRule.setMinRequestAmount(5);
        expressApiRule.setStatIntervalMs(10000);
        expressApiRule.setTimeWindow(60);

        DegradeRuleManager.loadRules(List.of(slowCallRule, exceptionRatioRule, expressApiRule));
    }
}
