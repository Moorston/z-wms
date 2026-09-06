package com.xwms.base.statemachine;

import java.util.Set;

import org.springframework.stereotype.Component;

import com.xwms.common.statemachine.annotation.StateMachine;
import com.xwms.common.statemachine.annotation.Transition;

/**
 * 产品状态机
 *
 * <p>状态流转: DRAFT(草稿) → ACTIVE(正常) → DISABLED(停用) → ACTIVE(启用) ACTIVE → FROZEN(冻结) → ACTIVE(解冻)
 */
@Component
@StateMachine(name = "productStateMachine", description = "产品状态机")
public class ProductStateMachine {

    @Transition(from = "DRAFT", to = "ACTIVE", event = "PUBLISH", description = "发布")
    public void publish() {}

    @Transition(from = "ACTIVE", to = "DISABLED", event = "DISABLE", description = "停用")
    public void disable() {}

    @Transition(from = "DISABLED", to = "ACTIVE", event = "ENABLE", description = "启用")
    public void enable() {}

    @Transition(from = "ACTIVE", to = "FROZEN", event = "FREEZE", description = "冻结")
    public void freeze() {}

    @Transition(from = "FROZEN", to = "ACTIVE", event = "UNFREEZE", description = "解冻")
    public void unfreeze() {}

    public Set<String> getAllStates() {
        return Set.of("DRAFT", "ACTIVE", "DISABLED", "FROZEN");
    }
}
