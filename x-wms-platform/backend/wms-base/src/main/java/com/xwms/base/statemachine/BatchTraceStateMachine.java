package com.xwms.base.statemachine;

import java.util.Set;

import org.springframework.stereotype.Component;

import com.xwms.common.statemachine.annotation.StateMachine;
import com.xwms.common.statemachine.annotation.Transition;

/**
 * 批次追踪状态机
 *
 * <p>批次生命周期: CREATED(已创建) → INBOUND(已入库) → STORAGE(在库) → PICKING(拣货中) → SORTING(分拣中) → SHIPPED(已出库)
 * → RETURNED(已退货) → SCRAPPED(已报废)
 */
@Component
@StateMachine(name = "batchTraceStateMachine", description = "批次追踪状态机")
public class BatchTraceStateMachine {

    @Transition(from = "CREATED", to = "INBOUND", event = "INBOUND", description = "入库")
    public void inbound() {}

    @Transition(from = "INBOUND", to = "STORAGE", event = "PUTAWAY", description = "上架")
    public void putaway() {}

    @Transition(from = "STORAGE", to = "PICKING", event = "PICK", description = "拣货")
    public void pick() {}

    @Transition(from = "PICKING", to = "SORTING", event = "SORT", description = "分拣")
    public void sort() {}

    @Transition(from = "SORTING", to = "SHIPPED", event = "SHIP", description = "发运")
    public void ship() {}

    @Transition(from = "PICKING", to = "STORAGE", event = "CANCEL_PICK", description = "取消拣货")
    public void cancelPick() {}

    @Transition(from = "SHIPPED", to = "RETURNED", event = "RETURN", description = "退货")
    public void returnBatch() {}

    @Transition(from = "RETURNED", to = "STORAGE", event = "REPUTAWAY", description = "重新上架")
    public void reputaway() {}

    @Transition(from = "STORAGE", to = "SCRAPPED", event = "SCRAP", description = "报废")
    public void scrap() {}

    @Transition(from = "STORAGE", to = "FROZEN", event = "FREEZE", description = "冻结")
    public void freeze() {}

    @Transition(from = "FROZEN", to = "STORAGE", event = "UNFREEZE", description = "解冻")
    public void unfreeze() {}

    public Set<String> getAllStates() {
        return Set.of(
                "CREATED",
                "INBOUND",
                "STORAGE",
                "PICKING",
                "SORTING",
                "SHIPPED",
                "RETURNED",
                "SCRAPPED",
                "FROZEN");
    }
}
