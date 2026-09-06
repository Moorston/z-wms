package com.xwms.base.statemachine;

import java.util.Set;

import org.springframework.stereotype.Component;

import com.xwms.common.statemachine.annotation.StateMachine;
import com.xwms.common.statemachine.annotation.Transition;

/**
 * 库位状态机
 *
 * <p>状态流转: EMPTY(空) → NORMAL(正常) → FULL(已满) NORMAL → EMPTY(空) NORMAL/FULL → FROZEN(冻结) → NORMAL(解冻)
 * 任意状态 → DISABLED(停用)
 */
@Component
@StateMachine(name = "locationStateMachine", description = "库位状态机")
public class LocationStateMachine {

    @Transition(from = "EMPTY", to = "NORMAL", event = "PUTAWAY", description = "上架入库")
    public void putaway() {}

    @Transition(from = "NORMAL", to = "FULL", event = "FILL", description = "库位已满")
    public void fill() {}

    @Transition(from = "FULL", to = "NORMAL", event = "PICK", description = "拣货出库")
    public void pickFromFull() {}

    @Transition(from = "NORMAL", to = "EMPTY", event = "EMPTY", description = "全部出库")
    public void empty() {}

    @Transition(from = "NORMAL", to = "FROZEN", event = "FREEZE", description = "冻结库位")
    public void freeze() {}

    @Transition(from = "FULL", to = "FROZEN", event = "FREEZE", description = "冻结库位")
    public void freezeFull() {}

    @Transition(from = "FROZEN", to = "NORMAL", event = "UNFREEZE", description = "解冻库位")
    public void unfreeze() {}

    @Transition(from = "EMPTY", to = "DISABLED", event = "DISABLE", description = "停用库位")
    public void disableEmpty() {}

    @Transition(from = "DISABLED", to = "EMPTY", event = "ENABLE", description = "启用库位")
    public void enable() {}

    public Set<String> getAllStates() {
        return Set.of("EMPTY", "NORMAL", "FULL", "FROZEN", "DISABLED");
    }
}
