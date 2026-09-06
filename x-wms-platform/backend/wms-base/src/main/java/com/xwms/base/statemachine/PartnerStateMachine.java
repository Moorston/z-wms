package com.xwms.base.statemachine;

import java.util.Set;

import org.springframework.stereotype.Component;

import com.xwms.common.statemachine.annotation.StateMachine;
import com.xwms.common.statemachine.annotation.Transition;

/**
 * 合作伙伴状态机
 *
 * <p>状态流转: ACTIVE(正常) → FROZEN(冻结) → ACTIVE(解冻) ACTIVE → DISABLED(停用) → ACTIVE(启用) FROZEN →
 * DISABLED(停用)
 */
@Component
@StateMachine(name = "partnerStateMachine", description = "合作伙伴状态机")
public class PartnerStateMachine {

    @Transition(from = "ACTIVE", to = "FROZEN", event = "FREEZE", description = "冻结")
    public void freeze() {}

    @Transition(from = "FROZEN", to = "ACTIVE", event = "UNFREEZE", description = "解冻")
    public void unfreeze() {}

    @Transition(from = "ACTIVE", to = "DISABLED", event = "DISABLE", description = "停用")
    public void disable() {}

    @Transition(from = "DISABLED", to = "ACTIVE", event = "ENABLE", description = "启用")
    public void enable() {}

    @Transition(from = "FROZEN", to = "DISABLED", event = "DISABLE", description = "冻结中停用")
    public void disableFrozen() {}

    public Set<String> getAllStates() {
        return Set.of("ACTIVE", "FROZEN", "DISABLED");
    }
}
