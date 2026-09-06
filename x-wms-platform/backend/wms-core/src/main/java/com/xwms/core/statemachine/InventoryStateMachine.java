package com.xwms.core.statemachine;

import com.xwms.common.statemachine.annotation.StateMachine;
import com.xwms.common.statemachine.annotation.Transition;

/**
 * 库存状态机
 *
 * <p>状态流转： NORMAL(正常) ↔ FROZEN(冻结) NORMAL → DEFECTIVE(残次) NORMAL ↔ PENDING_QC(待检) NORMAL ↔
 * RESERVED(预留)
 *
 * <p>事件： FREEZE 冻结 UNFREEZE 解冻 MARK_DEFECT 标记残次 START_QC 送检 COMPLETE_QC 质检完成 RESERVE 预留 UNRESERVE
 * 释放预留
 */
@StateMachine(name = "inventory", description = "库存状态机", initialState = "NORMAL")
public interface InventoryStateMachine {

    @Transition(from = "NORMAL", to = "FROZEN", event = "FREEZE", description = "冻结库存")
    void freeze();

    @Transition(from = "FROZEN", to = "NORMAL", event = "UNFREEZE", description = "解冻库存")
    void unfreeze();

    @Transition(from = "NORMAL", to = "DEFECTIVE", event = "MARK_DEFECT", description = "标记残次")
    void markDefect();

    @Transition(from = "NORMAL", to = "PENDING_QC", event = "START_QC", description = "送检")
    void startQc();

    @Transition(from = "PENDING_QC", to = "NORMAL", event = "COMPLETE_QC", description = "质检合格")
    void completeQc();

    @Transition(from = "PENDING_QC", to = "DEFECTIVE", event = "QC_FAIL", description = "质检不合格")
    void qcFail();

    @Transition(from = "NORMAL", to = "RESERVED", event = "RESERVE", description = "预留库存")
    void reserve();

    @Transition(from = "RESERVED", to = "NORMAL", event = "UNRESERVE", description = "释放预留")
    void unreserve();
}
