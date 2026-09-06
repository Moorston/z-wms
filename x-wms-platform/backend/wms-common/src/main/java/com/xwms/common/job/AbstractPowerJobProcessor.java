package com.xwms.common.job;

import java.util.Map;

import com.xwms.common.trace.util.TraceIdUtil;
import com.xwms.common.utils.JsonUtils;

import lombok.extern.slf4j.Slf4j;
import tech.powerjob.worker.core.processor.ProcessResult;
import tech.powerjob.worker.core.processor.TaskContext;
import tech.powerjob.worker.core.processor.sdk.BasicProcessor;

/**
 * PowerJob任务处理器基类 核心能力： 1. TraceId自动生成（定时任务无HTTP请求，需手动生成TraceId） 2. 统一日志格式（任务ID/任务名称/实例ID/参数） 3.
 * 异常统一处理（失败返回ProcessResult(false)） 4. 耗时统计 5. 货主上下文支持（从任务参数读取ownerCode）
 *
 * <p>使用方式：
 *
 * <pre>
 * &#64;Component
 * public class ExpireWarningProcessor extends AbstractPowerJobProcessor {
 *     &#64;Override
 *     protected ProcessResult doProcess(TaskContext context) {
 *         // 业务逻辑
 *         return success("处理完成");
 *     }
 * }
 * </pre>
 */
@Slf4j
public abstract class AbstractPowerJobProcessor implements BasicProcessor {

    @Override
    public ProcessResult process(TaskContext context) throws Exception {
        long startTime = System.currentTimeMillis();
        String taskName = getClass().getSimpleName();

        try {
            // 1. 生成TraceId（定时任务无HTTP请求）
            TraceIdUtil.initTraceId();

            // 2. 从任务参数中解析货主编码（支持多货主分片）
            String ownerCode = parseOwnerCode(context);
            if (ownerCode != null && !ownerCode.isEmpty()) {
                com.xwms.common.tenant.context.OwnerContext.set(ownerCode);
                log.info(
                        "[PowerJob] 任务开始: task={}, instanceId={}, jobId={}, owner={}, params={}",
                        taskName,
                        context.getInstanceId(),
                        context.getJobId(),
                        ownerCode,
                        context.getJobParams());
            } else {
                log.info(
                        "[PowerJob] 任务开始: task={}, instanceId={}, jobId={}, params={}",
                        taskName,
                        context.getInstanceId(),
                        context.getJobId(),
                        context.getJobParams());
            }

            // 3. 执行业务逻辑
            ProcessResult result = doProcess(context);

            long cost = System.currentTimeMillis() - startTime;
            log.info(
                    "[PowerJob] 任务完成: task={}, instanceId={}, success={}, cost={}ms, msg={}",
                    taskName,
                    context.getInstanceId(),
                    result.isSuccess(),
                    cost,
                    result.getMsg());
            return result;

        } catch (Exception e) {
            long cost = System.currentTimeMillis() - startTime;
            log.error(
                    "[PowerJob] 任务异常: task={}, instanceId={}, cost={}ms, error={}",
                    taskName,
                    context.getInstanceId(),
                    cost,
                    e.getMessage(),
                    e);
            return new ProcessResult(false, "任务执行异常: " + e.getMessage());
        } finally {
            // 4. 清理上下文
            TraceIdUtil.clear();
            com.xwms.common.tenant.context.OwnerContext.clear();
        }
    }

    /** 子类实现具体业务逻辑 */
    protected abstract ProcessResult doProcess(TaskContext context) throws Exception;

    /** 从任务参数中解析货主编码 参数格式支持： - JSON: {"ownerCode":"OWNER001"} - 纯文本: OWNER001 */
    protected String parseOwnerCode(TaskContext context) {
        String params = context.getJobParams();
        if (params == null || params.isEmpty()) {
            return null;
        }
        // 尝试JSON解析
        if (params.contains("ownerCode")) {
            try {
                Map<String, Object> json = JsonUtils.parseObject(params);
                return getString(json, "ownerCode");
            } catch (Exception e) {
                // 非JSON，忽略
            }
        }
        return null;
    }

    /** 成功结果 */
    protected ProcessResult success(String msg) {
        return new ProcessResult(true, msg);
    }

    /** 失败结果 */
    protected ProcessResult fail(String msg) {
        return new ProcessResult(false, msg);
    }

    /** 获取任务参数（JSON解析为Map） */
    protected Map<String, Object> getParams(TaskContext context) {
        String params = context.getJobParams();
        if (params == null || params.isEmpty()) {
            return Map.of();
        }
        try {
            return JsonUtils.parseObject(params);
        } catch (Exception e) {
            return Map.of();
        }
    }

    // ============================================================
    // 参数读取辅助方法（供子类使用，替代 FastJSON JSONObject 的 getString/getIntValue）
    // ============================================================

    protected static String getString(Map<String, Object> params, String key) {
        Object val = params.get(key);
        return val != null ? val.toString() : null;
    }

    protected static String getString(Map<String, Object> params, String key, String def) {
        Object val = params.get(key);
        return val != null ? val.toString() : def;
    }

    protected static int getInt(Map<String, Object> params, String key, int def) {
        Object val = params.get(key);
        if (val == null) return def;
        if (val instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(val.toString());
        } catch (Exception e) {
            return def;
        }
    }
}
