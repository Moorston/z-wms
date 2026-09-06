package com.xwms.core.inventory;

import com.xwms.core.inventory.entity.Inventory;
import com.xwms.core.inventory.mapper.InventoryMapper;
import com.xwms.core.inventory.service.InventoryService;
import com.xwms.core.support.BaseTest;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 库存服务单元测试
 * 核心测试：Oracle 原子扣减/增加/预占/释放 + 乐观锁重试（成功与耗尽）
 * <p>
 * 契约权威：{@link InventoryService} 的 4 个写方法均为 9 参业务签名，返回 {@link Inventory}，
 * 失败（库存不足/乐观锁冲突/记录不存在）抛 {@link RuntimeException}。
 * 对应 {@link InventoryMapper} 写方法为 (id, qty, version) 三参乐观锁，返回受影响行数 int。
 */
class InventoryServiceTest extends BaseTest {

    private static final String WH = "WH01";
    private static final String LOC = "A-01-01";
    private static final String SKU = "SKU001";
    private static final String BATCH = "BATCH001";
    private static final String OWNER = "OWNER01";

    @Mock
    private InventoryMapper inventoryMapper;

    @InjectMocks
    private InventoryService inventoryService;

    /** 构造一条存在的库存记录 */
    private Inventory existing(long id, BigDecimal qty, BigDecimal available, int version)
    {
        Inventory inv = new Inventory();
        inv.setId(id);
        inv.setWarehouseCode(WH);
        inv.setLocationCode(LOC);
        inv.setSkuCode(SKU);
        inv.setBatchNo(BATCH);
        inv.setOwnerCodeCol(OWNER);
        inv.setQuantity(qty);
        inv.setAvailableQty(available);
        inv.setAllocatedQty(BigDecimal.ZERO);
        inv.setVersion(version);
        return inv;
    }

    // ============================================================
   
    // addInventory
    // ============================================================

   
    @Test
    void testAddInventory_newRecord()
    {
        // 记录不存在 → insert 新记录（version=0）→ addInventory 成功
        when(inventoryMapper.selectByUniqueKey(WH, LOC, SKU, BATCH, OWNER)).thenReturn(null);
        // Mockito mock 的 insert 不回填 @TableId，主代码 insert 后立即用 getId()，
        // 故用 doAnswer 模拟 DB 回填 id，使后续 addInventory(id,...) / selectById(id) 正常
        doAnswer(inv -> {
            Inventory entity = inv.getArgument(0);
            entity.setId(1L);
            return 1;
        }).when(inventoryMapper).insert(any(Inventory.class));
        when(inventoryMapper.addInventory(anyLong(), any(BigDecimal.class), anyInt())).thenReturn(1);
        when(inventoryMapper.selectById(anyLong())).thenReturn(existing(1L, new BigDecimal("50"), new BigDecimal("50"), 1));

        Inventory result = inventoryService.addInventory(WH, LOC, SKU, BATCH, OWNER,
                new BigDecimal("50"), "INBOUND", "IN-001", "test");

        assertNotNull(result);
        verify(inventoryMapper, times(1)).insert(any(Inventory.class));
        verify(inventoryMapper, times(1)).addInventory(anyLong(), any(BigDecimal.class), anyInt());
    }

    @Test
    void testAddInventory_existingRecord()
    {
        Inventory inv = existing(1L, new BigDecimal("100"), new BigDecimal("100"), 0);
        when(inventoryMapper.selectByUniqueKey(WH, LOC, SKU, BATCH, OWNER)).thenReturn(inv);
        when(inventoryMapper.addInventory(anyLong(), any(BigDecimal.class), anyInt())).thenReturn(1);
        when(inventoryMapper.selectById(anyLong())).thenReturn(existing(1L, new BigDecimal("150"), new BigDecimal("150"), 1));

        Inventory result = inventoryService.addInventory(WH, LOC, SKU, BATCH, OWNER,
                new BigDecimal("50"), "INBOUND", "IN-002", "test");

        assertNotNull(result);
        verify(inventoryMapper, never()).insert(any(Inventory.class));
        verify(inventoryMapper, times(1)).addInventory(anyLong(), any(BigDecimal.class), anyInt());
    }

    // ============================================================
   
    // deductInventory
    // ============================================================

   
    @Test
    void testDeductInventory_success()
    {
        Inventory inv = existing(1L, new BigDecimal("100"), new BigDecimal("100"), 0);
        when(inventoryMapper.selectByUniqueKey(WH, LOC, SKU, BATCH, OWNER)).thenReturn(inv);
        when(inventoryMapper.deductInventory(anyLong(), any(BigDecimal.class), anyInt())).thenReturn(1);
        when(inventoryMapper.selectById(anyLong())).thenReturn(existing(1L, new BigDecimal("70"), new BigDecimal("70"), 1));

        Inventory result = inventoryService.deductInventory(WH, LOC, SKU, BATCH, OWNER,
                new BigDecimal("30"), "OUTBOUND", "OUT-001", "test");

        assertNotNull(result);
        verify(inventoryMapper, times(1)).deductInventory(anyLong(), any(BigDecimal.class), anyInt());
    }

    @Test
    void testDeductInventory_notEnough()
    {
        // 库存 10，扣 20 → 抛异常，不调用 deductInventory
        Inventory inv = existing(1L, new BigDecimal("10"), new BigDecimal("10"), 0);
        when(inventoryMapper.selectByUniqueKey(WH, LOC, SKU, BATCH, OWNER)).thenReturn(inv);

        assertThrows(RuntimeException.class, () ->
                inventoryService.deductInventory(WH, LOC, SKU, BATCH, OWNER,
                        new BigDecimal("20"), "OUTBOUND", "OUT-002", "test"));
        verify(inventoryMapper, never()).deductInventory(anyLong(), any(BigDecimal.class), anyInt());
    }

