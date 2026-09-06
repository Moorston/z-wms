package com.xwms.core.statemachine;

import java.util.Set;

import org.springframework.stereotype.Component;

import com.xwms.common.statemachine.annotation.StateMachine;
import com.xwms.common.statemachine.annotation.Transition;

/**
 * 绩效考核状态机
 *
 * <p>状态流转: DRAFT(草稿) → CONFIRMED(已确认) → PUBLISHED(已发布) DRAFT → DRAFT(重新编辑)
 */
@Component
@StateMachine(name = "performanceAssessStateMachine", description = "绩效考核状态机")
public class PerformanceAssessStateMachine {

    @Transition(from = "DRAFT", to = "CONFIRMED", event = "CONFIRM", description = "确认考核")
    public void confirm() {}

    @Transition(from = "CONFIRMED", to = "PUBLISHED", event = "PUBLISH", description = "发布考核")
    public void publish() {}

    @Transition(from = "CONFIRMED", to = "DRAFT", event = "REVISE", description = "退回修改")
    public void revise() {}

    public Set<String> getAllStates() {
        return Set.of("DRAFT", "CONFIRMED", "PUBLISHED");
    }
}
