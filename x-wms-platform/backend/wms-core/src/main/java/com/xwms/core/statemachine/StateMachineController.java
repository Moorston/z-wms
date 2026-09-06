package com.xwms.core.statemachine;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.web.bind.annotation.*;

import com.xwms.common.core.Result;
import com.xwms.common.statemachine.engine.StateMachineEngine;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/** 状态机Controller 提供状态机定义查询和可用事件查询 */
@Tag(name = "状态机管理", description = "查询状态机定义、可用事件、触发流转")
@RestController
@RequestMapping("/statemachine")
@RequiredArgsConstructor
public class StateMachineController {

    private final StateMachineEngine engine;

    @Operation(summary = "查询所有已注册的状态机")
    @GetMapping("/machines")
    public Result<Set<String>> listMachines() {
        return Result.success(engine.getRegisteredMachines());
    }

    @Operation(summary = "查询状态机所有状态")
    @GetMapping("/{machineName}/states")
    public Result<Set<String>> listStates(@PathVariable String machineName) {
        return Result.success(engine.getStates(machineName));
    }

    @Operation(summary = "查询状态机所有事件")
    @GetMapping("/{machineName}/events")
    public Result<Set<String>> listEvents(@PathVariable String machineName) {
        return Result.success(engine.getEvents(machineName));
    }

    @Operation(summary = "查询当前状态可触发的事件")
    @GetMapping("/{machineName}/events/{currentState}")
    public Result<List<String>> getAvailableEvents(
            @PathVariable String machineName, @PathVariable String currentState) {
        return Result.success(engine.getAvailableEvents(machineName, currentState));
    }

    @Operation(summary = "检查状态流转是否合法")
    @GetMapping("/{machineName}/can-fire/{currentState}/{event}")
    public Result<Boolean> canFire(
            @PathVariable String machineName,
            @PathVariable String currentState,
            @PathVariable String event) {
        return Result.success(engine.canFire(machineName, currentState, event));
    }

    @Operation(summary = "触发状态流转（测试用）")
    @PostMapping("/{machineName}/fire/{currentState}/{event}")
    public Result<String> fire(
            @PathVariable String machineName,
            @PathVariable String currentState,
            @PathVariable String event,
            @RequestBody(required = false) Map<String, Object> context) {
        String newState =
                engine.fire(
                        machineName,
                        currentState,
                        event,
                        context != null ? context : new HashMap<>());
        return Result.success(newState);
    }
}
