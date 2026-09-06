package com.xwms.analytics.statemachine;

import java.util.Set;

import org.springframework.stereotype.Component;

import com.xwms.common.statemachine.annotation.StateMachine;
import com.xwms.common.statemachine.annotation.Transition;

/**
 * KPI目标状态机
 *
 * <p>状态流转: ACTIVE(生效) → EXPIRED(过期)
 */
@Component
@StateMachine(name = "kpiTargetStateMachine", description = "KPI目标状态机")
public class KpiTargetStateMachine {

    @Transition(from = "ACTIVE", to = "EXPIRED", event = "EXPIRE", description = "目标过期")
    public void expire() {}

    public Set<String> getAllStates() {
        return Set.of("ACTIVE", "EXPIRED");
    }
}
