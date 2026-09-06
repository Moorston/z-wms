package com.xwms.common.statemachine.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 状态流转定义注解 定义从一个状态到另一个状态的流转规则
 *
 * <p>使用方式：
 *
 * <pre>
 * &#64;Transition(from = "CREATED", to = "RECEIVING", event = "START_RECEIVE")
 * void startReceive();
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Transition {

    /** 源状态 */
    String from();

    /** 目标状态 */
    String to();

    /** 触发事件 */
    String event();

    /** 守卫条件（Spring Bean名称，实现StateGuard接口） */
    String guard() default "";

    /** 执行动作（Spring Bean名称，实现StateAction接口） */
    String action() default "";

    /** 流转描述 */
    String description() default "";
}
