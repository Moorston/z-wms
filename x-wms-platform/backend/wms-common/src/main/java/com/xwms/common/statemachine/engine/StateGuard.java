package com.xwms.common.statemachine.engine;

import java.util.Map;

/**
 * 状态守卫条件接口 在状态流转前执行，返回false则阻止流转
 *
 * <p>实现类需注册为Spring Bean，通过@Transition(guard="beanName")引用
 */
public interface StateGuard {

    /**
     * 守卫条件判断
     *
     * @param fromState 源状态
     * @param toState 目标状态
     * @param event 触发事件
     * @param context 上下文参数
     * @return true=允许流转, false=阻止流转
     */
    boolean evaluate(String fromState, String toState, String event, Map<String, Object> context);
}
