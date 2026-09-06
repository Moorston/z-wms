package com.xwms.core.statemachine;

import java.util.Set;

import org.springframework.stereotype.Component;

import com.xwms.common.statemachine.annotation.StateMachine;
import com.xwms.common.statemachine.annotation.Transition;

/**
 * 入库单状态机
 *
 * <p>状态流转: CREATED(已创建) → RECEIVING(收货中) → RECEIVED(已收货) → QCING(质检中) → PUTAWAYING(上架中) → DONE(已完成)
 * 任意状态 → CANCELLED(已取消)
 */
@Component
@StateMachine(name = "inboundOrderStateMachine", description = "入库单状态机")
public class InboundOrderStateMachine {

    @Transition(from = "CREATED", to = "RECEIVING", event = "START_RECEIVE", description = "开始收货")
    public void startReceive() {}

    @Transition(
            from = "RECEIVING",
            to = "RECEIVED",
            event = "COMPLETE_RECEIVE",
            description = "收货完成")
    public void completeReceive() {}

    @Transition(from = "RECEIVED", to = "QCING", event = "START_QC", description = "开始质检")
    public void startQc() {}

    @Transition(from = "QCING", to = "RECEIVED", event = "COMPLETE_QC", description = "质检完成")
    public void completeQc() {}

    @Transition(from = "RECEIVED", to = "PUTAWAYING", event = "START_PUTAWAY", description = "开始上架")
    public void startPutaway() {}

    @Transition(from = "PUTAWAYING", to = "DONE", event = "COMPLETE_PUTAWAY", description = "上架完成")
    public void completePutaway() {}

    @Transition(from = "CREATED", to = "CANCELLED", event = "CANCEL", description = "取消")
    public void cancelCreated() {}

    @Transition(from = "RECEIVING", to = "CANCELLED", event = "CANCEL", description = "取消")
    public void cancelReceiving() {}

    public Set<String> getAllStates() {
        return Set.of(
                "CREATED", "RECEIVING", "RECEIVED", "QCING", "PUTAWAYING", "DONE", "CANCELLED");
    }
}
