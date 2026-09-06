package com.xwms.core.statemachine;

import java.util.Set;

import org.springframework.stereotype.Component;

import com.xwms.common.statemachine.annotation.StateMachine;
import com.xwms.common.statemachine.annotation.Transition;

/**
 * 越库单状态机
 *
 * <p>状态流转: CREATED(已创建) → MATCHED(已匹配) → RECEIVING(收货中) → RECEIVED(已收货) → SORTING(分拣中) →
 * SORTED(已分拣) → SHIPPING(发运中) → SHIPPED(已发运) CREATED/MATCHED → CANCELLED(已取消)
 */
@Component
@StateMachine(name = "crossdockStateMachine", description = "越库单状态机")
public class CrossdockStateMachine {

    @Transition(from = "CREATED", to = "MATCHED", event = "MATCH", description = "匹配")
    public void match() {}

    @Transition(from = "MATCHED", to = "RECEIVING", event = "START_RECEIVE", description = "开始收货")
    public void startReceive() {}

    @Transition(
            from = "RECEIVING",
            to = "RECEIVED",
            event = "COMPLETE_RECEIVE",
            description = "收货完成")
    public void completeReceive() {}

    @Transition(from = "RECEIVED", to = "SORTING", event = "START_SORT", description = "开始分拣")
    public void startSort() {}

    @Transition(from = "SORTING", to = "SORTED", event = "COMPLETE_SORT", description = "分拣完成")
    public void completeSort() {}

    @Transition(from = "SORTED", to = "SHIPPING", event = "START_SHIP", description = "开始发运")
    public void startShip() {}

    @Transition(from = "SHIPPING", to = "SHIPPED", event = "COMPLETE_SHIP", description = "发运完成")
    public void completeShip() {}

    @Transition(from = "CREATED", to = "CANCELLED", event = "CANCEL", description = "取消")
    public void cancelCreated() {}

    @Transition(from = "MATCHED", to = "CANCELLED", event = "CANCEL", description = "取消")
    public void cancelMatched() {}

    public Set<String> getAllStates() {
        return Set.of(
                "CREATED",
                "MATCHED",
                "RECEIVING",
                "RECEIVED",
                "SORTING",
                "SORTED",
                "SHIPPING",
                "SHIPPED",
                "CANCELLED");
    }
}
