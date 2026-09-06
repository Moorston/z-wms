package com.xwms.core.outbound;

import com.xwms.core.inventory.entity.Inventory;
import com.xwms.core.inventory.mapper.InventoryMapper;
import com.xwms.core.inventory.service.InventoryService;
import com.xwms.core.outbound.entity.OutboundOrder;
import com.xwms.core.outbound.mapper.OutboundOrderMapper;
import com.xwms.core.outbound.service.OutboundOrderService;
import com.xwms.core.support.IntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 出库全流程集成测试
 * 验证：创建→分配预占→拣货→发运扣减 的完整链路
 * 核心验证：库存预占和实际扣减的一致性
 */
class OutboundFlowIntegrationTest extends IntegrationTestBase {

    @Autowired
    private OutboundOrderService outboundOrderService;
    @Autowired
    private OutboundOrderMapper outboundOrderMapper;
    @Autowired
    private InventoryService inventoryService;
    @Autowired
    private InventoryMapper inventoryMapper;

    private static final String SKU = "OUT-SKU-001";
    private static final String WH = "WH01";
    private static final String LOC = "A-01-01";
    private static final String BATCH = "B-OUT-001";
    private static final String OWNER = "OWNER01";

    /** 按 SKU+货主跨库位汇总可用库存（替代已删除的 getAvailableQty） */
    private BigDecimal availableQty()
    {
        return inventoryService.getInventoryBySku(SKU, OWNER)
                .stream()
                    .map(Inventory::getAvailableQty)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @BeforeEach
    void setUp()
    {
        outboundOrderMapper.delete(null);
        // 清残留库存，每个测试从干净状态开始（预占接通后 available 跨测试会变，须重置）
        inventoryMapper.delete(null);
        // 初始库存100
        inventoryService.addInventory(WH, LOC, SKU, BATCH,
                OWNER, new BigDecimal("100"), "INBOUND", "INIT-SETUP", "test");
    }

    @Test
    void testFullOutboundFlow()
    {
        // 1. 创建出库单
        OutboundOrder order = new OutboundOrder();
        order.setOutboundNo("OUT-IT-001");
        order.setOutboundType("SALES");
        order.setWarehouseCode(WH);
        order.setTotalQty(new BigDecimal("30"));
        OutboundOrder created = outboundOrderService.create(order);
        assertEquals("CREATED", created.getStatus());

        // 2. 分配预占（库存100，预占30，可用变70）
        outboundOrderService.allocate("OUT-IT-001", SKU, WH, LOC, BATCH, new BigDecimal("30"));
        OutboundOrder allocated = getOrder("OUT-IT-001");
        assertEquals("ALLOCATED", allocated.getStatus());
        assertEquals(new BigDecimal("30"), allocated.getAllocatedQty());

        // 3. 拣货确认
        outboundOrderService.confirmPick("OUT-IT-001", new BigDecimal("30"));
        OutboundOrder picked = getOrder("OUT-IT-001");
        assertEquals("PICKED", picked.getStatus());

        // 4. 发运确认（实际扣减库存）
        outboundOrderService.confirmShip("OUT-IT-001", SKU, WH, LOC, BATCH, new BigDecimal("30"));
        OutboundOrder shipped = getOrder("OUT-IT-001");
        assertEquals("SHIPPED", shipped.getStatus());
        assertNotNull(shipped.getActualShipTime());

        // 5. 验证库存（100-30=70）
        assertEquals(new BigDecimal("70"), availableQty());
    }

    @Test
    void testAllocate_moreThanAvailable()
    {
        OutboundOrder order = new OutboundOrder();
        order.setOutboundNo("OUT-IT-OVER");
        order.setTotalQty(new BigDecimal("200"));
        outboundOrderService.create(order);

        // 库存只有100，分配200：预占校验 available_qty，不足立即失败（预占阶段就挡，优于发运时才失败）
        assertThrows(Exception.class, () ->
                outboundOrderService.allocate("OUT-IT-OVER", SKU, WH, LOC, BATCH, new BigDecimal("200")));
    }

    @Test
    void testCancel_afterAllocate()
    {
        OutboundOrder order = new OutboundOrder();
        order.setOutboundNo("OUT-IT-CANCEL");
        order.setTotalQty(new BigDecimal("20"));
        outboundOrderService.create(order);
        outboundOrderService.allocate("OUT-IT-CANCEL", SKU, WH, LOC, BATCH, new BigDecimal("20"));

        // 取消后释放预占
        outboundOrderService.cancel("OUT-IT-CANCEL", SKU, WH, LOC, BATCH, new BigDecimal("20"));
        OutboundOrder cancelled = getOrder("OUT-IT-CANCEL");
        assertEquals("CANCELLED", cancelled.getStatus());

        // 库存应该恢复100
        assertEquals(new BigDecimal("100"), availableQty());
    }

    private OutboundOrder getOrder(String orderNo)
    {
        return outboundOrderMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<OutboundOrder>()
                        .eq(OutboundOrder::getOutboundNo, orderNo));
    }
}
