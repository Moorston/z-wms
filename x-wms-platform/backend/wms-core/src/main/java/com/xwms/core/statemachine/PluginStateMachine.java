package com.xwms.core.statemachine;

import java.util.Set;

import org.springframework.stereotype.Component;

import com.xwms.common.statemachine.annotation.StateMachine;
import com.xwms.common.statemachine.annotation.Transition;

/**
 * 插件状态机
 *
 * <p>状态流转: INSTALLED(已安装) → ENABLED(已启用) ENABLED → DISABLED(已禁用) → ENABLED(重新启用) ENABLED/DISABLED →
 * ERROR(异常) → ENABLED(恢复)
 */
@Component
@StateMachine(name = "pluginStateMachine", description = "插件状态机")
public class PluginStateMachine {

    @Transition(from = "INSTALLED", to = "ENABLED", event = "ENABLE", description = "启用插件")
    public void enable() {}

    @Transition(from = "ENABLED", to = "DISABLED", event = "DISABLE", description = "禁用插件")
    public void disable() {}

    @Transition(from = "DISABLED", to = "ENABLED", event = "ENABLE", description = "重新启用")
    public void reEnable() {}

    @Transition(from = "ENABLED", to = "ERROR", event = "ERROR", description = "插件异常")
    public void error() {}

    @Transition(from = "ERROR", to = "ENABLED", event = "RECOVER", description = "恢复插件")
    public void recover() {}

    public Set<String> getAllStates() {
        return Set.of("INSTALLED", "ENABLED", "DISABLED", "ERROR");
    }
}
