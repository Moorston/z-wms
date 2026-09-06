package com.xwms.core.statemachine;

import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.xwms.common.statemachine.annotation.StateMachine;
import com.xwms.common.statemachine.annotation.Transition;
import com.xwms.core.qc.enums.QcOrderStatus;

/**
 * 质检单状态机
 *
 * <p>状态流转: PENDING(待检) → INSPECTING(检验中) → PASSED(合格) → FAILED(不合格) → CONCESSION_PENDING(让步待审) →
 * CONCESSION_APPROVED(让步通过) → FAILED(让步拒绝) FAILED → DISPOSED(已处理)
 */
@Component
@StateMachine(name = "qcOrderStateMachine", description = "质检单状态机")
public class QcOrderStateMachine {

    /** 定义所有合法状态流转 */
    @Transition(from = "PENDING", to = "INSPECTING", event = "START_QC", description = "开始质检")
    public void startQc(Map<String, Object> context) {
        // 设置检验员、开始时间
    }

    @Transition(from = "INSPECTING", to = "PASSED", event = "QC_PASS", description = "质检合格")
    public void qcPass(Map<String, Object> context) {
        // 触发上架流程
    }

    @Transition(from = "INSPECTING", to = "FAILED", event = "QC_FAIL", description = "质检不合格")
    public void qcFail(Map<String, Object> context) {
        // 创建不合格品记录, 冻结库存
    }

    @Transition(
            from = "FAILED",
            to = "CONCESSION_PENDING",
            event = "APPLY_CONCESSION",
            description = "申请让步接收")
    public void applyConcession(Map<String, Object> context) {}

    @Transition(
            from = "CONCESSION_PENDING",
            to = "CONCESSION_APPROVED",
            event = "APPROVE_CONCESSION",
            description = "让步接收通过")
    public void approveConcession(Map<String, Object> context) {
        // 不合格品转为可用, 触发上架
    }

    @Transition(
            from = "CONCESSION_PENDING",
            to = "FAILED",
            event = "REJECT_CONCESSION",
            description = "让步接收拒绝")
    public void rejectConcession(Map<String, Object> context) {}

    @Transition(from = "FAILED", to = "DISPOSED", event = "DISPOSE", description = "不合格品处理完成")
    public void dispose(Map<String, Object> context) {}

    /**
     * 守卫: 只有待检状态才能开始 NOTE: 框架的 StateGuard 是接口而非注解，正确做法是实现该接口并注册为 Bean，
     * 通过 @Transition(guard="beanName") 引用。此处先保留守卫逻辑，待后续按框架规约重构。
     */
    public boolean guardPending(String event) {
        return "START_QC".equals(event);
    }

    /** 获取所有状态 */
    public Set<String> getAllStates() {
        return Set.of(
                QcOrderStatus.PENDING.getCode(),
                QcOrderStatus.INSPECTING.getCode(),
                QcOrderStatus.PASSED.getCode(),
                QcOrderStatus.FAILED.getCode(),
                QcOrderStatus.CONCESSION_PENDING.getCode(),
                QcOrderStatus.CONCESSION_APPROVED.getCode(),
                QcOrderStatus.CONCESSION_REJECTED.getCode(),
                QcOrderStatus.DISPOSED.getCode());
    }
}
