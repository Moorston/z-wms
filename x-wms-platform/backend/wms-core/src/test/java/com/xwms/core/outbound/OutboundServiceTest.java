package com.xwms.core.outbound;

import com.xwms.core.inventory.entity.Inventory;
import com.xwms.core.inventory.mapper.InventoryMapper;
import com.xwms.core.inventory.service.InventoryService;
import com.xwms.core.outbound.entity.OutboundDetail;
import com.xwms.core.outbound.entity.OutboundOrder;
import com.xwms.core.outbound.mapper.OutboundDetailMapper;
import com.xwms.core.outbound.mapper.OutboundOrderMapper;
import com.xwms.core.outbound.mapper.PickRecordMapper;
import com.xwms.core.outbound.mapper.ShipRecordMapper;
import com.xwms.core.outbound.service.OutboundService;
import com.xwms.core.support.BaseTest;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 出库管理核心服务单元测试（B 链路：明细版出库）
 * 核心验证：allocate/cancelAllocation/ship 对库存服务的调用契约
 * 台账数值正确性由集成测试覆盖，此处验证调用编排与异常传播
 */
class OutboundServiceTest extends BaseTest {

    @Mock
    private OutboundOrderMapper outboundOrderMapper;
    @Mock
    private OutboundDetailMapper outboundDetailMapper;
    @Mock
    private PickRecordMapper pickRecordMapper;
    @Mock
    private ShipRecordMapper shipRecordMapper;
    @Mock
    private InventoryService inventoryService;
    @Mock
    private InventoryMapper inventoryMapper;

    @InjectMocks
    private OutboundService outboundService;

    private static final String OUTBOUND_NO = "OUT-B-001";
    private static final String OWNER = "OWNER01";
    private static final String SKU = "SKU-B-001";
    private static final String WH = "WH01";
    private static final String LOC = "A-01";
    private static final String BATCH = "B001";

    private OutboundOrder order(String status)
    {
        OutboundOrder o = new OutboundOrder();
        o.setOutboundNo(OUTBOUND_NO);
        o.setOwnerCodeCol(OWNER);
        o.setStatus(status);
        o.setTotalQty(new BigDecimal("10"));
        o.setAllocatedQty(BigDecimal.ZERO);
        o.setPickedQty(BigDecimal.ZERO);
        o.setPackedQty(BigDecimal.ZERO);
        o.setShippedQty(BigDecimal.ZERO);
        return o;
    }

    private OutboundDetail detail(BigDecimal expected)
    {
        OutboundDetail d = new OutboundDetail();
        d.setDetailNo("OD-1");
        d.setOutboundNo(OUTBOUND_NO);
        d.setSkuCode(SKU);
        d.setExpectedQty(expected);
        d.setAllocatedQty(BigDecimal.ZERO);
        d.setPickedQty(BigDecimal.ZERO);
        d.setPackedQty(BigDecimal.ZERO);
        d.setShippedQty(BigDecimal.ZERO);
        return d;
    }

    private Inventory fifoRow(String wh, String loc, String batch, BigDecimal available)
    {
        Inventory inv = new Inventory();
        inv.setWarehouseCode(wh);
        inv.setLocationCode(loc);
        inv.setSkuCode(SKU);
        inv.setBatchNo(batch);
        inv.setAvailableQty(available);
        return inv;
    }

    @Test
    void testAllocate_fifo_success()
    {
        when(outboundOrderMapper.selectByOutboundNo(OUTBOUND_NO)).thenReturn(order("CREATED"));
        OutboundDetail d = detail(new BigDecimal("10"));
        when(outboundDetailMapper.selectByOutboundNo(OUTBOUND_NO))
                .thenReturn(List.of(d));
        // FIFO 命中单行可用 10
        when(inventoryMapper.selectAvailableFifo(SKU, OWNER))
                .thenReturn(List.of(fifoRow(WH, LOC, BATCH, new BigDecimal("10"))));

        OutboundOrder result = outboundService.allocate(OUTBOUND_NO, "op");

        assertEquals("ALLOCATED", result.getStatus());
        // 预占 10
        verify(inventoryService, times(1)).allocateInventory(eq(WH), eq(LOC), eq(SKU),
                eq(BATCH), eq(OWNER), eq(new BigDecimal("10")), eq("OUTBOUND"),
                eq(OUTBOUND_NO), eq("op"));
        // 明细回填仓库/库位/批次 + allocatedQty
        assertEquals(WH, d.getWarehouseCode());
        assertEquals(LOC, d.getLocationCode());
        assertEquals(BATCH, d.getBatchNo());
        assertEquals(new BigDecimal("10"), d.getAllocatedQty());
        assertEquals("ALLOCATED", d.getStatus());
    }

