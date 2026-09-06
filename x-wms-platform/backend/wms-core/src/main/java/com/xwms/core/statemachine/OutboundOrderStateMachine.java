package com.xwms.core.statemachine;

import java.util.Set;

import org.springframework.stereotype.Component;

import com.xwms.common.statemachine.annotation.StateMachine;
import com.xwms.common.statemachine.annotation.Transition;

/**
 * 出库单状态机
 *
 * <p>状态流转: CREATED(已创建) → ALLOCATING(分配中) → ALLOCATED(已分配) → PICKING(拣货中) → PICKED(已拣货) →
 * PACKING(打包中) → PACKED(已打包) → SHIPPING(发运中) → SHIPPED(已发运) 任意状态 → CANCELLED(已取消)
 */
@Component
@StateMachine(name = "outboundOrderStateMachine", description = "出库单状态机")
public class OutboundOrderStateMachine {

    @Transition(from = "CREATED", to = "ALLOCATING", event = "START_ALLOCATE", description = "开始分配")
    public void startAllocate() {}

    @Transition(
            from = "ALLOCATING",
            to = "ALLOCATED",
            event = "COMPLETE_ALLOCATE",
            description = "分配完成")
    public void completeAllocate() {}

    @Transition(from = "ALLOCATED", to = "PICKING", event = "START_PICK", description = "开始拣货")
    public void startPick() {}

    @Transition(from = "PICKING", to = "PICKED", event = "COMPLETE_PICK", description = "拣货完成")
    public void completePick() {}

    @Transition(from = "PICKED", to = "PACKING", event = "START_PACK", description = "开始打包")
    public void startPack() {}

    @Transition(from = "PACKING", to = "PACKED", event = "COMPLETE_PACK", description = "打包完成")
    public void completePack() {}

    @Transition(from = "PACKED", to = "SHIPPING", event = "START_SHIP", description = "开始发运")
    public void startShip() {}

    @Transition(from = "SHIPPING", to = "SHIPPED", event = "COMPLETE_SHIP", description = "发运完成")
    public void completeShip() {}

    @Transition(from = "ALLOCATED", to = "CREATED", event = "CANCEL_ALLOCATE", description = "取消分配")
    public void cancelAllocate() {}

    @Transition(from = "CREATED", to = "CANCELLED", event = "CANCEL", description = "取消")
    public void cancelCreated() {}

    @Transition(from = "ALLOCATED", to = "CANCELLED", event = "CANCEL", description = "取消")
    public void cancelAllocated() {}

    public Set<String> getAllStates() {
        return Set.of(
                "CREATED",
                "ALLOCATING",
                "ALLOCATED",
                "PICKING",
                "PICKED",
                "PACKING",
                "PACKED",
                "SHIPPING",
                "SHIPPED",
                "CANCELLED");
    }
}
