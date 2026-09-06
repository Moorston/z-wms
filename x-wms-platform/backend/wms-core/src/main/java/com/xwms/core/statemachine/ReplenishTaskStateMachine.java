package com.xwms.core.statemachine;

import java.util.Set;

import org.springframework.stereotype.Component;

import com.xwms.common.statemachine.annotation.StateMachine;
import com.xwms.common.statemachine.annotation.Transition;
import com.xwms.core.replenish.enums.ReplenishTaskStatus;

/**
 * 补货任务状态机
 *
 * <p>状态流转: PENDING(待处理) → ASSIGNED(已分配) → PICKING(拣货中) → PICKED(已拣货) → PUTAWAYING(上架中) →
 * COMPLETED(完成) PENDING/ASSIGNED → CANCELLED(取消) PICKING/PICKED/PUTAWAYING → EXCEPTION(异常) →
 * PICKING(异常解决) / CANCELLED(取消)
 */
@Component
@StateMachine(name = "replenishTaskStateMachine", description = "补货任务状态机")
public class ReplenishTaskStateMachine {

    @Transition(from = "PENDING", to = "ASSIGNED", event = "ASSIGN", description = "分配作业员")
    public void assign() {}

    @Transition(from = "PENDING", to = "PICKING", event = "START_PICK", description = "开始拣货")
    public void startPick() {}

    @Transition(from = "ASSIGNED", to = "PICKING", event = "START_PICK", description = "开始拣货")
    public void startPickAssigned() {}

    @Transition(from = "PICKING", to = "PICKED", event = "PICK_DONE", description = "拣货完成")
    public void pickDone() {}

    @Transition(from = "PICKED", to = "PUTAWAYING", event = "START_PUTAWAY", description = "开始上架")
    public void startPutaway() {}

    @Transition(from = "PUTAWAYING", to = "COMPLETED", event = "PUTAWAY_DONE", description = "上架完成")
    public void putawayDone() {}

    @Transition(from = "PENDING", to = "CANCELLED", event = "CANCEL", description = "取消")
    public void cancelPending() {}

    @Transition(from = "ASSIGNED", to = "CANCELLED", event = "CANCEL", description = "取消")
    public void cancelAssigned() {}

    @Transition(from = "PICKING", to = "EXCEPTION", event = "EXCEPTION", description = "异常")
    public void exception() {}

    @Transition(from = "PICKED", to = "EXCEPTION", event = "EXCEPTION", description = "异常")
    public void exceptionPicked() {}

    @Transition(from = "PUTAWAYING", to = "EXCEPTION", event = "EXCEPTION", description = "异常")
    public void exceptionPutawaying() {}

    @Transition(from = "EXCEPTION", to = "PICKING", event = "RESOLVE", description = "异常解决")
    public void resolve() {}

    @Transition(from = "EXCEPTION", to = "CANCELLED", event = "CANCEL", description = "取消")
    public void cancelException() {}

    public Set<String> getAllStates() {
        return Set.of(
                ReplenishTaskStatus.PENDING.getCode(),
                ReplenishTaskStatus.ASSIGNED.getCode(),
                ReplenishTaskStatus.PICKING.getCode(),
                ReplenishTaskStatus.PICKED.getCode(),
                ReplenishTaskStatus.PUTAWAYING.getCode(),
                ReplenishTaskStatus.COMPLETED.getCode(),
                ReplenishTaskStatus.EXCEPTION.getCode(),
                ReplenishTaskStatus.CANCELLED.getCode());
    }
}
