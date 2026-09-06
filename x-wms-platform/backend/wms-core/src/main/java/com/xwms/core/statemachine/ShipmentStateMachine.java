package com.xwms.core.statemachine;

import java.util.Set;

import org.springframework.stereotype.Component;

import com.xwms.common.statemachine.annotation.StateMachine;
import com.xwms.common.statemachine.annotation.Transition;

/**
 * 发运单状态机
 *
 * <p>状态流转: CREATED(已创建) → PRINTED(已打印) → PICKED_UP(已揽收) → IN_TRANSIT(在途) → DELIVERED(已送达)
 * CREATED/PRINTED → FAILED(失败) → CREATED(重试) CREATED/PRINTED → CANCELLED(已取消)
 */
@Component
@StateMachine(name = "shipmentStateMachine", description = "发运单状态机")
public class ShipmentStateMachine {

    @Transition(from = "CREATED", to = "PRINTED", event = "PRINT", description = "打印")
    public void print() {}

    @Transition(from = "PRINTED", to = "PICKED_UP", event = "PICKUP", description = "揽收")
    public void pickup() {}

    @Transition(from = "PICKED_UP", to = "IN_TRANSIT", event = "IN_TRANSIT", description = "在途")
    public void inTransit() {}

    @Transition(from = "IN_TRANSIT", to = "DELIVERED", event = "DELIVER", description = "送达")
    public void deliver() {}

    @Transition(from = "CREATED", to = "FAILED", event = "FAIL", description = "失败")
    public void failCreated() {}

    @Transition(from = "PRINTED", to = "FAILED", event = "FAIL", description = "失败")
    public void failPrinted() {}

    @Transition(from = "FAILED", to = "CREATED", event = "RETRY", description = "重试")
    public void retry() {}

    @Transition(from = "CREATED", to = "CANCELLED", event = "CANCEL", description = "取消")
    public void cancelCreated() {}

    @Transition(from = "PRINTED", to = "CANCELLED", event = "CANCEL", description = "取消")
    public void cancelPrinted() {}

    public Set<String> getAllStates() {
        return Set.of(
                "CREATED",
                "PRINTED",
                "PICKED_UP",
                "IN_TRANSIT",
                "DELIVERED",
                "FAILED",
                "CANCELLED");
    }
}
