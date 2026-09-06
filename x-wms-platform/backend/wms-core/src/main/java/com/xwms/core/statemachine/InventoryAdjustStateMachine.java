package com.xwms.core.statemachine;

import java.util.Set;

import org.springframework.stereotype.Component;

import com.xwms.common.statemachine.annotation.StateMachine;
import com.xwms.common.statemachine.annotation.Transition;

/**
 * 库存调整单状态机
 *
 * <p>状态流转: CREATED(已创建) → APPROVED(已审批) → DONE(已执行) CREATED → CANCELLED(已取消)
 */
@Component
@StateMachine(name = "inventoryAdjustStateMachine", description = "库存调整单状态机")
public class InventoryAdjustStateMachine {

    @Transition(from = "CREATED", to = "APPROVED", event = "APPROVE", description = "审批")
    public void approve() {}

    @Transition(from = "APPROVED", to = "DONE", event = "EXECUTE", description = "执行")
    public void execute() {}

    @Transition(from = "CREATED", to = "CANCELLED", event = "CANCEL", description = "取消")
    public void cancel() {}

    public Set<String> getAllStates() {
        return Set.of("CREATED", "APPROVED", "DONE", "CANCELLED");
    }
}
