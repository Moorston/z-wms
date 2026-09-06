package com.xwms.core.statemachine;

import com.xwms.common.statemachine.annotation.StateMachine;
import com.xwms.common.statemachine.annotation.Transition;

/**
 * 作业任务状态机
 *
 * <p>状态流转： PENDING(待执行) → PROCESSING(执行中) → COMPLETED(已完成) ↓ EXCEPTION(异常) → PROCESSING(重试)
 * PENDING/PROCESSING → CANCELLED(已取消)
 *
 * <p>事件： START 开始执行 COMPLETE 完成 FAIL 异常 RETRY 重试 CANCEL 取消
 */
@StateMachine(name = "workTask", description = "作业任务状态机", initialState = "PENDING")
public interface WorkTaskStateMachine {

    @Transition(from = "PENDING", to = "PROCESSING", event = "START", description = "开始执行作业")
    void start();

    @Transition(from = "PROCESSING", to = "COMPLETED", event = "COMPLETE", description = "作业完成")
    void complete();

    @Transition(from = "PROCESSING", to = "EXCEPTION", event = "FAIL", description = "作业执行异常")
    void fail();

    @Transition(from = "EXCEPTION", to = "PROCESSING", event = "RETRY", description = "异常后重试")
    void retry();

    @Transition(from = "PENDING", to = "CANCELLED", event = "CANCEL", description = "取消作业")
    void cancelPending();

    @Transition(from = "PROCESSING", to = "CANCELLED", event = "CANCEL", description = "执行中取消")
    void cancelProcessing();

    @Transition(from = "EXCEPTION", to = "CANCELLED", event = "CANCEL", description = "异常后取消")
    void cancelException();
}
