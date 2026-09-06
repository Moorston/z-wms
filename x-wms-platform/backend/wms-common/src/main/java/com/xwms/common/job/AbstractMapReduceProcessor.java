package com.xwms.common.job;

import java.util.List;
import java.util.Map;

import com.xwms.common.trace.util.TraceIdUtil;
import com.xwms.common.utils.JsonUtils;

import lombok.extern.slf4j.Slf4j;
import tech.powerjob.worker.core.processor.ProcessResult;
import tech.powerjob.worker.core.processor.TaskContext;
import tech.powerjob.worker.core.processor.sdk.MapReduceProcessor;
import tech.powerjob.worker.log.OmsLogger;

/**
 * PowerJob MapReduce任务处理器基类 适用于大数据量分片处理，如： - 库存对账（按仓库/货主分片） - 数据归档（按表/时间分片） - 仓储费计算（按货主分片）
 *
 * <p>工作流程： 1. map() — 主节点将任务拆分成多个子任务 2. process() — 各Worker节点并行处理子任务 3. reduce() — 汇总所有子任务结果
 *
 * <p>使用方式：
 *
 * <pre>
 * &#64;Component
 * public class InventoryCheckProcessor extends AbstractMapReduceProcessor<InventoryCheckTask, String> {
 *     &#64;Override
 *     protected List&lt;InventoryCheckTask&gt; splitTasks(TaskContext context) {
 *         // 按仓库拆分任务
 *         return warehouseService.listAll().stream()
 *             .map(w -&gt; new InventoryCheckTask(w.getWarehouseCode()))
 *             .collect(Collectors.toList());
 *     }
 *     &#64;Override
 *     protected String processSubTask(InventoryCheckTask task) {
 *         // 单个仓库对账
 *         return inventoryService.check(task.getWarehouseCode());
 *     }
 *     &#64;Override
 *     protected ProcessResult reduce(List&lt;String&gt; results) {
 *         // 汇总结果
 *         return success("对账完成，共" + results.size() + "个仓库");
 *     }
 * }
 * </pre>
 */
@Slf4j
public abstract class AbstractMapReduceProcessor<T, R> implements MapReduceProcessor {

    @Override
    public ProcessResult process(TaskContext context) throws Exception {
        // 根任务：拆分
        if (isRootTask(context)) {
            return doMap(context);
        }
        // 子任务：处理
        return doProcessSubTask(context);
    }

    /** Map阶段：主节点拆分任务 */
    private ProcessResult doMap(TaskContext context) {
        long startTime = System.currentTimeMillis();
        TraceIdUtil.initTraceId();
        try {
            log.info(
                    "[PowerJob-MR] Map阶段开始: task={}, instanceId={}",
                    getClass().getSimpleName(),
                    context.getInstanceId());

            List<T> subTasks = splitTasks(context);
            if (subTasks == null || subTasks.isEmpty()) {
                return success("无待处理任务");
            }

            // 分发子任务
            map(subTasks, getClass().getSimpleName());

            log.info(
                    "[PowerJob-MR] Map阶段完成: task={}, 子任务数={}, cost={}ms",
                    getClass().getSimpleName(),
                    subTasks.size(),
                    System.currentTimeMillis() - startTime);
            return new ProcessResult(true, "已分发" + subTasks.size() + "个子任务");
        } catch (Exception e) {
            log.error(
                    "[PowerJob-MR] Map阶段异常: task={}, error={}",
                    getClass().getSimpleName(),
                    e.getMessage(),
                    e);
            return fail("Map阶段异常: " + e.getMessage());
        } finally {
            TraceIdUtil.clear();
        }
    }

    /** 子任务处理 */
    @SuppressWarnings("unchecked")
    private ProcessResult doProcessSubTask(TaskContext context) {
        long startTime = System.currentTimeMillis();
        TraceIdUtil.initTraceId();
        try {
            T subTask = (T) context.getSubTask();
            log.info(
                    "[PowerJob-MR] 子任务开始: task={}, subTask={}",
                    getClass().getSimpleName(),
                    subTask);

            R result = processSubTask(subTask);

            log.info(
                    "[PowerJob-MR] 子任务完成: task={}, cost={}ms, result={}",
                    getClass().getSimpleName(),
                    System.currentTimeMillis() - startTime,
                    result);
            return new ProcessResult(true, String.valueOf(result));
        } catch (Exception e) {
            log.error(
                    "[PowerJob-MR] 子任务异常: task={}, error={}",
                    getClass().getSimpleName(),
                    e.getMessage(),
                    e);
            return fail("子任务异常: " + e.getMessage());
        } finally {
            TraceIdUtil.clear();
        }
    }

    /**
     * 子类实现：拆分任务
     *
     * @return 子任务列表
     */
    protected abstract List<T> splitTasks(TaskContext context);

    /**
     * 子类实现：处理单个子任务
     *
     * @return 子任务处理结果
     */
    protected abstract R processSubTask(T subTask);

    /** 是否为根任务（Map阶段） */
    private boolean isRootTask(TaskContext context) {
        return context.getSubTask() == null;
    }

    /** 成功结果 */
    protected ProcessResult success(String msg) {
        return new ProcessResult(true, msg);
    }

    /** 失败结果 */
    protected ProcessResult fail(String msg) {
        return new ProcessResult(false, msg);
    }

    /** 获取PowerJob日志器（可在任务中输出在线日志） */
    protected OmsLogger getLogger(TaskContext context) {
        return context.getOmsLogger();
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
    // 参数读取辅助方法（供子类使用）
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
