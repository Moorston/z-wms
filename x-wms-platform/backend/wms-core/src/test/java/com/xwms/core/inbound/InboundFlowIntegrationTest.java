package com.xwms.core.inbound;

import com.xwms.core.inbound.entity.InboundOrder;
import com.xwms.core.inbound.mapper.InboundOrderMapper;
import com.xwms.core.inbound.service.InboundOrderService;
import com.xwms.core.inventory.entity.Inventory;
import com.xwms.core.inventory.service.InventoryService;
import com.xwms.core.support.IntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 入库全流程集成测试
 * 验证：创建→收货→上架完成→库存增加 的完整链路
 */
class InboundFlowIntegrationTest extends IntegrationTestBase {

    @Autowired
    private InboundOrderService inboundOrderService;
    @Autowired
    private InboundOrderMapper inboundOrderMapper;
    @Autowired
    private InventoryService inventoryService;

    @BeforeEach
    void setUp()
    {
        inboundOrderMapper.delete(null);
    }

    @Test
    void testFullInboundFlow()
    {
        // 1. 创建入库单
        InboundOrder order = new InboundOrder();
        order.setInboundNo("IN-IT-001");
        order.setInboundType("PURCHASE");
        order.setWarehouseCode("WH01");
        order.setOwnerCode("OWNER01");
        order.setTotalQty(new BigDecimal("100"));
        InboundOrder created = inboundOrderService.create(order);
        assertEquals("CREATED", created.getStatus());

        // 2. 确认收货
        inboundOrderService.confirmReceive("IN-IT-001", new BigDecimal("100"));
        InboundOrder received = inboundOrderMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<InboundOrder>()
                        .eq(InboundOrder::getInboundNo, "IN-IT-001"));
        assertEquals("RECEIVED", received.getStatus());
        assertEquals(new BigDecimal("100"), received.getReceivedQty());
        assertNotNull(received.getReceiveTime());

        // 3. 上架完成（库存增加）
        inventoryService.addInventory("WH01", "A-01-01", "SKU001", "B001",
                "OWNER01", new BigDecimal("100"), "INBOUND", "IN-IT-001", "test");
        inboundOrderService.completePutaway("IN-IT-001", new BigDecimal("100"));

        InboundOrder completed = inboundOrderMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<InboundOrder>()
                        .eq(InboundOrder::getInboundNo, "IN-IT-001"));
        assertEquals("COMPLETED", completed.getStatus());
        assertEquals(new BigDecimal("100"), completed.getPutawayQty());

        // 4. 验证库存（按 SKU+货主跨库位汇总可用库存）
        BigDecimal available = inventoryService.getInventoryBySku("SKU001", "OWNER01")
                .stream()
                    .map(Inventory::getAvailableQty)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(new BigDecimal("100"), available);
    }

    @Test
    void testCancelCreatedOrder()
    {
        InboundOrder order = new InboundOrder();
        order.setInboundNo("IN-IT-CANCEL");
        order.setInboundType("PURCHASE");
        order.setTotalQty(new BigDecimal("50"));
        inboundOrderService.create(order);

        inboundOrderService.cancel("IN-IT-CANCEL");
        InboundOrder cancelled = inboundOrderMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<InboundOrder>()
                        .eq(InboundOrder::getInboundNo, "IN-IT-CANCEL"));
        assertEquals("CANCELLED", cancelled.getStatus());
    }

    @Test
    void testPartialReceive()
    {
        InboundOrder order = new InboundOrder();
        order.setInboundNo("IN-IT-PARTIAL");
        order.setTotalQty(new BigDecimal("100"));
        inboundOrderService.create(order);

        // 只收80
        inboundOrderService.confirmReceive("IN-IT-PARTIAL", new BigDecimal("80"));
        InboundOrder received = inboundOrderMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<InboundOrder>()
                        .eq(InboundOrder::getInboundNo, "IN-IT-PARTIAL"));
        assertEquals(new BigDecimal("80"), received.getReceivedQty());
    }
}
