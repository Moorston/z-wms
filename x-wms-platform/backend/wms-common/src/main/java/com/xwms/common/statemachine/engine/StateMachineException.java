package com.xwms.common.statemachine.engine;

/** 状态机异常 */
public class StateMachineException extends RuntimeException {

    private final String stateMachine;
    private final String currentState;
    private final String event;

    public StateMachineException(String message) {
        super(message);
        this.stateMachine = null;
        this.currentState = null;
        this.event = null;
    }

    public StateMachineException(
            String stateMachine, String currentState, String event, String message) {
        super(
                String.format(
                        "[%s] 状态[%s] 触发事件[%s] 失败: %s", stateMachine, currentState, event, message));
        this.stateMachine = stateMachine;
        this.currentState = currentState;
        this.event = event;
    }

    public String getStateMachine() {
        return stateMachine;
    }

    public String getCurrentState() {
        return currentState;
    }

    public String getEvent() {
        return event;
    }
}
