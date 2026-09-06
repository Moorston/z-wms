package com.xwms.base.rule.engine;

import java.util.Map;

import com.xwms.common.plugin.WmsPlugin;

/** 规则插件接口 13类规则：上架/分配/周转/波次/补货/质检/越库/配送/路径/发运/预配/调度/归档 每种规则实现此接口，按优先级执行链 */
public interface RulePlugin extends WmsPlugin {
    /** 规则类型 */
    String getRuleType();

    /**
     * 执行规则
     *
     * @param context 规则上下文（输入参数）
     * @return 规则执行结果（null=未命中，继续下一个）
     */
    RuleResult execute(Map<String, Object> context);
}
