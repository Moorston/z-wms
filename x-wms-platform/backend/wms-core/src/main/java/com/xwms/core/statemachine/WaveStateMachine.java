package com.xwms.core.statemachine;

import java.util.Set;

import org.springframework.stereotype.Component;

import com.xwms.common.statemachine.annotation.StateMachine;
import com.xwms.common.statemachine.annotation.Transition;

/**
 * 波次状态机
 *
 * <p>状态流转: CREATED(已创建) → ALLOCATED(已分配) → PICKING(拣货中) → PICKED(已拣货) → PACKING(打包中) → DONE(已完成)
 * CREATED/ALLOCATED → CANCELLED(已取消)
 */
@Component
@StateMachine(name = "waveStateMachine", description = "波次状态机")
public class WaveStateMachine {

    @Transition(from = "CREATED", to = "ALLOCATED", event = "ALLOCATE", description = "分配")
    public void allocate() {}

    @Transition(from = "ALLOCATED", to = "PICKING", event = "START_PICK", description = "开始拣货")
    public void startPick() {}

    @Transition(from = "PICKING", to = "PICKED", event = "COMPLETE_PICK", description = "拣货完成")
    public void completePick() {}

    @Transition(from = "PICKED", to = "PACKING", event = "START_PACK", description = "开始打包")
    public void startPack() {}

    @Transition(from = "PACKING", to = "DONE", event = "COMPLETE", description = "完成")
    public void complete() {}

    @Transition(from = "CREATED", to = "CANCELLED", event = "CANCEL", description = "取消")
    public void cancelCreated() {}

    @Transition(from = "ALLOCATED", to = "CANCELLED", event = "CANCEL", description = "取消")
    public void cancelAllocated() {}

    public Set<String> getAllStates() {
        return Set.of("CREATED", "ALLOCATED", "PICKING", "PICKED", "PACKING", "DONE", "CANCELLED");
    }
}
