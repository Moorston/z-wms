package com.xwms.core.statemachine;

import java.util.Set;

import org.springframework.stereotype.Component;

import com.xwms.common.statemachine.annotation.StateMachine;
import com.xwms.common.statemachine.annotation.Transition;

/**
 * 归档任务状态机
 *
 * <p>状态流转: PENDING(待执行) → RUNNING(执行中) → COMPLETED(已完成) PENDING → CANCELLED(已取消) RUNNING →
 * FAILED(失败)
 */
@Component
@StateMachine(name = "archiveTaskStateMachine", description = "归档任务状态机")
public class ArchiveTaskStateMachine {

    @Transition(from = "PENDING", to = "RUNNING", event = "START", description = "开始执行")
    public void start() {}

    @Transition(from = "RUNNING", to = "COMPLETED", event = "COMPLETE", description = "执行完成")
    public void complete() {}

    @Transition(from = "RUNNING", to = "FAILED", event = "FAIL", description = "执行失败")
    public void fail() {}

    @Transition(from = "PENDING", to = "CANCELLED", event = "CANCEL", description = "取消任务")
    public void cancel() {}

    public Set<String> getAllStates() {
        return Set.of("PENDING", "RUNNING", "COMPLETED", "FAILED", "CANCELLED");
    }
}