    @Test
    void testAllocate_fifoCrossRow_success()
    {
        when(outboundOrderMapper.selectByOutboundNo(OUTBOUND_NO)).thenReturn(order("CREATED"));
        OutboundDetail d = detail(new BigDecimal("15"));
        when(outboundDetailMapper.selectByOutboundNo(OUTBOUND_NO))
                .thenReturn(List.of(d));
        // 跨行累加：首行 10 + 次行 5
        when(inventoryMapper.selectAvailableFifo(SKU, OWNER))
                .thenReturn(List.of(
                        fifoRow(WH, LOC, BATCH, new BigDecimal("10")),
                        fifoRow(WH, "B-02", "B002", new BigDecimal("5"))));

        outboundService.allocate(OUTBOUND_NO, "op");

        // 两次预占：10 + 5
        verify(inventoryService, times(1)).allocateInventory(eq(WH), eq(LOC), eq(SKU),
                eq(BATCH), eq(OWNER), eq(new BigDecimal("10")), anyString(), anyString(), anyString());
        verify(inventoryService, times(1)).allocateInventory(eq(WH), eq("B-02"), eq(SKU),
                eq("B002"), eq(OWNER), eq(new BigDecimal("5")), anyString(), anyString(), anyString());
        // 明细回填首行（FIFO 首命中）
        assertEquals(WH, d.getWarehouseCode());
        assertEquals(LOC, d.getLocationCode());
        assertEquals(BATCH, d.getBatchNo());
        assertEquals(new BigDecimal("15"), d.getAllocatedQty());
    }

    @Test
    void testAllocate_insufficient_throws()
    {
        when(outboundOrderMapper.selectByOutboundNo(OUTBOUND_NO)).thenReturn(order("CREATED"));
        when(outboundDetailMapper.selectByOutboundNo(OUTBOUND_NO))
                .thenReturn(List.of(detail(new BigDecimal("100"))));
        // 可用仅 10，不足 100
        when(inventoryMapper.selectAvailableFifo(SKU, OWNER))
                .thenReturn(List.of(fifoRow(WH, LOC, BATCH, new BigDecimal("10"))));

        // 不足应抛异常（@Transactional 回滚靠调用方/容器，单测验证异常本身）
        assertThrows(RuntimeException.class, () -> outboundService.allocate(OUTBOUND_NO, "op"));
    }

    @Test
    void testCancelAllocation_releasesPreallocation()
    {
        OutboundOrder o = order("ALLOCATED");
        when(outboundOrderMapper.selectByOutboundNo(OUTBOUND_NO)).thenReturn(o);
        OutboundDetail d = detail(new BigDecimal("10"));
        d.setWarehouseCode(WH);
        d.setLocationCode(LOC);
        d.setBatchNo(BATCH);
        d.setAllocatedQty(new BigDecimal("10"));
        when(outboundDetailMapper.selectByOutboundNo(OUTBOUND_NO))
                .thenReturn(List.of(d));

        outboundService.cancelAllocation(OUTBOUND_NO, "op");

        verify(inventoryService, times(1)).releaseAllocation(eq(WH), eq(LOC), eq(SKU),
                eq(BATCH), eq(OWNER), eq(new BigDecimal("10")), eq("OUTBOUND_CANCEL"),
                eq(OUTBOUND_NO), eq("op"));
        assertEquals(BigDecimal.ZERO, d.getAllocatedQty());
        assertEquals("CREATED", d.getStatus());
        assertEquals("CREATED", o.getStatus());
    }

