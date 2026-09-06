package com.xwms.base.statemachine;

import java.util.Set;

import org.springframework.stereotype.Component;

import com.xwms.common.statemachine.annotation.StateMachine;
import com.xwms.common.statemachine.annotation.Transition;

/**
 * 规则状态机
 *
 * <p>状态流转: DRAFT(草稿) → ACTIVE(启用) → DISABLED(禁用) → ACTIVE(启用) ACTIVE → ARCHIVED(归档)
 */
@Component
@StateMachine(name = "ruleStateMachine", description = "规则状态机")
public class RuleStateMachine {

    @Transition(from = "DRAFT", to = "ACTIVE", event = "PUBLISH", description = "发布")
    public void publish() {}

    @Transition(from = "ACTIVE", to = "DISABLED", event = "DISABLE", description = "禁用")
    public void disable() {}

    @Transition(from = "DISABLED", to = "ACTIVE", event = "ENABLE", description = "启用")
    public void enable() {}

    @Transition(from = "ACTIVE", to = "ARCHIVED", event = "ARCHIVE", description = "归档")
    public void archive() {}

    public Set<String> getAllStates() {
        return Set.of("DRAFT", "ACTIVE", "DISABLED", "ARCHIVED");
    }
}
