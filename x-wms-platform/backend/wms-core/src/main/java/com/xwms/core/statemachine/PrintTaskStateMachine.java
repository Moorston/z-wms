package com.xwms.core.statemachine;

import java.util.Set;

import org.springframework.stereotype.Component;

import com.xwms.common.statemachine.annotation.StateMachine;
import com.xwms.common.statemachine.annotation.Transition;

/**
 * 打印任务状态机
 *
 * <p>状态流转: PENDING(待打印) → PRINTING(打印中) → SUCCESS(成功) PENDING → CANCELLED(已取消) PRINTING →
 * FAILED(失败) → PENDING(重试)
 */
@Component
@StateMachine(name = "printTaskStateMachine", description = "打印任务状态机")
public class PrintTaskStateMachine {

    @Transition(from = "PENDING", to = "PRINTING", event = "START", description = "开始打印")
    public void start() {}

    @Transition(from = "PRINTING", to = "SUCCESS", event = "COMPLETE", description = "打印成功")
    public void complete() {}

    @Transition(from = "PRINTING", to = "FAILED", event = "FAIL", description = "打印失败")
    public void fail() {}

    @Transition(from = "FAILED", to = "PENDING", event = "RETRY", description = "重试打印")
    public void retry() {}

    @Transition(from = "PENDING", to = "CANCELLED", event = "CANCEL", description = "取消打印")
    public void cancel() {}

    public Set<String> getAllStates() {
        return Set.of("PENDING", "PRINTING", "SUCCESS", "FAILED", "CANCELLED");
    }
}
