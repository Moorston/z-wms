package com.xwms.base.liteflow.component.rule;

import java.util.HashMap;
import java.util.Map;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

import com.xwms.base.rule.engine.RuleEngine;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** LiteFlow规则执行组件 - 业务流程中调用规则引擎 适用于上架/分配/周转/波次等业务流程中的规则计算 */
@Slf4j
@LiteflowComponent("ruleExecute")
@RequiredArgsConstructor
public class RuleExecuteComponent extends NodeComponent {

    private final RuleEngine ruleEngine;

    @Override
    public void process() {
        @SuppressWarnings("unchecked")
        Map<String, Object> context = this.getContextBean(Map.class);
        if (context == null || context.get("ruleType") == null) {
            log.info("无规则执行上下文, 跳过");
            return;
        }
        try {
            String ruleType = context.get("ruleType").toString();

            // 构建规则输入
            Map<String, Object> input = new HashMap<>();
            if (context.get("ruleInput") != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> ruleInput = (Map<String, Object>) context.get("ruleInput");
                input.putAll(ruleInput);
            } else {
                input.putAll(context);
            }

            // 执行规则
            RuleEngine.RuleResult result = ruleEngine.execute(ruleType, input);

            // 将规则输出写回上下文
            if (result.isSuccess()) {
                context.put("ruleResult", result.getOutput());
                context.put("ruleExecuted", true);
                log.info("规则执行成功: type={}, output={}", ruleType, result.getOutput());
            } else if (result.isSkip()) {
                context.put("ruleSkip", true);
                context.put("ruleMessage", result.getMessage());
                log.info("规则跳过: type={}, msg={}", ruleType, result.getMessage());
            } else {
                context.put("ruleError", result.getMessage());
                log.warn("规则执行失败: type={}, msg={}", ruleType, result.getMessage());
            }
        } catch (Exception e) {
            log.error("规则执行异常: {}", e.getMessage());
            context.put("ruleError", e.getMessage());
        }
    }

    @Override
    public boolean isAccess() {
        return true;
    }
}
