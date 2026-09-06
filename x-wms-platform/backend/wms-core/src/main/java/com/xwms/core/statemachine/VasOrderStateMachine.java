package com.xwms.core.statemachine;

import java.util.Set;

import org.springframework.stereotype.Component;

import com.xwms.common.statemachine.annotation.StateMachine;
import com.xwms.common.statemachine.annotation.Transition;
import com.xwms.core.vas.enums.VasOrderStatus;

/**
 * VAS工单状态机
 *
 * <p>状态流转: PENDING(待处理) → ASSIGNED(已分配) → PROCESSING(处理中) → COMPLETED(完成) PENDING →
 * PROCESSING(直接开始) PROCESSING → PAUSED(暂停) → PROCESSING(恢复) PENDING/ASSIGNED/PAUSED → CANCELLED(取消)
 * PROCESSING → EXCEPTION(异常) → PROCESSING(解决) / CANCELLED(取消)
 */
@Component
@StateMachine(name = "vasOrderStateMachine", description = "VAS工单状态机")
public class VasOrderStateMachine {

    @Transition(from = "PENDING", to = "ASSIGNED", event = "ASSIGN", description = "分配作业员")
    public void assign() {}

    @Transition(from = "PENDING", to = "PROCESSING", event = "START", description = "开始执行")
    public void start() {}

    @Transition(from = "ASSIGNED", to = "PROCESSING", event = "START", description = "开始执行")
    public void startAssigned() {}

    @Transition(from = "PROCESSING", to = "PAUSED", event = "PAUSE", description = "暂停")
    public void pause() {}

    @Transition(from = "PAUSED", to = "PROCESSING", event = "RESUME", description = "恢复")
    public void resume() {}

    @Transition(from = "PROCESSING", to = "COMPLETED", event = "COMPLETE", description = "完成")
    public void complete() {}

    @Transition(from = "PAUSED", to = "COMPLETED", event = "COMPLETE", description = "完成")
    public void completePaused() {}

    @Transition(from = "PENDING", to = "CANCELLED", event = "CANCEL", description = "取消")
    public void cancelPending() {}

    @Transition(from = "ASSIGNED", to = "CANCELLED", event = "CANCEL", description = "取消")
    public void cancelAssigned() {}

    @Transition(from = "PAUSED", to = "CANCELLED", event = "CANCEL", description = "取消")
    public void cancelPaused() {}

    @Transition(from = "PROCESSING", to = "EXCEPTION", event = "EXCEPTION", description = "异常")
    public void exception() {}

    @Transition(from = "EXCEPTION", to = "PROCESSING", event = "RESOLVE", description = "异常解决")
    public void resolve() {}

    @Transition(from = "EXCEPTION", to = "CANCELLED", event = "CANCEL", description = "取消")
    public void cancelException() {}

    public Set<String> getAllStates() {
        return Set.of(
                VasOrderStatus.PENDING.getCode(),
                VasOrderStatus.ASSIGNED.getCode(),
                VasOrderStatus.PROCESSING.getCode(),
                VasOrderStatus.PAUSED.getCode(),
                VasOrderStatus.COMPLETED.getCode(),
                VasOrderStatus.CANCELLED.getCode(),
                VasOrderStatus.EXCEPTION.getCode());
    }
}
