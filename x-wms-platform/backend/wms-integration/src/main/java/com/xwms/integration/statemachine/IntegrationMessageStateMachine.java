package com.xwms.integration.statemachine;

import java.util.Set;

import org.springframework.stereotype.Component;

import com.xwms.common.statemachine.annotation.StateMachine;
import com.xwms.common.statemachine.annotation.Transition;

/**
 * 集成消息状态机
 *
 * <p>状态流转: PENDING(待发送) → SENDING(发送中) → SENT(已发送) PENDING → SENDING → FAILED(失败) → PENDING(重试)
 * SENT → CONSUMED(已消费)
 */
@Component
@StateMachine(name = "integrationMessageStateMachine", description = "集成消息状态机")
public class IntegrationMessageStateMachine {

    @Transition(from = "PENDING", to = "SENDING", event = "SEND", description = "开始发送")
    public void send() {}

    @Transition(from = "SENDING", to = "SENT", event = "SUCCESS", description = "发送成功")
    public void success() {}

    @Transition(from = "SENDING", to = "FAILED", event = "FAIL", description = "发送失败")
    public void fail() {}

    @Transition(from = "FAILED", to = "PENDING", event = "RETRY", description = "重试发送")
    public void retry() {}

    @Transition(from = "SENT", to = "CONSUMED", event = "CONSUME", description = "消息已消费")
    public void consume() {}

    public Set<String> getAllStates() {
        return Set.of("PENDING", "SENDING", "SENT", "FAILED", "CONSUMED");
    }
}
