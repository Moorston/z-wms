package com.xwms.base.statemachine;

import java.util.Set;

import org.springframework.stereotype.Component;

import com.xwms.common.statemachine.annotation.StateMachine;
import com.xwms.common.statemachine.annotation.Transition;

/**
 * 安全审计状态机
 *
 * <p>状态流转: PENDING(待处理) → PROCESSING(处理中) → RESOLVED(已解决) PENDING → IGNORED(已忽略)
 */
@Component
@StateMachine(name = "securityAuditStateMachine", description = "安全审计状态机")
public class SecurityAuditStateMachine {

    @Transition(from = "PENDING", to = "PROCESSING", event = "START", description = "开始处理")
    public void start() {}

    @Transition(from = "PROCESSING", to = "RESOLVED", event = "RESOLVE", description = "已解决")
    public void resolve() {}

    @Transition(from = "PENDING", to = "IGNORED", event = "IGNORE", description = "忽略")
    public void ignore() {}

    @Transition(from = "PROCESSING", to = "IGNORED", event = "IGNORE", description = "忽略")
    public void ignoreProcessing() {}

    public Set<String> getAllStates() {
        return Set.of("PENDING", "PROCESSING", "RESOLVED", "IGNORED");
    }
}
