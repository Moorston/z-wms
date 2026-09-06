package com.xwms.core.statemachine;

import java.util.Set;

import org.springframework.stereotype.Component;

import com.xwms.common.statemachine.annotation.StateMachine;
import com.xwms.common.statemachine.annotation.Transition;
import com.xwms.core.yard.enums.AppointmentStatus;

/**
 * 预约单状态机
 *
 * <p>状态流转: PENDING(待确认) → CONFIRMED(已确认) → ARRIVED(已到达) → CHECKED_IN(已签到) → LOADING(装卸中) →
 * COMPLETED(已完成) PENDING/CONFIRMED → CANCELLED(取消) CONFIRMED → NO_SHOW(未到) / OVERDUE(逾期)
 */
@Component
@StateMachine(name = "appointmentStateMachine", description = "预约单状态机")
public class AppointmentStateMachine {

    @Transition(from = "PENDING", to = "CONFIRMED", event = "CONFIRM", description = "确认预约")
    public void confirm() {}

    @Transition(from = "CONFIRMED", to = "ARRIVED", event = "ARRIVE", description = "车辆到达")
    public void arrive() {}

    @Transition(from = "ARRIVED", to = "CHECKED_IN", event = "CHECK_IN", description = "签到")
    public void checkIn() {}

    @Transition(from = "CONFIRMED", to = "CHECKED_IN", event = "CHECK_IN", description = "直接签到")
    public void checkInDirect() {}

    @Transition(from = "CHECKED_IN", to = "LOADING", event = "START_LOADING", description = "开始装卸")
    public void startLoading() {}

    @Transition(from = "LOADING", to = "COMPLETED", event = "CHECK_OUT", description = "签退离场")
    public void checkOut() {}

    @Transition(from = "CHECKED_IN", to = "COMPLETED", event = "CHECK_OUT", description = "直接签退")
    public void checkOutDirect() {}

    @Transition(from = "PENDING", to = "CANCELLED", event = "CANCEL", description = "取消")
    public void cancelPending() {}

    @Transition(from = "CONFIRMED", to = "CANCELLED", event = "CANCEL", description = "取消")
    public void cancelConfirmed() {}

    @Transition(from = "CONFIRMED", to = "NO_SHOW", event = "NO_SHOW", description = "未到")
    public void noShow() {}

    @Transition(from = "CONFIRMED", to = "OVERDUE", event = "OVERDUE", description = "逾期")
    public void overdue() {}

    public Set<String> getAllStates() {
        return Set.of(
                AppointmentStatus.PENDING.getCode(),
                AppointmentStatus.CONFIRMED.getCode(),
                AppointmentStatus.CANCELLED.getCode(),
                AppointmentStatus.ARRIVED.getCode(),
                AppointmentStatus.CHECKED_IN.getCode(),
                AppointmentStatus.LOADING.getCode(),
                AppointmentStatus.COMPLETED.getCode(),
                AppointmentStatus.NO_SHOW.getCode(),
                AppointmentStatus.OVERDUE.getCode());
    }
}
