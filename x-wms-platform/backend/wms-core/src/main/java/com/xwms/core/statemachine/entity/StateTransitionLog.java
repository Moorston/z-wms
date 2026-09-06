package com.xwms.core.statemachine.entity;

import com.baomidou.mybatisplus.annotation.TableName;

import com.xwms.common.core.BaseEntity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/** 状态流转日志实体 记录每次状态变更，用于审计和追溯 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_state_transition_log")
public class StateTransitionLog extends BaseEntity {

    /** 状态机名称 */
    private String machineName;

    /** 业务ID（如入库单号、出库单号） */
    private String bizId;

    /** 源状态 */
    private String fromState;

    /** 目标状态 */
    private String toState;

    /** 触发事件 */
    private String event;

    /** 操作人 */
    private String operator;

    /** 备注 */
    private String remark;

    /** 上下文参数（JSON） */
    private String context;
}
