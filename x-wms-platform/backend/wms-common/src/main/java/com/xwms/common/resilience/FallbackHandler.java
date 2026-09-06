package com.xwms.common.resilience;

import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeException;
import com.alibaba.csp.sentinel.slots.block.flow.FlowException;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowException;

import com.xwms.common.core.Result;
import com.xwms.common.exception.BizException;
import com.xwms.common.exception.ErrorCode;

import lombok.extern.slf4j.Slf4j;

/**
 * 通用降级处理工具
 *
 * <p>提供统一的降级响应，避免每个Fallback方法重复写异常处理逻辑。
 *
 * <p>使用方式：
 *
 * <pre>
 * &#64;SentinelResource(value = "inventoryService", fallback = "inventoryFallback")
 * public Result&lt;Inventory&gt; getInventory(String sku) { ... }
 *
 * private Result&lt;Inventory&gt; inventoryFallback(String sku, BlockException e) {
 *     return FallbackHandler.handle("库存查询", e, () -> Result.fail("库存查询服务暂不可用"));
 * }
 * </pre>
 */
@Slf4j
public class FallbackHandler {

    private FallbackHandler() {}

    /**
     * 统一降级处理
     *
     * @param operation 操作名称（用于日志）
     * @param e 异常
     * @param defaultResult 默认降级结果
     * @return 降级结果
     */
    public static <T> Result<T> handle(String operation, Exception e, Result<T> defaultResult) {
        if (e instanceof DegradeException) {
            log.warn("[Fallback] {} 熔断降级触发，快速失败", operation);
            return Result.fail(ErrorCode.SERVICE_UNAVAILABLE.getCode(), "服务繁忙，请稍后重试");
        }
        if (e instanceof FlowException || e instanceof ParamFlowException) {
            log.warn("[Fallback] {} 触发限流", operation);
            return Result.fail(ErrorCode.RATE_LIMITED.getCode(), "请求过于频繁，请稍后重试");
        }
        if (e instanceof BlockException) {
            log.warn("[Fallback] {} Sentinel阻断: rule={}", operation, e.getClass().getSimpleName());
            return Result.fail(ErrorCode.SERVICE_UNAVAILABLE.getCode(), "服务繁忙，请稍后重试");
        }
        if (e instanceof java.util.concurrent.TimeoutException) {
            log.warn("[Fallback] {} 调用超时", operation);
            return Result.fail(ErrorCode.TIMEOUT.getCode(), "请求超时，请稍后重试");
        }
        if (e instanceof BizException bizException) {
            log.warn(
                    "[Fallback] {} 业务异常: code={}, msg={}",
                    operation,
                    bizException.getCode(),
                    bizException.getMessage());
            return Result.fail(bizException.getCode(), bizException.getMessage());
        }
        log.error("[Fallback] {} 未知异常: {}", operation, e.getMessage(), e);
        return defaultResult;
    }

    /** 简单降级（返回默认消息） */
    public static <T> Result<T> simple(String operation, Exception e) {
        return handle(
                operation,
                e,
                Result.fail(ErrorCode.SERVICE_UNAVAILABLE.getCode(), operation + "服务暂不可用，请稍后重试"));
    }

    /** 带默认值的降级 */
    public static <T> Result<T> withDefault(String operation, Exception e, T defaultValue) {
        Result<T> result = handle(operation, e, null);
        if (result != null && !result.isSuccess()) {
            return Result.success(defaultValue);
        }
        return result != null ? result : Result.success(defaultValue);
    }
}
