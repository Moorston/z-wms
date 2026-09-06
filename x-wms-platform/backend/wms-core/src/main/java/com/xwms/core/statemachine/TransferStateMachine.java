package com.xwms.core.statemachine;

import java.util.Set;

import org.springframework.stereotype.Component;

import com.xwms.common.statemachine.annotation.StateMachine;
import com.xwms.common.statemachine.annotation.Transition;

/**
 * 调拨单状态机
 *
 * <p>状态流转: CREATED(已创建) → APPROVED(已审批) → SHIPPED(已发运) → IN_TRANSIT(在途) → RECEIVED(已收货) → DONE(已完成)
 * CREATED → CANCELLED(已取消)
 */
@Component
@StateMachine(name = "transferStateMachine", description = "调拨单状态机")
public class TransferStateMachine {

    @Transition(from = "CREATED", to = "APPROVED", event = "APPROVE", description = "审批")
    public void approve() {}

    @Transition(from = "APPROVED", to = "SHIPPED", event = "SHIP", description = "发运")
    public void ship() {}

    @Transition(from = "SHIPPED", to = "IN_TRANSIT", event = "COMPLETE_SHIP", description = "全部发运")
    public void completeShip() {}

    @Transition(from = "IN_TRANSIT", to = "RECEIVED", event = "RECEIVE", description = "收货")
    public void receive() {}

    @Transition(from = "RECEIVED", to = "DONE", event = "COMPLETE_RECEIVE", description = "全部收货")
    public void completeReceive() {}

    @Transition(from = "CREATED", to = "CANCELLED", event = "CANCEL", description = "取消")
    public void cancel() {}

    public Set<String> getAllStates() {
        return Set.of(
                "CREATED", "APPROVED", "SHIPPED", "IN_TRANSIT", "RECEIVED", "DONE", "CANCELLED");
    }
}
