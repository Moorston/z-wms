package com.xwms.core.outbound.service;

import org.junit.jupiter.api.Test;

import com.alibaba.csp.sentinel.slots.block.BlockException;

import com.xwms.core.outbound.entity.OutboundOrder;

import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * OutboundOrderService fallback 回归测试
 *
 * <p>覆盖 R2: @SentinelResource fallback。验证
 * {@code createFallback()} 返回 null（降级时不创建出库单）。
 */
class OutboundOrderServiceFallbackTest {

    private static final BlockException BLOCK_EXCEPTION =
            new BlockException("testFallback") {};

    @Test
    void createFallback_returnsNull() {
        // Given
        OutboundOrder order = new OutboundOrder();
        order.setOutboundNo("OUT-20260904-001");

        // When & Then
        OutboundOrderService service = createService();
        OutboundOrder result = service.createFallback(order, BLOCK_EXCEPTION);

        assertNull(result, "createFallback 应返回 null（降级时不创建出库单）");
    }

    /** 创建 OutboundOrderService 实例（fallback 不依赖任何注入字段） */
    private OutboundOrderService createService() {
        // createFallback 是 public 方法，仅调用 FallbackHandler.simple()，
        // 不访问 outboundOrderMapper/inventoryService 等字段，可直接实例化
        return new OutboundOrderService(null, null, null, null);
    }
}
