package com.xwms.core.statemachine.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.xwms.core.statemachine.entity.StateTransitionLog;
import com.xwms.core.statemachine.mapper.StateTransitionLogMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 状态流转日志服务 记录和查询状态变更历史 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StateTransitionLogService {

    private final StateTransitionLogMapper logMapper;
    private final ObjectMapper objectMapper;

    /** 记录状态流转 */
    public void record(
            String machineName,
            String bizId,
            String fromState,
            String toState,
            String event,
            String operator,
            Map<String, Object> context) {
        try {
            StateTransitionLog logEntry = new StateTransitionLog();
            logEntry.setMachineName(machineName);
            logEntry.setBizId(bizId);
            logEntry.setFromState(fromState);
            logEntry.setToState(toState);
            logEntry.setEvent(event);
            logEntry.setOperator(operator);
            if (context != null) {
                logEntry.setContext(objectMapper.writeValueAsString(context));
            }
            logMapper.insert(logEntry);
        } catch (JsonProcessingException e) {
            log.warn("状态流转日志序列化失败: {}", e.getMessage());
        } catch (Exception e) {
            // 日志记录失败不影响主流程
            log.warn("状态流转日志记录失败: {}", e.getMessage());
        }
    }

    /** 查询业务单据的状态流转历史 */
    public List<StateTransitionLog> getHistory(String machineName, String bizId) {
        return logMapper.selectList(
                new LambdaQueryWrapper<StateTransitionLog>()
                        .eq(StateTransitionLog::getMachineName, machineName)
                        .eq(StateTransitionLog::getBizId, bizId)
                        .orderByAsc(StateTransitionLog::getCreatedAt));
    }
}
