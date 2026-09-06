package com.xwms.integration.statemachine;

import java.util.Set;

import org.springframework.stereotype.Component;

import com.xwms.common.statemachine.annotation.StateMachine;
import com.xwms.common.statemachine.annotation.Transition;

/**
 * API密钥状态机
 *
 * <p>状态流转: ACTIVE(正常) → DISABLED(禁用) → ACTIVE(启用) ACTIVE → EXPIRED(过期)
 */
@Component
@StateMachine(name = "apiKeyStateMachine", description = "API密钥状态机")
public class ApiKeyStateMachine {

    @Transition(from = "ACTIVE", to = "DISABLED", event = "DISABLE", description = "禁用")
    public void disable() {}

    @Transition(from = "DISABLED", to = "ACTIVE", event = "ENABLE", description = "启用")
    public void enable() {}

    @Transition(from = "ACTIVE", to = "EXPIRED", event = "EXPIRE", description = "过期")
    public void expire() {}

    public Set<String> getAllStates() {
        return Set.of("ACTIVE", "DISABLED", "EXPIRED");
    }
}
