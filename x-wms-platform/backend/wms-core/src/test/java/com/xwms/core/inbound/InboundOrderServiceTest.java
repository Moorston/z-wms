package com.xwms.core.inbound;

import com.xwms.common.exception.BizException;
import com.xwms.core.inbound.entity.InboundOrder;
import com.xwms.core.inbound.mapper.InboundOrderMapper;
import com.xwms.core.inbound.service.InboundOrderService;
import com.xwms.core.support.BaseTest;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 入库单服务单元测试
 * 核心测试：创建/收货/上架/取消/状态流转
 */
class InboundOrderServiceTest extends BaseTest {

    @Mock
    private InboundOrderMapper inboundOrderMapper;

    @InjectMocks
    private InboundOrderService inboundOrderService;

    @Test
    void testCreate_success()
    {
        when(inboundOrderMapper.selectOne(any())).thenReturn(null);
        doReturn(1).when(inboundOrderMapper).insert(any(InboundOrder.class));

        InboundOrder order = new InboundOrder();
        order.setInboundNo("IN20260818001");
        order.setInboundType("PURCHASE");
        InboundOrder result = inboundOrderService.create(order);

        assertEquals("CREATED", result.getStatus());
        verify(inboundOrderMapper, times(1)).insert(any(InboundOrder.class));
    }

    @Test
    void testCreate_duplicate()
    {
        InboundOrder exist = new InboundOrder();
        exist.setInboundNo("IN20260818001");
        when(inboundOrderMapper.selectOne(any())).thenReturn(exist);

        InboundOrder order = new InboundOrder();
        order.setInboundNo("IN20260818001");
        assertThrows(BizException.class, () -> inboundOrderService.create(order));
    }

    @Test
    void testConfirmReceive_success()
    {
        InboundOrder order = new InboundOrder();
        order.setInboundNo("IN001");
        order.setStatus("CREATED");
        when(inboundOrderMapper.selectOne(any())).thenReturn(order);
        doReturn(1).when(inboundOrderMapper).updateById(any(InboundOrder.class));

        inboundOrderService.confirmReceive("IN001", new BigDecimal("100"));

        assertEquals("RECEIVED", order.getStatus());
        assertEquals(new BigDecimal("100"), order.getReceivedQty());
        assertNotNull(order.getReceiveTime());
    }

    @Test
    void testConfirmReceive_invalidStatus()
    {
        InboundOrder order = new InboundOrder();
        order.setInboundNo("IN001");
        order.setStatus("COMPLETED");
        when(inboundOrderMapper.selectOne(any())).thenReturn(order);

        assertThrows(BizException.class,
                () -> inboundOrderService.confirmReceive("IN001", new BigDecimal("100")));
    }

    @Test
    void testCompletePutaway()
    {
        InboundOrder order = new InboundOrder();
        order.setInboundNo("IN001");
        order.setStatus("RECEIVED");
        when(inboundOrderMapper.selectOne(any())).thenReturn(order);
        doReturn(1).when(inboundOrderMapper).updateById(any(InboundOrder.class));

        inboundOrderService.completePutaway("IN001", new BigDecimal("100"));

        assertEquals("COMPLETED", order.getStatus());
    }

    @Test
    void testCancel_success()
    {
        InboundOrder order = new InboundOrder();
        order.setInboundNo("IN001");
        order.setStatus("CREATED");
        when(inboundOrderMapper.selectOne(any())).thenReturn(order);
        doReturn(1).when(inboundOrderMapper).updateById(any(InboundOrder.class));

        inboundOrderService.cancel("IN001");
        assertEquals("CANCELLED", order.getStatus());
    }

    @Test
    void testCancel_invalidStatus()
    {
        InboundOrder order = new InboundOrder();
        order.setInboundNo("IN001");
        order.setStatus("RECEIVED");
        when(inboundOrderMapper.selectOne(any())).thenReturn(order);

        assertThrows(BizException.class, () -> inboundOrderService.cancel("IN001"));
    }
}
