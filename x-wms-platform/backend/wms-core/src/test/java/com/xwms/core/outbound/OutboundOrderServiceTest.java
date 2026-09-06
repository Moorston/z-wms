package com.xwms.core.outbound;

import com.xwms.common.exception.BizException;
import com.xwms.common.exception.ErrorCode;
import com.xwms.common.statemachine.engine.StateMachineEngine;
import com.xwms.core.inventory.entity.Inventory;
import com.xwms.core.inventory.service.InventoryService;
import com.xwms.core.outbound.entity.OutboundOrder;
import com.xwms.core.outbound.mapper.OutboundOrderMapper;
import com.xwms.core.outbound.service.OutboundOrderService;
import com.xwms.core.statemachine.service.StateTransitionLogService;
import com.xwms.core.support.BaseTest;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 出库单服务单元测试
 * 核心测试：创建/分配/拣货/发运(扣减)/取消
 */
class OutboundOrderServiceTest extends BaseTest {

    @Mock
    private OutboundOrderMapper outboundOrderMapper;
    @Mock
    private InventoryService inventoryService;
    @Mock
    private StateMachineEngine stateMachineEngine;
    @Mock
    private StateTransitionLogService transitionLogService;

    @InjectMocks
    private OutboundOrderService outboundOrderService;

    @Test
    void testCreate_success()
    {
        when(outboundOrderMapper.selectOne(any())).thenReturn(null);
        doReturn(1).when(outboundOrderMapper).insert(any(OutboundOrder.class));

        OutboundOrder order = new OutboundOrder();
        order.setOutboundNo("OUT20260818001");
        OutboundOrder result = outboundOrderService.create(order);

        assertEquals("CREATED", result.getStatus());
    }

    @Test
    void testCreate_duplicate()
    {
        OutboundOrder exist = new OutboundOrder();
        when(outboundOrderMapper.selectOne(any())).thenReturn(exist);
        OutboundOrder order = new OutboundOrder();
        order.setOutboundNo("OUT001");
        assertThrows(BizException.class, () -> outboundOrderService.create(order));
    }

    @Test
    void testAllocate_success()
    {
        OutboundOrder order = new OutboundOrder();
        order.setOutboundNo("OUT001");
        order.setStatus("CREATED");
        when(outboundOrderMapper.selectOne(any())).thenReturn(order);
        doReturn(1).when(outboundOrderMapper).updateById(any(OutboundOrder.class));
        when(stateMachineEngine.fire(anyString(), anyString(), anyString(), any())).thenReturn("ALLOCATED");

        outboundOrderService.allocate("OUT001", "SKU001", "WH01", "A-01", "B001", new BigDecimal("10"));

        assertEquals("ALLOCATED", order.getStatus());
        assertEquals(new BigDecimal("10"), order.getAllocatedQty());
    }

    @Test
    void testAllocate_invalidStatus()
    {
        OutboundOrder order = new OutboundOrder();
        order.setStatus("SHIPPED");
        when(outboundOrderMapper.selectOne(any())).thenReturn(order);
        // 状态机拒绝非法转换（SHIPPED → ALLOCATE），抛异常
        when(stateMachineEngine.fire(anyString(), anyString(), anyString(), any()))
                .thenThrow(new BizException(ErrorCode.ORDER_STATUS_ERROR));

        assertThrows(BizException.class,
                () -> outboundOrderService.allocate("OUT001", "SKU001", "WH01", "A-01", "B001", BigDecimal.TEN));
    }

    @Test
    void testConfirmShip_success()
    {
        OutboundOrder order = new OutboundOrder();
        order.setOutboundNo("OUT001");
        order.setStatus("PICKED");
        order.setOwnerCodeCol("OWNER01");
        when(outboundOrderMapper.selectOne(any())).thenReturn(order);
        doReturn(1).when(outboundOrderMapper).updateById(any(OutboundOrder.class));
        when(stateMachineEngine.fire(anyString(), anyString(), anyString(), any())).thenReturn("SHIPPED");
        // deductAllocatedInventory 成功：返回 mock 库存记录（不抛异常）——核销预占扣减
        when(inventoryService.deductAllocatedInventory(anyString(), anyString(), anyString(), anyString(),
                anyString(), any(), anyString(), anyString(), anyString())).thenReturn(new Inventory());

        outboundOrderService.confirmShip("OUT001", "SKU001", "WH01", "A-01", "B001", new BigDecimal("10"));

        assertEquals("SHIPPED", order.getStatus());
        assertNotNull(order.getActualShipTime());
        verify(inventoryService, times(1)).deductAllocatedInventory(anyString(), anyString(), anyString(), anyString(),
                anyString(), any(), anyString(), anyString(), anyString());
    }

    @Test
    void testConfirmShip_deductFailed()
    {
        OutboundOrder order = new OutboundOrder();
        order.setOutboundNo("OUT001");
        order.setStatus("PICKED");
        order.setOwnerCodeCol("OWNER01");
        when(outboundOrderMapper.selectOne(any())).thenReturn(order);
        when(stateMachineEngine.fire(anyString(), anyString(), anyString(), any())).thenReturn("SHIPPED");
        // deductAllocatedInventory 失败：预占不足/乐观锁冲突时抛 RuntimeException，服务层转换为 BizException
        when(inventoryService.deductAllocatedInventory(anyString(), anyString(), anyString(), anyString(),
                anyString(), any(), anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("库存不足"));

        assertThrows(BizException.class,
                () -> outboundOrderService.confirmShip("OUT001", "SKU001", "WH01", "A-01", "B001", BigDecimal.TEN));
    }

    @Test
    void testCancel_alreadyShipped()
    {
        OutboundOrder order = new OutboundOrder();
        order.setStatus("SHIPPED");
        when(outboundOrderMapper.selectOne(any())).thenReturn(order);

        assertThrows(BizException.class, () -> outboundOrderService.cancel("OUT001", "SKU001", "WH01", "A-01", "B001", BigDecimal.TEN));
    }
}