    @Test
    void testDeductInventory_notFound()
    {
        when(inventoryMapper.selectByUniqueKey(WH, LOC, SKU, BATCH, OWNER)).thenReturn(null);

        assertThrows(RuntimeException.class, () ->
                inventoryService.deductInventory(WH, LOC, SKU, BATCH, OWNER,
                        new BigDecimal("10"), "OUTBOUND", "OUT-003", "test"));
    }

    // ============================================================
   
    // 乐观锁重试（R2 核心增量）
    // ============================================================

   
    @Test
    void testDeductInventory_optimisticLockRetrySuccess()
    {
        // 前 2 次乐观锁冲突（返回 0），第 3 次成功（返回 1）
        Inventory v0 = existing(1L, new BigDecimal("100"), new BigDecimal("100"), 0);
        when(inventoryMapper.selectByUniqueKey(WH, LOC, SKU, BATCH, OWNER)).thenReturn(v0);
        when(inventoryMapper.deductInventory(anyLong(), any(BigDecimal.class), anyInt()))
                .thenReturn(0, 0, 1);
        // 重试时 selectById 重读，返回递增版本号的记录
        when(inventoryMapper.selectById(anyLong()))
                .thenReturn(existing(1L, new BigDecimal("100"), new BigDecimal("100"), 1),
                        existing(1L, new BigDecimal("100"), new BigDecimal("100"), 2),
                        existing(1L, new BigDecimal("70"), new BigDecimal("70"), 3));

        Inventory result = inventoryService.deductInventory(WH, LOC, SKU, BATCH, OWNER,
                new BigDecimal("30"), "OUTBOUND", "OUT-004", "test");

        assertNotNull(result);
        verify(inventoryMapper, times(3)).deductInventory(anyLong(), any(BigDecimal.class), anyInt());
    }

    @Test
    void testDeductInventory_optimisticLockExhausted()
    {
        Inventory v0 = existing(1L, new BigDecimal("100"), new BigDecimal("100"), 0);
        when(inventoryMapper.selectByUniqueKey(WH, LOC, SKU, BATCH, OWNER)).thenReturn(v0);
        // 恒返回 0（持续冲突），3 次重试耗尽
        when(inventoryMapper.deductInventory(anyLong(), any(BigDecimal.class), anyInt())).thenReturn(0);
        when(inventoryMapper.selectById(anyLong()))
                .thenReturn(existing(1L, new BigDecimal("100"), new BigDecimal("100"), 1),
                        existing(1L, new BigDecimal("100"), new BigDecimal("100"), 2));

        assertThrows(RuntimeException.class, () ->
                inventoryService.deductInventory(WH, LOC, SKU, BATCH, OWNER,
                        new BigDecimal("30"), "OUTBOUND", "OUT-005", "test"));
        verify(inventoryMapper, times(3)).deductInventory(anyLong(), any(BigDecimal.class), anyInt());
    }

    // ============================================================
   
    // allocateInventory
    // ============================================================

   
    @Test
    void testAllocateInventory_success()
    {
        Inventory inv = existing(1L, new BigDecimal("100"), new BigDecimal("100"), 0);
        when(inventoryMapper.selectByUniqueKey(WH, LOC, SKU, BATCH, OWNER)).thenReturn(inv);
        when(inventoryMapper.allocateInventory(anyLong(), any(BigDecimal.class), anyInt())).thenReturn(1);
        when(inventoryMapper.selectById(anyLong()))
                .thenReturn(existing(1L, new BigDecimal("100"), new BigDecimal("70"), 1));

        Inventory result = inventoryService.allocateInventory(WH, LOC, SKU, BATCH, OWNER,
                new BigDecimal("30"), "ALLOCATE", "OUT-006", "test");

        assertNotNull(result);
        verify(inventoryMapper, times(1)).allocateInventory(anyLong(), any(BigDecimal.class), anyInt());
    }

    @Test
    void testAllocateInventory_availableNotEnough()
    {
        // 可用 10，预占 20 → 抛异常
        Inventory inv = existing(1L, new BigDecimal("100"), new BigDecimal("10"), 0);
        when(inventoryMapper.selectByUniqueKey(WH, LOC, SKU, BATCH, OWNER)).thenReturn(inv);

        assertThrows(RuntimeException.class, () ->
                inventoryService.allocateInventory(WH, LOC, SKU, BATCH, OWNER,
                        new BigDecimal("20"), "ALLOCATE", "OUT-007", "test"));
        verify(inventoryMapper, never()).allocateInventory(anyLong(), any(BigDecimal.class), anyInt());
    }

    // ============================================================
   
    // releaseAllocation
    // ============================================================

   
    @Test
    void testReleaseAllocation_success()
    {
        Inventory inv = existing(1L, new BigDecimal("100"), new BigDecimal("70"), 0);
        inv.setAllocatedQty(new BigDecimal("30"));
        when(inventoryMapper.selectByUniqueKey(WH, LOC, SKU, BATCH, OWNER)).thenReturn(inv);
        when(inventoryMapper.releaseAllocation(anyLong(), any(BigDecimal.class), anyInt())).thenReturn(1);
        when(inventoryMapper.selectById(anyLong()))
                .thenReturn(existing(1L, new BigDecimal("100"), new BigDecimal("100"), 1));

        Inventory result = inventoryService.releaseAllocation(WH, LOC, SKU, BATCH, OWNER,
                new BigDecimal("30"), "RELEASE", "OUT-008", "test");

        assertNotNull(result);
        verify(inventoryMapper, times(1)).releaseAllocation(anyLong(), any(BigDecimal.class), anyInt());
    }
}
