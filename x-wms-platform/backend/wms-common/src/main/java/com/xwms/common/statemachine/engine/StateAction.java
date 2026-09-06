package com.xwms.common.statemachine.engine;

import java.util.Map;

/**
 * 状态动作接口 在状态流转成功后执行，用于触发业务逻辑
 *
 * <p>实现类需注册为Spring Bean，通过@Transition(action="beanName")引用
 */
public interface StateAction {

    /**
     * 执行动作
     *
     * @param fromState 源状态
     * @param toState 目标状态
     * @param event 触发事件
     * @param context 上下文参数
     */
    void execute(String fromState, String toState, String event, Map<String, Object> context);
}
