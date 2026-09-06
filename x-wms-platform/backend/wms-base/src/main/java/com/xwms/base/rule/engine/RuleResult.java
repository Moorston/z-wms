package com.xwms.base.rule.engine;

import java.util.Map;

import lombok.Data;

/** 规则执行结果 */
@Data
public class RuleResult {
    private boolean matched;
    private String ruleId;
    private Map<String, Object> data;
    private String message;

    public static RuleResult of(String ruleId, Map<String, Object> data) {
        RuleResult r = new RuleResult();
        r.setMatched(true);
        r.setRuleId(ruleId);
        r.setData(data);
        return r;
    }

    public static RuleResult notMatched() {
        RuleResult r = new RuleResult();
        r.setMatched(false);
        return r;
    }
}
