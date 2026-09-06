package com.xwms.core.statemachine;

import java.util.Set;

import org.springframework.stereotype.Component;

import com.xwms.common.statemachine.annotation.StateMachine;
import com.xwms.common.statemachine.annotation.Transition;

/**
 * 预警记录状态机
 *
 * <p>状态流转: PENDING(待处理) → PROCESSING(处理中) → RESOLVED(已解决) PENDING/PROCESSING → IGNORED(已忽略)
 */
@Component
@StateMachine(name = "alertRecordStateMachine", description = "预警记录状态机")
public class AlertRecordStateMachine {

    @Transition(from = "PENDING", to = "PROCESSING", event = "ACK", description = "确认预警")
    public void ack() {}

    @Transition(from = "PROCESSING", to = "RESOLVED", event = "RESOLVE", description = "解决预警")
    public void resolve() {}

    @Transition(from = "PENDING", to = "IGNORED", event = "IGNORE", description = "忽略预警")
    public void ignorePending() {}

    @Transition(from = "PROCESSING", to = "IGNORED", event = "IGNORE", description = "忽略预警")
    public void ignoreProcessing() {}

    public Set<String> getAllStates() {
        return Set.of("PENDING", "PROCESSING", "RESOLVED", "IGNORED");
    }
}
