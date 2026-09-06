package com.xwms.common.statemachine.engine;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.annotation.PostConstruct;

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import com.xwms.common.statemachine.annotation.StateMachine;
import com.xwms.common.statemachine.annotation.Transition;
import com.xwms.common.statemachine.listener.StateMachineEventPublisher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 轻量级状态机引擎
 *
 * <p>核心能力： 1. 扫描@StateMachine注解的类，注册状态流转规则 2. 执行状态流转（守卫检查 → 状态变更 → 动作执行 → 事件发布） 3. 支持动态注册状态机 4.
 * 状态流转日志
 *
 * <p>使用方式：
 *
 * <pre>
 * // 触发状态流转
 * stateMachineEngine.fire("inboundOrder", "CREATED", "START_RECEIVE", context);
 *
 * // 查询可触发的事件
 * List<String> events = stateMachineEngine.getAvailableEvents("inboundOrder", "CREATED");
 * </pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StateMachineEngine {

    private final ApplicationContext applicationContext;
    private final StateMachineEventPublisher eventPublisher;

    /** 状态机注册表：machineName → 状态机定义 */
    private final Map<String, MachineDefinition> registry = new ConcurrentHashMap<>();

    /** 状态流转规则：fromState + event → TransitionRule */
    private static class TransitionRule {
        String fromState;
        String toState;
        String event;
        String guardBean;
        String actionBean;
        String description;

        String getDescription() {
            return description;
        }
    }

    /** 状态机定义 */
    private static class MachineDefinition {
        String name;
        String description;
        String initialState;
        Map<String, Map<String, TransitionRule>> transitions = new HashMap<>();
        Set<String> states = new HashSet<>();
        Set<String> events = new HashSet<>();

        String getDescription() {
            return description;
        }
    }

    @PostConstruct
    public void init() {
        // 扫描所有@StateMachine注解的Bean
        Map<String, Object> beans = applicationContext.getBeansWithAnnotation(StateMachine.class);
        for (Object bean : beans.values()) {
            register(bean.getClass());
        }
        log.info("状态机引擎初始化完成，共注册 {} 个状态机", registry.size());
    }

    /** 注册状态机定义 */
    public void register(Class<?> stateMachineClass) {
        StateMachine sm = stateMachineClass.getAnnotation(StateMachine.class);
        if (sm == null) {
            throw new StateMachineException(
                    "类 " + stateMachineClass.getName() + " 没有@StateMachine注解");
        }

        MachineDefinition def = new MachineDefinition();
        def.name = sm.name();
        def.description = sm.description();
        def.initialState = sm.initialState();

        // 扫描所有@Transition注解的方法
        for (Method method : stateMachineClass.getMethods()) {
            Transition t = method.getAnnotation(Transition.class);
            if (t == null) continue;

            TransitionRule rule = new TransitionRule();
            rule.fromState = t.from();
            rule.toState = t.to();
            rule.event = t.event();
            rule.guardBean = t.guard();
            rule.actionBean = t.action();
            rule.description = t.description();

            def.transitions
                    .computeIfAbsent(rule.fromState, k -> new HashMap<>())
                    .put(rule.event, rule);
            def.states.add(rule.fromState);
            def.states.add(rule.toState);
            def.events.add(rule.event);
        }

        registry.put(def.name, def);
        log.info(
                "注册状态机: {} ({}状态, {}事件, {}流转)",
                def.name,
                def.states.size(),
                def.events.size(),
                def.transitions.values().stream().mapToInt(Map::size).sum());
    }

    /**
     * 触发状态流转
     *
     * @param machineName 状态机名称
     * @param currentState 当前状态
     * @param event 触发事件
     * @param context 上下文参数
     * @return 目标状态
     */
    public String fire(
            String machineName, String currentState, String event, Map<String, Object> context) {
        MachineDefinition def = getMachine(machineName);

        // 查找流转规则
        Map<String, TransitionRule> fromTransitions = def.transitions.get(currentState);
        if (fromTransitions == null) {
            throw new StateMachineException(machineName, currentState, event, "当前状态没有可触发的事件");
        }
        TransitionRule rule = fromTransitions.get(event);
        if (rule == null) {
            throw new StateMachineException(
                    machineName,
                    currentState,
                    event,
                    "事件不允许在当前状态触发，可用事件: " + fromTransitions.keySet());
        }

        // 执行守卫条件
        if (rule.guardBean != null && !rule.guardBean.isEmpty()) {
            StateGuard guard = applicationContext.getBean(rule.guardBean, StateGuard.class);
            if (!guard.evaluate(currentState, rule.toState, event, context)) {
                throw new StateMachineException(
                        machineName, currentState, event, "守卫条件不满足: " + rule.guardBean);
            }
        }

        // 执行动作
        if (rule.actionBean != null && !rule.actionBean.isEmpty()) {
            StateAction action = applicationContext.getBean(rule.actionBean, StateAction.class);
            action.execute(currentState, rule.toState, event, context);
        }

        // 发布状态变更事件
        eventPublisher.publish(machineName, currentState, rule.toState, event, context);

        log.info("[状态机] {}: {} --[{}]--> {}", machineName, currentState, event, rule.toState);
        return rule.toState;
    }

    /** 触发状态流转（无上下文） */
    public String fire(String machineName, String currentState, String event) {
        return fire(machineName, currentState, event, new HashMap<>());
    }

    /** 查询当前状态可触发的事件列表 */
    public List<String> getAvailableEvents(String machineName, String currentState) {
        MachineDefinition def = getMachine(machineName);
        Map<String, TransitionRule> transitions = def.transitions.get(currentState);
        if (transitions == null) {
            return Collections.emptyList();
        }
        return new ArrayList<>(transitions.keySet());
    }

    /** 查询状态机所有状态 */
    public Set<String> getStates(String machineName) {
        return getMachine(machineName).states;
    }

    /** 查询状态机所有事件 */
    public Set<String> getEvents(String machineName) {
        return getMachine(machineName).events;
    }

    /** 获取初始状态 */
    public String getInitialState(String machineName) {
        return getMachine(machineName).initialState;
    }

    /** 检查状态流转是否合法 */
    public boolean canFire(String machineName, String currentState, String event) {
        MachineDefinition def = registry.get(machineName);
        if (def == null) return false;
        Map<String, TransitionRule> transitions = def.transitions.get(currentState);
        return transitions != null && transitions.containsKey(event);
    }

    /** 获取所有已注册的状态机名称 */
    public Set<String> getRegisteredMachines() {
        return registry.keySet();
    }

    private MachineDefinition getMachine(String machineName) {
        MachineDefinition def = registry.get(machineName);
        if (def == null) {
            throw new StateMachineException("状态机未注册: " + machineName);
        }
        return def;
    }
}
