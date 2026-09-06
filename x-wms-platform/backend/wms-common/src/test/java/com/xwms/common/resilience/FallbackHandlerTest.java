package com.xwms.common.resilience;

import org.junit.jupiter.api.Test;

import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeException;
import com.alibaba.csp.sentinel.slots.block.flow.FlowException;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowException;

import com.xwms.common.core.Result;
import com.xwms.common.exception.BizException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * FallbackHandler 回归测试
 *
 * <p>覆盖 R2: Resilience4j→Sentinel 升级。验证 {@code FallbackHandler.handle()}
 * 将各异常类型映射到正确的 {@link Result} 状态码。
 */
class FallbackHandlerTest {

    private static final String OPERATION = "测试操作";

    private static final BlockException BLOCK_EXCEPTION =
            new BlockException("testBlock") {};

    // ============================================================
    // T2.1: DegradeException → 503 SERVICE_UNAVAILABLE
    // ============================================================

    @Test
    void handle_degradeException_returnsServiceUnavailable() {
        Exception e = new DegradeException("test");
        Result<Void> result = FallbackHandler.handle(OPERATION, e, null);

        assertEquals(503, result.getCode());
        assertTrue(result.getMessage().contains("服务繁忙"));
    }

    // ============================================================
    // T2.2: FlowException → 429 RATE_LIMITED
    // ============================================================

    @Test
    void handle_flowException_returnsRateLimited() {
        Exception e = new FlowException("testFlow");
        Result<Void> result = FallbackHandler.handle(OPERATION, e, null);

        assertEquals(429, result.getCode());
        assertTrue(result.getMessage().contains("请求过于频繁"));
    }

    // ============================================================
    // T2.3: ParamFlowException → 429 RATE_LIMITED
    // ============================================================

    @Test
    void handle_paramFlowException_returnsRateLimited() {
        Exception e = new ParamFlowException("testParam", "testResource");
        Result<Void> result = FallbackHandler.handle(OPERATION, e, null);

        assertEquals(429, result.getCode());
    }

    // ============================================================
    // T2.4: BlockException → 503 SERVICE_UNAVAILABLE
    // ============================================================

    @Test
    void handle_blockException_returnsServiceUnavailable() {
        Result<Void> result = FallbackHandler.handle(OPERATION, BLOCK_EXCEPTION, null);

        assertEquals(503, result.getCode());
    }

    // ============================================================
    // T2.5: TimeoutException → 408 TIMEOUT
    // ============================================================

    @Test
    void handle_timeoutException_returnsTimeout() {
        Exception e = new java.util.concurrent.TimeoutException();
        Result<Void> result = FallbackHandler.handle(OPERATION, e, null);

        assertEquals(408, result.getCode());
    }

    // ============================================================
    // T2.6: BizException → 动态 code/message
    // ============================================================

    @Test
    void handle_bizException_returnsDynamicCodeAndMessage() {
        BizException e = new BizException(1001, "库存不足");
        Result<Void> result = FallbackHandler.handle(OPERATION, e, null);

        assertEquals(1001, result.getCode());
        assertEquals("库存不足", result.getMessage());
    }

    // ============================================================
    // T2.7: 未知异常 → 返回 defaultResult
    // ============================================================

    @Test
    void handle_unknownException_returnsDefaultResult() {
        Result<Void> defaultResult =
                Result.fail(CUSTOM_ERROR_CODE, "自定义降级结果");
        Exception e = new RuntimeException("unexpected");
        Result<Void> result = FallbackHandler.handle(OPERATION, e, defaultResult);

        assertSame(defaultResult, result);
    }

    // ============================================================
    // 辅助：自定义错误码（避免依赖生产枚举中不存在的项）
    // ============================================================

    private static final Integer CUSTOM_ERROR_CODE = 999;
}
