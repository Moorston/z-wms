package com.xwms.core.statemachine;

import java.util.Set;

import org.springframework.stereotype.Component;

import com.xwms.common.statemachine.annotation.StateMachine;
import com.xwms.common.statemachine.annotation.Transition;
import com.xwms.core.returnorder.enums.ReturnOrderStatus;

/**
 * 退货单状态机
 *
 * <p>状态流转: CREATED(已创建) → RECEIVING(收货中) → RECEIVED(已收货) → QC(质检中) → QC_PASSED(质检合格) →
 * PUTAWAYING(上架中) → COMPLETED(完成) → QC_FAILED(质检不合格) → PUTAWAYING(上架中, 次品/二手) → COMPLETED CREATED →
 * REJECTED(拒收) CREATED → CANCELLED(取消)
 */
@Component
@StateMachine(name = "returnOrderStateMachine", description = "退货单状态机")
public class ReturnOrderStateMachine {

    @Transition(from = "CREATED", to = "RECEIVING", event = "START_RECEIVE", description = "开始收货")
    public void startReceive() {}

    @Transition(from = "RECEIVING", to = "RECEIVED", event = "RECEIVE_DONE", description = "收货完成")
    public void receiveDone() {}

    @Transition(from = "RECEIVED", to = "QC", event = "START_QC", description = "开始质检")
    public void startQc() {}

    @Transition(from = "QC", to = "QC_PASSED", event = "QC_PASS", description = "质检合格")
    public void qcPass() {}

    @Transition(from = "QC", to = "QC_FAILED", event = "QC_FAIL", description = "质检不合格")
    public void qcFail() {}

    @Transition(
            from = "QC_PASSED",
            to = "PUTAWAYING",
            event = "START_PUTAWAY",
            description = "开始上架")
    public void startPutaway() {}

    @Transition(
            from = "QC_FAILED",
            to = "PUTAWAYING",
            event = "START_PUTAWAY",
            description = "开始上架(次品)")
    public void startPutawayFailed() {}

    @Transition(from = "PUTAWAYING", to = "COMPLETED", event = "PUTAWAY_DONE", description = "上架完成")
    public void putawayDone() {}

    @Transition(from = "CREATED", to = "REJECTED", event = "REJECT", description = "拒收")
    public void reject() {}

    @Transition(from = "RECEIVING", to = "REJECTED", event = "REJECT", description = "拒收")
    public void rejectReceiving() {}

    @Transition(from = "CREATED", to = "CANCELLED", event = "CANCEL", description = "取消")
    public void cancel() {}

    public Set<String> getAllStates() {
        return Set.of(
                ReturnOrderStatus.CREATED.getCode(),
                ReturnOrderStatus.RECEIVING.getCode(),
                ReturnOrderStatus.RECEIVED.getCode(),
                ReturnOrderStatus.QC.getCode(),
                ReturnOrderStatus.QC_PASSED.getCode(),
                ReturnOrderStatus.QC_FAILED.getCode(),
                ReturnOrderStatus.PUTAWAYING.getCode(),
                ReturnOrderStatus.COMPLETED.getCode(),
                ReturnOrderStatus.REJECTED.getCode(),
                ReturnOrderStatus.CANCELLED.getCode());
    }
}
