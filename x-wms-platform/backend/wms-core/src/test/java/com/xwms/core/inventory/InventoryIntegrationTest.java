package com.xwms.core.inventory;

import com.xwms.core.inventory.entity.Inventory;
import com.xwms.core.inventory.mapper.InventoryMapper;
import com.xwms.core.inventory.service.InventoryService;
import com.xwms.core.support.IntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 库存核心集成测试
 * 真实 MySQL（TestContainers），验证：
 * 1. 增加/扣减/预占/释放的原子性与一致性（乐观锁）
 * 2. 并发扣减不超卖（乐观锁原子 SQL）
 * 3. 库存不足/记录不存在时抛 RuntimeException
 * <p>
 * 契约权威：{@link InventoryService} 4 个写方法 9 参签名，返回 {@link Inventory}，
 * 失败抛 {@link RuntimeException}；{@link InventoryMapper} 写方法 (id, qty, version) 三参乐观锁。
 */
class InventoryIntegrationTest extends IntegrationTestBase {

    @Autowired
    private InventoryService inventoryService;
    @Autowired
    private InventoryMapper inventoryMapper;

    private static final String SKU = "IT-SKU-001";
    private static final String WH = "WH01";
    private static final String LOC = "A-01-01";
    private static final String BATCH = "B-IT-001";
    private static final String OWNER = "OWNER01";

    @BeforeEach
    void setUp()
    {
        inventoryMapper.delete(null);
    }

    /** 读回唯一键库存记录（9 参唯一键：wh+loc+sku+batch+owner） */
    private Inventory reload()
    {
        return inventoryService.getInventory(WH, LOC, SKU, BATCH, OWNER);
    }

    /** BigDecimal 比较忽略 scale（PG NUMERIC(18,4) 返回 70.0000，不能用 equals） */
    private static void assertQtyEquals(String expected, BigDecimal actual)
    {
        assertNotNull(actual, "库存记录不应为空");
        assertEquals(0, actual.compareTo(new BigDecimal(expected)),
                "期望 " + expected + "，实际 " + actual);
    }

    // ============================================================
   
    // add + deduct
    // ============================================================

   
    @Test
    void testAddAndDeduct_success()
    {
        inventoryService.addInventory(WH, LOC, SKU, BATCH, OWNER,
                new BigDecimal("100"), "INBOUND", "IN-IT-001", "test");
        assertQtyEquals("100", reload().getQuantity());
        assertQtyEquals("100", reload().getAvailableQty());

        inventoryService.deductInventory(WH, LOC, SKU, BATCH, OWNER,
                new BigDecimal("30"), "OUTBOUND", "OUT-IT-001", "test");

        Inventory after = reload();
        assertQtyEquals("70", after.getQuantity());
        assertQtyEquals("70", after.getAvailableQty());
    }

    @Test
    void testDeduct_notEnough()
    {
        inventoryService.addInventory(WH, LOC, SKU, BATCH, OWNER,
                new BigDecimal("10"), "INBOUND", "IN-IT-002", "test");

        assertThrows(RuntimeException.class, () ->
                inventoryService.deductInventory(WH, LOC, SKU, BATCH, OWNER,
                        new BigDecimal("20"), "OUTBOUND", "OUT-IT-002", "test"));

        // 库存不足时记录不变
        assertQtyEquals("10", reload().getQuantity());
    }

    @Test
    void testDeduct_notFound()
    {
        // 未 add，直接扣减不存在的唯一键
        assertThrows(RuntimeException.class, () ->
                inventoryService.deductInventory(WH, LOC, SKU, BATCH, OWNER,
                        new BigDecimal("10"), "OUTBOUND", "OUT-IT-003", "test"));
    }

    // ============================================================
   
    // allocate + release
    // ============================================================

   
    @Test
    void testAllocate_success()
    {
        inventoryService.addInventory(WH, LOC, SKU, BATCH, OWNER,
                new BigDecimal("100"), "INBOUND", "IN-IT-003", "test");

        inventoryService.allocateInventory(WH, LOC, SKU, BATCH, OWNER,
                new BigDecimal("30"), "ALLOCATE", "OUT-IT-004", "test");

        Inventory after = reload();
        assertQtyEquals("100", after.getQuantity());      // 预占不减总量
        assertQtyEquals("70", after.getAvailableQty());   // 可用减 30
        assertQtyEquals("30", after.getAllocatedQty());   // 预占增 30
    }

