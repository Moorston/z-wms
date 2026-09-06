package com.xwms.core.statemachine;

import java.util.Set;

import org.springframework.stereotype.Component;

import com.xwms.common.statemachine.annotation.StateMachine;
import com.xwms.common.statemachine.annotation.Transition;
import com.xwms.core.stocktake.enums.StocktakeTaskStatus;

/**
 * 盘点任务状态机
 *
 * <p>状态流转: DRAFT(草稿) → PENDING(待执行) → COUNTING(盘点中) → RECOUNTING(复盘中) → ADJUSTING(调整中) →
 * COMPLETED(完成) DRAFT/PENDING → CANCELLED(取消)
 */
@Component
@StateMachine(name = "stocktakeTaskStateMachine", description = "盘点任务状态机")
public class StocktakeTaskStateMachine {

    @Transition(from = "DRAFT", to = "PENDING", event = "GENERATE_ITEMS", description = "生成明细")
    public void generateItems() {}

    @Transition(from = "PENDING", to = "COUNTING", event = "START", description = "开始盘点")
    public void start() {}

    @Transition(
            from = "COUNTING",
            to = "RECOUNTING",
            event = "TRIGGER_RECOUNT",
            description = "触发复盘")
    public void triggerRecount() {}

    @Transition(from = "RECOUNTING", to = "COUNTING", event = "RECOUNT_DONE", description = "复盘完成")
    public void recountDone() {}

    @Transition(from = "COUNTING", to = "ADJUSTING", event = "FINISH", description = "完成盘点生成差异")
    public void finish() {}

    @Transition(from = "RECOUNTING", to = "ADJUSTING", event = "FINISH", description = "完成盘点生成差异")
    public void finishRecount() {}

    @Transition(from = "ADJUSTING", to = "COMPLETED", event = "COMPLETE", description = "差异处理完成")
    public void complete() {}

    @Transition(from = "DRAFT", to = "CANCELLED", event = "CANCEL", description = "取消")
    public void cancelDraft() {}

    @Transition(from = "PENDING", to = "CANCELLED", event = "CANCEL", description = "取消")
    public void cancelPending() {}

    public Set<String> getAllStates() {
        return Set.of(
                StocktakeTaskStatus.DRAFT.getCode(),
                StocktakeTaskStatus.PENDING.getCode(),
                StocktakeTaskStatus.COUNTING.getCode(),
                StocktakeTaskStatus.RECOUNTING.getCode(),
                StocktakeTaskStatus.ADJUSTING.getCode(),
                StocktakeTaskStatus.COMPLETED.getCode(),
                StocktakeTaskStatus.CANCELLED.getCode());
    }
}