    @Test
    void testCancelAllocation_noAllocation_skipsRelease()
    {
        OutboundOrder o = order("CREATED");
        when(outboundOrderMapper.selectByOutboundNo(OUTBOUND_NO)).thenReturn(o);
        // 明细无预占（allocatedQty=0 或 warehouseCode=null）
        OutboundDetail d = detail(new BigDecimal("10"));
        when(outboundDetailMapper.selectByOutboundNo(OUTBOUND_NO))
                .thenReturn(List.of(d));

        outboundService.cancelAllocation(OUTBOUND_NO, "op");

        // 无预占不应调 releaseAllocation
        verify(inventoryService, never()).releaseAllocation(anyString(), anyString(), anyString(),
                anyString(), anyString(), any(), anyString(), anyString(), anyString());
    }

    @Test
    void testShip_deductsAllocated()
    {
        OutboundOrder o = order("PACKED");
        o.setPackedQty(new BigDecimal("10"));
        when(outboundOrderMapper.selectByOutboundNo(OUTBOUND_NO)).thenReturn(o);
        OutboundDetail d = detail(new BigDecimal("10"));
        d.setWarehouseCode(WH);
        d.setLocationCode(LOC);
        d.setBatchNo(BATCH);
        d.setAllocatedQty(new BigDecimal("10"));
        d.setPackedQty(new BigDecimal("10"));
        when(outboundDetailMapper.selectByOutboundNo(OUTBOUND_NO))
                .thenReturn(List.of(d));

        outboundService.ship(OUTBOUND_NO, "SF", "TRK001", new BigDecimal("10"),
                1, new BigDecimal("1.0"), new BigDecimal("0.01"), "op");

        // 核销预占扣减：用 allocatedQty（10），非 packedQty
        verify(inventoryService, times(1)).deductAllocatedInventory(eq(WH), eq(LOC), eq(SKU),
                eq(BATCH), eq(OWNER), eq(new BigDecimal("10")), eq("OUTBOUND"),
                eq(OUTBOUND_NO), eq("op"));
        assertEquals("SHIPPED", d.getStatus());
    }

    @Test
    void testShip_deductFailed_throws()
    {
        OutboundOrder o = order("PACKED");
        o.setPackedQty(new BigDecimal("10"));
        when(outboundOrderMapper.selectByOutboundNo(OUTBOUND_NO)).thenReturn(o);
        OutboundDetail d = detail(new BigDecimal("10"));
        d.setWarehouseCode(WH);
        d.setLocationCode(LOC);
        d.setBatchNo(BATCH);
        d.setAllocatedQty(new BigDecimal("10"));
        d.setPackedQty(new BigDecimal("10"));
        when(outboundDetailMapper.selectByOutboundNo(OUTBOUND_NO))
                .thenReturn(List.of(d));
        // 预占不足/乐观锁冲突
        doThrow(new RuntimeException("预占不足")).when(inventoryService)
                .deductAllocatedInventory(anyString(), anyString(), anyString(), anyString(),
                        anyString(), any(), anyString(), anyString(), anyString());

        assertThrows(RuntimeException.class, () -> outboundService.ship(OUTBOUND_NO, "SF", "TRK001",
                new BigDecimal("10"), 1, new BigDecimal("1.0"), new BigDecimal("0.01"), "op"));
    }

    @Test
    void testAllocate_orderNotFound()
    {
        when(outboundOrderMapper.selectByOutboundNo(OUTBOUND_NO)).thenReturn(null);
        assertThrows(RuntimeException.class, () -> outboundService.allocate(OUTBOUND_NO, "op"));
    }

    @Test
    void testAllocate_emptyDetails()
    {
        when(outboundOrderMapper.selectByOutboundNo(OUTBOUND_NO)).thenReturn(order("CREATED"));
        when(outboundDetailMapper.selectByOutboundNo(OUTBOUND_NO))
                .thenReturn(Collections.emptyList());

        OutboundOrder result = outboundService.allocate(OUTBOUND_NO, "op");
        assertEquals("ALLOCATED", result.getStatus());
        verify(inventoryService, never()).allocateInventory(anyString(), anyString(), anyString(),
                anyString(), anyString(), any(), anyString(), anyString(), anyString());
    }
}