    @Test
    void testAllocate_availableNotEnough()
    {
        inventoryService.addInventory(WH, LOC, SKU, BATCH, OWNER,
                new BigDecimal("10"), "INBOUND", "IN-IT-004", "test");

        assertThrows(RuntimeException.class, () ->
                inventoryService.allocateInventory(WH, LOC, SKU, BATCH, OWNER,
                        new BigDecimal("20"), "ALLOCATE", "OUT-IT-005", "test"));

        // 可用不足时记录不变
        assertQtyEquals("10", reload().getAvailableQty());
    }

    @Test
    void testRelease_rollbackAllocation()
    {
        inventoryService.addInventory(WH, LOC, SKU, BATCH, OWNER,
                new BigDecimal("100"), "INBOUND", "IN-IT-005", "test");
        inventoryService.allocateInventory(WH, LOC, SKU, BATCH, OWNER,
                new BigDecimal("30"), "ALLOCATE", "OUT-IT-006", "test");

        inventoryService.releaseAllocation(WH, LOC, SKU, BATCH, OWNER,
                new BigDecimal("30"), "RELEASE", "OUT-IT-006", "test");

        Inventory after = reload();
        assertQtyEquals("100", after.getAvailableQty());
        assertQtyEquals("0", after.getAllocatedQty());
    }

    // ============================================================
   
    // 预占后扣减衔接（暴露 deduct 与 allocate 的语义衔接）
    // ============================================================

   
    @Test
    void testDeductAfterAllocate()
    {
        inventoryService.addInventory(WH, LOC, SKU, BATCH, OWNER,
                new BigDecimal("100"), "INBOUND", "IN-IT-006", "test");
        // 预占 30：available=70, allocated=30, quantity=100
        inventoryService.allocateInventory(WH, LOC, SKU, BATCH, OWNER,
                new BigDecimal("30"), "ALLOCATE", "OUT-IT-007", "test");
        // 扣减 70：deduct 同时减 quantity 和 available
        // 按 mapper SQL 实际行为：quantity=30, available=0（allocated 不变，仍=30）
        inventoryService.deductInventory(WH, LOC, SKU, BATCH, OWNER,
                new BigDecimal("70"), "OUTBOUND", "OUT-IT-007", "test");

        Inventory after = reload();
        assertQtyEquals("30", after.getQuantity());
        assertQtyEquals("0", after.getAvailableQty());
        // 注：deduct 不核销 allocated，预占后扣减会留下 allocated=30 的残留。
        // 此为当前实现的已知语义（design.md §5.4），测试按 mapper SQL 实际行为断言。
        assertQtyEquals("30", after.getAllocatedQty());
    }

    // ============================================================
   
    // 并发防超卖（R2 核心：乐观锁原子 SQL）
    // ============================================================

   
    @Test
    void testConcurrentDeduct_noOversell() throws InterruptedException {
        // 初始库存 100
        inventoryService.addInventory(WH, LOC, SKU, BATCH, OWNER,
                new BigDecimal("100"), "INBOUND", "IN-IT-007", "test");

        // 50 线程各扣 3，总需求 150，库存只有 100
        int threadCount = 50;
        BigDecimal deductPerThread = new BigDecimal("3");
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++)
    {
            executor.submit(() -> {
                try {
                    startLatch.await(); // 同步起跑，最大化并发冲突
                    inventoryService.deductInventory(WH, LOC, SKU, BATCH, OWNER,
                            deductPerThread, "OUTBOUND", "CONCURRENT", "test");
                    successCount.incrementAndGet();
                } catch (RuntimeException e)
    {
                    // 库存不足/乐观锁冲突重试耗尽 → 失败
                    failCount.incrementAndGet();
                } catch (InterruptedException e)
    {
                    Thread.currentThread().interrupt();
                    failCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }
        startLatch.countDown(); // 同时放开
        doneLatch.await();
        executor.shutdown();

        // 成功 + 失败 = 50（每个线程都有确定结局）
        assertEquals(threadCount, successCount.get() + failCount.get());
        // 成功次数 ≤ 34（34×3=102>100，最多 33 次成功=99，剩 1 不够扣 3）
        assertTrue(successCount.get() <= 34,
                "成功扣减不应超过 34 次，实际: " + successCount.get());
        // 成功扣减总量 ≤ 库存 100（绝不超卖）
        assertTrue(successCount.get() * 3 <= 100,
                "扣减总量不应超过库存，成功: " + successCount.get());

        // 最终库存 ≥ 0 且 < 3（剩余不足以再扣一次 3）
        BigDecimal remaining = reload().getQuantity();
        assertTrue(remaining.compareTo(BigDecimal.ZERO) >= 0,
                "库存不应为负，实际: " + remaining);
        assertTrue(remaining.compareTo(new BigDecimal("3")) < 0,
                "剩余库存应小于 3，实际: " + remaining);
    }
}
