package com.xwms.core.statemachine;

import java.util.Set;

import org.springframework.stereotype.Component;

import com.xwms.common.statemachine.annotation.StateMachine;
import com.xwms.common.statemachine.annotation.Transition;
import com.xwms.core.equipment.enums.EquipmentStatus;

/**
 * 设备状态机
 *
 * <p>状态流转: IDLE(空闲) → IN_USE(使用中) → IDLE(释放) IDLE/IN_USE → MAINTENANCE(维护中) → IDLE(维护完成)
 * IDLE/IN_USE → FAULT(故障) → MAINTENANCE(维修) → IDLE IDLE → RETIRED(报废)
 */
@Component
@StateMachine(name = "equipmentStateMachine", description = "设备状态机")
public class EquipmentStateMachine {

    @Transition(from = "IDLE", to = "IN_USE", event = "ASSIGN", description = "分配使用")
    public void assign() {}

    @Transition(from = "IN_USE", to = "IDLE", event = "RELEASE", description = "释放归还")
    public void release() {}

    @Transition(from = "IDLE", to = "MAINTENANCE", event = "MAINTAIN", description = "开始维护")
    public void maintainIdle() {}

    @Transition(from = "IN_USE", to = "MAINTENANCE", event = "MAINTAIN", description = "开始维护")
    public void maintainInUse() {}

    @Transition(from = "MAINTENANCE", to = "IDLE", event = "MAINTAIN_DONE", description = "维护完成")
    public void maintainDone() {}

    @Transition(from = "IDLE", to = "FAULT", event = "FAULT", description = "故障上报")
    public void faultIdle() {}

    @Transition(from = "IN_USE", to = "FAULT", event = "FAULT", description = "故障上报")
    public void faultInUse() {}

    @Transition(from = "FAULT", to = "MAINTENANCE", event = "REPAIR", description = "开始维修")
    public void repair() {}

    @Transition(from = "IDLE", to = "RETIRED", event = "RETIRE", description = "报废")
    public void retire() {}

    public Set<String> getAllStates() {
        return Set.of(
                EquipmentStatus.IDLE.getCode(),
                EquipmentStatus.IN_USE.getCode(),
                EquipmentStatus.MAINTENANCE.getCode(),
                EquipmentStatus.FAULT.getCode(),
                EquipmentStatus.RETIRED.getCode());
    }
}
