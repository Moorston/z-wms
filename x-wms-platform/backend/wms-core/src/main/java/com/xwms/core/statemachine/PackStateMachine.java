package com.xwms.core.statemachine;

import java.util.Set;

import org.springframework.stereotype.Component;

import com.xwms.common.statemachine.annotation.StateMachine;
import com.xwms.common.statemachine.annotation.Transition;

/**
 * 打包单状态机
 *
 * <p>状态流转: CREATED(已创建) → CHECKING(复核中) → CHECKED(已复核) → PACKING(打包中) → PACKED(已打包) → DONE(已完成)
 * CREATED → CANCELLED(已取消)
 */
@Component
@StateMachine(name = "packStateMachine", description = "打包单状态机")
public class PackStateMachine {

    @Transition(from = "CREATED", to = "CHECKING", event = "START_CHECK", description = "开始复核")
    public void startCheck() {}

    @Transition(from = "CHECKING", to = "CHECKED", event = "COMPLETE_CHECK", description = "复核完成")
    public void completeCheck() {}

    @Transition(from = "CHECKED", to = "PACKING", event = "START_PACK", description = "开始打包")
    public void startPack() {}

    @Transition(from = "PACKING", to = "PACKED", event = "COMPLETE_PACK", description = "打包完成")
    public void completePack() {}

    @Transition(from = "PACKED", to = "DONE", event = "COMPLETE", description = "完成")
    public void complete() {}

    @Transition(from = "CREATED", to = "CANCELLED", event = "CANCEL", description = "取消")
    public void cancel() {}

    public Set<String> getAllStates() {
        return Set.of("CREATED", "CHECKING", "CHECKED", "PACKING", "PACKED", "DONE", "CANCELLED");
    }
}
