package com.xwms.core.statemachine;

import java.util.Set;

import org.springframework.stereotype.Component;

import com.xwms.common.statemachine.annotation.StateMachine;
import com.xwms.common.statemachine.annotation.Transition;

/**
 * 调拨单状态机
 *
 * <p>状态流转: DRAFT(草稿) → CONFIRMED(已确认) → IN_TRANSIT(运输中) → RECEIVED(已收货) → COMPLETED(已完成)
 * DRAFT/CONFIRMED → CANCELLED(已取消)
 */
@Component
@StateMachine(name = "transferOrderStateMachine", description = "调拨单状态机")
public class TransferOrderStateMachine {

    @Transition(from = "DRAFT", to = "CONFIRMED", event = "CONFIRM", description = "确认调拨")
    public void confirm() {}

    @Transition(from = "CONFIRMED", to = "IN_TRANSIT", event = "SHIP", description = "调拨出库")
    public void ship() {}

    @Transition(from = "IN_TRANSIT", to = "RECEIVED", event = "RECEIVE", description = "调拨入库")
    public void receive() {}

    @Transition(from = "RECEIVED", to = "COMPLETED", event = "COMPLETE", description = "完成调拨")
    public void complete() {}

    @Transition(from = "DRAFT", to = "CANCELLED", event = "CANCEL", description = "取消调拨")
    public void cancelDraft() {}

    @Transition(from = "CONFIRMED", to = "CANCELLED", event = "CANCEL", description = "取消调拨")
    public void cancelConfirmed() {}

    public Set<String> getAllStates() {
        return Set.of("DRAFT", "CONFIRMED", "IN_TRANSIT", "RECEIVED", "COMPLETED", "CANCELLED");
    }
}
