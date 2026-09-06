package com.xwms.base.statemachine;

import java.util.Set;

import org.springframework.stereotype.Component;

import com.xwms.common.statemachine.annotation.StateMachine;
import com.xwms.common.statemachine.annotation.Transition;

/**
 * 租户状态机
 *
 * <p>状态流转: INACTIVE(未激活) → ACTIVE(正常) ACTIVE → FROZEN(已冻结) → ACTIVE(解冻) ACTIVE → EXPIRED(已过期)
 */
@Component
@StateMachine(name = "tenantStateMachine", description = "租户状态机")
public class TenantStateMachine {

    @Transition(from = "INACTIVE", to = "ACTIVE", event = "ACTIVATE", description = "激活租户")
    public void activate() {}

    @Transition(from = "ACTIVE", to = "FROZEN", event = "FREEZE", description = "冻结租户")
    public void freeze() {}

    @Transition(from = "FROZEN", to = "ACTIVE", event = "UNFREEZE", description = "解冻租户")
    public void unfreeze() {}

    @Transition(from = "ACTIVE", to = "EXPIRED", event = "EXPIRE", description = "租户过期")
    public void expire() {}

    public Set<String> getAllStates() {
        return Set.of("INACTIVE", "ACTIVE", "FROZEN", "EXPIRED");
    }
}
