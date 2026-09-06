package com.xwms.common.statemachine.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 状态机定义注解 标记一个接口或类为状态机定义，包含状态流转规则
 *
 * <p>使用方式：
 *
 * <pre>
 * &#64;StateMachine(name = "inboundOrder", description = "入库单状态机")
 * public interface InboundOrderStateMachine {
 *     &#64;Transition(from = "CREATED", to = "RECEIVING", event = "START_RECEIVE")
 *     void startReceive();
 * }
 * </pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface StateMachine {

    /** 状态机名称（唯一） */
    String name();

    /** 状态机描述 */
    String description() default "";

    /** 初始状态 */
    String initialState() default "";
}
