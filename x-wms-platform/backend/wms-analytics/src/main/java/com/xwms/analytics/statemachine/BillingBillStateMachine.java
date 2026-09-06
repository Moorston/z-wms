package com.xwms.analytics.statemachine;

import java.util.Set;

import org.springframework.stereotype.Component;

import com.xwms.analytics.billing.enums.BillStatus;
import com.xwms.common.statemachine.annotation.StateMachine;
import com.xwms.common.statemachine.annotation.Transition;

/**
 * 账单状态机
 *
 * <p>状态流转: DRAFT(草稿) → PENDING(待确认) → CONFIRMED(已确认) → INVOICED(已开票) → PARTIAL_PAID(部分付款) →
 * PAID(已付款) CONFIRMED/INVOICED → PARTIAL_PAID → PAID INVOICED → OVERDUE(逾期) → PAID DRAFT/PENDING →
 * CANCELLED(取消)
 */
@Component
@StateMachine(name = "billingBillStateMachine", description = "账单状态机")
public class BillingBillStateMachine {

    @Transition(from = "DRAFT", to = "PENDING", event = "SUBMIT", description = "提交确认")
    public void submit() {}

    @Transition(from = "PENDING", to = "CONFIRMED", event = "CONFIRM", description = "确认账单")
    public void confirm() {}

    @Transition(from = "DRAFT", to = "CONFIRMED", event = "CONFIRM", description = "直接确认")
    public void confirmDraft() {}

    @Transition(from = "CONFIRMED", to = "INVOICED", event = "INVOICE", description = "开票")
    public void invoice() {}

    @Transition(
            from = "CONFIRMED",
            to = "PARTIAL_PAID",
            event = "PARTIAL_PAY",
            description = "部分付款")
    public void partialPay() {}

    @Transition(from = "INVOICED", to = "PARTIAL_PAID", event = "PARTIAL_PAY", description = "部分付款")
    public void partialPayInvoiced() {}

    @Transition(from = "PARTIAL_PAID", to = "PAID", event = "FULL_PAY", description = "全额付款")
    public void fullPay() {}

    @Transition(from = "CONFIRMED", to = "PAID", event = "FULL_PAY", description = "全额付款")
    public void fullPayConfirmed() {}

    @Transition(from = "INVOICED", to = "PAID", event = "FULL_PAY", description = "全额付款")
    public void fullPayInvoiced() {}

    @Transition(from = "INVOICED", to = "OVERDUE", event = "OVERDUE", description = "逾期")
    public void overdue() {}

    @Transition(from = "OVERDUE", to = "PAID", event = "FULL_PAY", description = "逾期后付款")
    public void payOverdue() {}

    @Transition(from = "DRAFT", to = "CANCELLED", event = "CANCEL", description = "取消")
    public void cancelDraft() {}

    @Transition(from = "PENDING", to = "CANCELLED", event = "CANCEL", description = "取消")
    public void cancelPending() {}

    public Set<String> getAllStates() {
        return Set.of(
                BillStatus.DRAFT.getCode(),
                BillStatus.PENDING.getCode(),
                BillStatus.CONFIRMED.getCode(),
                BillStatus.INVOICED.getCode(),
                BillStatus.PARTIAL_PAID.getCode(),
                BillStatus.PAID.getCode(),
                BillStatus.OVERDUE.getCode(),
                BillStatus.CANCELLED.getCode());
    }
}
