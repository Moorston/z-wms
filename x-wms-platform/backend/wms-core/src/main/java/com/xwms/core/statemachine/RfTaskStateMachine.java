package com.xwms.core.statemachine;

import java.util.Set;

import org.springframework.stereotype.Component;

import com.xwms.common.statemachine.annotation.StateMachine;
import com.xwms.common.statemachine.annotation.Transition;

/**
 * RF任务状态机
 *
 * <p>状态流转: PENDING(待领取) → ASSIGNED(已分配) → IN_PROGRESS(进行中) → COMPLETED(已完成) IN_PROGRESS →
 * PAUSED(已暂停) → IN_PROGRESS(继续) PENDING/ASSIGNED/IN_PROGRESS/PAUSED → CANCELLED(已取消)
 */
@Component
@StateMachine(name = "rfTaskStateMachine", description = "RF任务状态机")
public class RfTaskStateMachine {

    @Transition(from = "PENDING", to = "ASSIGNED", event = "ACCEPT", description = "领取任务")
    public void accept() {}

    @Transition(from = "ASSIGNED", to = "IN_PROGRESS", event = "START", description = "开始任务")
    public void start() {}

    @Transition(from = "IN_PROGRESS", to = "PAUSED", event = "PAUSE", description = "暂停任务")
    public void pause() {}

    @Transition(from = "PAUSED", to = "IN_PROGRESS", event = "RESUME", description = "继续任务")
    public void resume() {}

    @Transition(from = "IN_PROGRESS", to = "COMPLETED", event = "COMPLETE", description = "完成任务")
    public void complete() {}

    @Transition(from = "PAUSED", to = "COMPLETED", event = "COMPLETE", description = "完成任务")
    public void completePaused() {}

    @Transition(from = "PENDING", to = "CANCELLED", event = "CANCEL", description = "取消任务")
    public void cancelPending() {}

    @Transition(from = "ASSIGNED", to = "CANCELLED", event = "CANCEL", description = "取消任务")
    public void cancelAssigned() {}

    @Transition(from = "IN_PROGRESS", to = "CANCELLED", event = "CANCEL", description = "取消任务")
    public void cancelInProgress() {}

    @Transition(from = "PAUSED", to = "CANCELLED", event = "CANCEL", description = "取消任务")
    public void cancelPaused() {}

    public Set<String> getAllStates() {
        return Set.of("PENDING", "ASSIGNED", "IN_PROGRESS", "PAUSED", "COMPLETED", "CANCELLED");
    }
}
