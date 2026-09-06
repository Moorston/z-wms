package com.xwms.base.rule.engine;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.xwms.base.rule.entity.RuleDefinition;
import com.xwms.base.rule.entity.RuleExecLog;
import com.xwms.base.rule.entity.RuleParam;
import com.xwms.base.rule.mapper.RuleDefinitionMapper;
import com.xwms.base.rule.mapper.RuleExecLogMapper;
import com.xwms.base.rule.mapper.RuleParamMapper;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 规则引擎核心 支持: 规则加载/规则执行/规则缓存/执行日志 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RuleEngine {

    private final RuleDefinitionMapper ruleDefinitionMapper;
    private final RuleParamMapper ruleParamMapper;
    private final RuleExecLogMapper ruleExecLogMapper;
    private final ObjectMapper objectMapper;

    /** 规则缓存: ruleType -> List<RuleDefinition> */
    private final Map<String, List<RuleDefinition>> ruleCache = new ConcurrentHashMap<>();

    /** 规则参数缓存: ruleCode -> List<RuleParam> */
    private final Map<String, List<RuleParam>> paramCache = new ConcurrentHashMap<>();

    private static final AtomicInteger EXEC_SEQ = new AtomicInteger(0);
    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /**
     * 执行规则
     *
     * @param ruleType 规则类型
     * @param input 输入数据
     * @return 执行结果
     */
    public RuleResult execute(String ruleType, Map<String, Object> input) {
        long startTime = System.currentTimeMillis();
        String execNo = generateExecNo();

        try {
            // 1. 加载规则（按优先级排序）
            List<RuleDefinition> rules = getRulesByType(ruleType);
            if (rules.isEmpty()) {
                log.info("无可用规则: type={}", ruleType);
                return RuleResult.skip("无可用规则");
            }

            // 2. 按优先级依次执行
            Map<String, Object> context = new HashMap<>(input);
            RuleResult finalResult = null;

            for (RuleDefinition rule : rules) {
                RuleResult result = executeSingleRule(rule, context);
                if (result.isSuccess()) {
                    context.putAll(result.getOutput());
                    finalResult = result;
                    // 如果规则标记为终止，则不再执行后续规则
                    if (result.isTerminate()) {
                        break;
                    }
                }
            }

            long duration = System.currentTimeMillis() - startTime;
            String inputJson = objectMapper.writeValueAsString(input);
            String outputJson =
                    finalResult != null
                            ? objectMapper.writeValueAsString(finalResult.getOutput())
                            : "{}";

            // 3. 记录执行日志
            saveExecLog(
                    execNo,
                    ruleType,
                    null,
                    null,
                    inputJson,
                    outputJson,
                    finalResult != null ? "SUCCESS" : "SKIP",
                    null,
                    duration);

            return finalResult != null ? finalResult : RuleResult.skip("所有规则未命中");

        } catch (Exception e) {
            log.error("规则执行异常: type={}, error={}", ruleType, e.getMessage(), e);
            long duration = System.currentTimeMillis() - startTime;
            saveExecLog(execNo, ruleType, null, null, "{}", "{}", "FAIL", e.getMessage(), duration);
            return RuleResult.fail(e.getMessage());
        }
    }

    /** 执行单条规则 */
    private RuleResult executeSingleRule(RuleDefinition rule, Map<String, Object> context) {
        try {
            // 加载规则参数
            List<RuleParam> params = getRuleParams(rule.getRuleCode());
            Map<String, Object> paramMap = new HashMap<>();
            for (RuleParam param : params) {
                paramMap.put(
                        param.getParamCode(),
                        param.getParamValue() != null
                                ? param.getParamValue()
                                : param.getDefaultValue());
            }
            context.put("ruleParams", paramMap);

            // 执行规则脚本（简化实现，实际可集成Groovy/JavaScript引擎）
            // TODO: 集成脚本引擎执行 rule.getRuleScript()
            Map<String, Object> output = new HashMap<>();
            output.put("ruleCode", rule.getRuleCode());
            output.put("ruleName", rule.getRuleName());
            output.put("executed", true);

            return RuleResult.success(output);

        } catch (Exception e) {
            log.error("单条规则执行失败: rule={}, error={}", rule.getRuleCode(), e.getMessage());
            return RuleResult.fail(e.getMessage());
        }
    }

    /** 按类型获取规则（带缓存） */
    public List<RuleDefinition> getRulesByType(String ruleType) {
        return ruleCache.computeIfAbsent(
                ruleType, k -> ruleDefinitionMapper.selectByType(ruleType));
    }

    /** 获取规则参数（带缓存） */
    public List<RuleParam> getRuleParams(String ruleCode) {
        return paramCache.computeIfAbsent(
                ruleCode, k -> ruleParamMapper.selectByRuleCode(ruleCode));
    }

    /** 刷新规则缓存 */
    public void refreshCache(String ruleType) {
        if (ruleType != null) {
            ruleCache.remove(ruleType);
            log.info("刷新规则缓存: type={}", ruleType);
        } else {
            ruleCache.clear();
            paramCache.clear();
            log.info("刷新全部规则缓存");
        }
    }

    /** 保存执行日志 */
    private void saveExecLog(
            String execNo,
            String ruleType,
            String ruleCode,
            String bizNo,
            String inputData,
            String outputData,
            String result,
            String errorMsg,
            long duration) {
        try {
            RuleExecLog log = new RuleExecLog();
            log.setExecNo(execNo);
            log.setRuleCode(ruleCode);
            log.setRuleType(ruleType);
            log.setBizNo(bizNo);
            log.setInputData(inputData);
            log.setOutputData(outputData);
            log.setExecResult(result);
            log.setErrorMsg(errorMsg);
            log.setExecDuration(duration);
            log.setExecTime(LocalDateTime.now());
            ruleExecLogMapper.insert(log);
        } catch (Exception e) {
            // 日志记录失败不影响主流程
        }
    }

    private String generateExecNo() {
        return "RULE"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", EXEC_SEQ.incrementAndGet() % 1000);
    }

    /** 规则执行结果 */
    @Data
    public static class RuleResult {
        private boolean success;
        private boolean skip;
        private boolean terminate;
        private String message;
        private Map<String, Object> output;

        public static RuleResult success(Map<String, Object> output) {
            RuleResult r = new RuleResult();
            r.success = true;
            r.output = output;
            return r;
        }

        public static RuleResult fail(String message) {
            RuleResult r = new RuleResult();
            r.success = false;
            r.message = message;
            r.output = new HashMap<>();
            return r;
        }

        public static RuleResult skip(String message) {
            RuleResult r = new RuleResult();
            r.skip = true;
            r.message = message;
            r.output = new HashMap<>();
            return r;
        }
    }
}
