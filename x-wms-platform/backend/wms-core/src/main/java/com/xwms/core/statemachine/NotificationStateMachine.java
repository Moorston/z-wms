package com.xwms.core.statemachine;

import java.util.Set;

import org.springframework.stereotype.Component;

import com.xwms.common.statemachine.annotation.StateMachine;
import com.xwms.common.statemachine.annotation.Transition;

/**
 * 通知状态机
 *
 * <p>状态流转: PENDING(待发送) → SENDING(发送中) → SENT(已发送) → READ(已读) PENDING/SENDING → FAILED(发送失败) →
 * PENDING(重试)
 */
@Component
@StateMachine(name = "notificationStateMachine", description = "通知状态机")
public class NotificationStateMachine {

    @Transition(from = "PENDING", to = "SENDING", event = "SEND", description = "开始发送")
    public void send() {}

    @Transition(from = "SENDING", to = "SENT", event = "SUCCESS", description = "发送成功")
    public void success() {}

    @Transition(from = "SENDING", to = "FAILED", event = "FAIL", description = "发送失败")
    public void fail() {}

    @Transition(from = "PENDING", to = "FAILED", event = "FAIL", description = "发送失败")
    public void failPending() {}

    @Transition(from = "FAILED", to = "PENDING", event = "RETRY", description = "重试")
    public void retry() {}

    @Transition(from = "SENT", to = "READ", event = "READ", description = "已读")
    public void read() {}

    public Set<String> getAllStates() {
        return Set.of("PENDING", "SENDING", "SENT", "FAILED", "READ");
    }
}
