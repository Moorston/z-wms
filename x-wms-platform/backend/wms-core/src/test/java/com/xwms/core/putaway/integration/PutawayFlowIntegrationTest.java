package com.xwms.core.putaway.integration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.xwms.common.lock.DistributedLock;
import com.xwms.core.inventory.service.InventoryService;
import com.xwms.core.putaway.entity.*;
import com.xwms.core.putaway.enums.PutawayStatus;
import com.xwms.core.putaway.event.PutawayEventPublisher;
import com.xwms.core.putaway.mapper.*;
import com.xwms.core.putaway.service.PutawayPerformanceService;
import com.xwms.core.putaway.service.PutawayTaskService;
import com.xwms.core.support.BaseTest;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;

/** 上架全流程集成测试 模拟完整业务流程：任务生成 → 派发 → 领取 → 上架确认（库存扣减）→ 完成 验证各环节状态流转、库存操作、事件发布的正确性 */
class PutawayFlowIntegrationTest extends BaseTest {

    @Mock private PutawayTaskMapper taskMapper;
    @Mock private PutawayTaskDetailMapper taskDetailMapper;
    @Mock private PutawayRecordMapper recordMapper;
    @Mock private PutawayReasonCodeMapper reasonCodeMapper;
    @Mock private PutawayRecommendLogMapper recommendLogMapper;
    @Mock private PutawayExceptionLogMapper exceptionLogMapper;
    @Mock private InventoryService inventoryService;
    @Mock private PutawayEventPublisher eventPublisher;
    @Mock private PutawayPerformanceService performanceService;
    @Mock private DistributedLock distributedLock;

    @InjectMocks private PutawayTaskService putawayTaskService;

    private static final String TASK_NO = "PAT20260816001";
    private static final String DETAIL_NO = "PAD20260816001";
    private static final String WAREHOUSE = "WH01";
    private static final String OWNER = "OWNER01";
    private static final String SKU = "SKU001";
    private static final String BATCH = "BATCH001";
    private static final String SRC_LOC = "RECV-01";
    private static final String TGT_LOC = "A-01-01";
    private static final String PICKER = "picker001";
    private static final String ADMIN = "admin";

    private PutawayTask task;
    private PutawayTaskDetail detail;

    @BeforeEach
    public void setUp() {
        super.setUp();
        task = new PutawayTask();
        task.setId(1L);
        task.setTaskNo(TASK_NO);
        task.setWarehouseCode(WAREHOUSE);
        task.setOwnerCode(OWNER);
        task.setStatus(PutawayStatus.PENDING.getCode());
        task.setExpectedQty(BigDecimal.valueOf(100));
        task.setPutawayQty(BigDecimal.ZERO);
        task.setAllowLocationChange("Y");
        task.setAllowQtyChange("Y");

        detail = new PutawayTaskDetail();
        detail.setId(1L);
        detail.setDetailNo(DETAIL_NO);
        detail.setTaskNo(TASK_NO);
        detail.setSkuCode(SKU);
        detail.setBatchNo(BATCH);
        detail.setSourceLocation(SRC_LOC);
        detail.setRecommendLocation(TGT_LOC);
        detail.setExpectedQty(BigDecimal.valueOf(100));
        detail.setPutawayQty(BigDecimal.ZERO);
        detail.setStatus(PutawayStatus.PENDING.getCode());

        when(taskMapper.selectByTaskNo(TASK_NO)).thenReturn(task);
        when(taskDetailMapper.selectByDetailNo(DETAIL_NO)).thenReturn(detail);
        doReturn(1).when(taskMapper).updateById(any(PutawayTask.class));
        doReturn(1).when(taskDetailMapper).updateById(any(PutawayTaskDetail.class));
        doReturn(1).when(recordMapper).insert(any(PutawayRecord.class));
        when(distributedLock.tryLock(anyString(), anyLong())).thenReturn(true);
    }

    @Test
    @DisplayName("完整上架流程：PENDING → ASSIGNED → CLAIMED → COMPLETED")
    void testFullPutawayFlow_Success() {
        // Step 1: 派发任务
        PutawayTask assigned = putawayTaskService.assignTask(TASK_NO, PICKER, ADMIN, "ZONE_A");
        assertEquals("ASSIGNED", assigned.getStatus());
        assertEquals(PICKER, assigned.getAssignee());

        // Step 2: 领取任务
        PutawayTask claimed = putawayTaskService.claimTask(TASK_NO, PICKER);
        assertEquals("CLAIMED", claimed.getStatus());
        assertEquals(PICKER, claimed.getAssignee());
        assertNotNull(claimed.getClaimTime());

        // Step 3: 上架确认（全部上架）
        PutawayRecord record =
                putawayTaskService.confirmPutaway(
                        TASK_NO, DETAIL_NO, TGT_LOC, BigDecimal.valueOf(100), BATCH, PICKER);

        assertNotNull(record);
        assertEquals(TGT_LOC, record.getTargetLocation());
        assertEquals(0, BigDecimal.valueOf(100).compareTo(record.getPutawayQty()));

        // 验证最终状态
        assertEquals(PutawayStatus.COMPLETED.getCode(), task.getStatus());
        assertEquals(PutawayStatus.COMPLETED.getCode(), detail.getStatus());
        assertNotNull(task.getCompleteTime());

        // 验证库存操作顺序：先扣减源库位，再增加目标库位
        InOrder inOrder = inOrder(inventoryService);
        inOrder.verify(inventoryService)
                .deductInventory(
                        eq(WAREHOUSE),
                        eq(SRC_LOC),
                        eq(SKU),
                        eq(BATCH),
                        eq(OWNER),
                        eq(BigDecimal.valueOf(100)),
                        anyString(),
                        eq(TASK_NO),
                        eq(PICKER));
        inOrder.verify(inventoryService)
                .addInventory(
                        eq(WAREHOUSE),
                        eq(TGT_LOC),
                        eq(SKU),
                        eq(BATCH),
                        eq(OWNER),
                        eq(BigDecimal.valueOf(100)),
                        anyString(),
                        eq(TASK_NO),
                        eq(PICKER));

        // 验证事件发布
        verify(eventPublisher, times(1)).publishTaskCompleted(any());
    }

    @Test
    @DisplayName("部分上架流程：CLAIMED → PARTIAL → COMPLETED")
    void testPartialPutawayFlow() {
        // 领取任务
        putawayTaskService.claimTask(TASK_NO, PICKER);

        // 第一次上架：50件
        putawayTaskService.confirmPutaway(
                TASK_NO, DETAIL_NO, TGT_LOC, BigDecimal.valueOf(50), BATCH, PICKER);
        assertEquals(PutawayStatus.PARTIAL.getCode(), task.getStatus());
        assertEquals(0, BigDecimal.valueOf(50).compareTo(task.getPutawayQty()));
        assertEquals(PutawayStatus.PARTIAL.getCode(), detail.getStatus());

        // 第二次上架：剩余50件
        detail.setPutawayQty(BigDecimal.valueOf(50));
        putawayTaskService.confirmPutaway(
                TASK_NO, DETAIL_NO, TGT_LOC, BigDecimal.valueOf(50), BATCH, PICKER);

        assertEquals(PutawayStatus.COMPLETED.getCode(), task.getStatus());
        assertEquals(0, BigDecimal.valueOf(100).compareTo(task.getPutawayQty()));

        // 验证两次库存操作
        verify(inventoryService, times(2))
                .deductInventory(any(), any(), any(), any(), any(), any(), any(), any(), any());
        verify(inventoryService, times(2))
                .addInventory(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("异常流程：CLAIMED → EXCEPTION → RESOLVED → COMPLETED")
    void testExceptionFlow() {
        // 领取任务
        putawayTaskService.claimTask(TASK_NO, PICKER);

        // 上报异常
        doReturn(1).when(exceptionLogMapper).insert(any(PutawayExceptionLog.class));
        PutawayExceptionLog exceptionLog =
                putawayTaskService.reportException(
                        TASK_NO, DETAIL_NO, "NO_LOCATION", "E001", "目标库位已满", PICKER);

        assertEquals("EXCEPTION", task.getStatus());
        assertEquals("PENDING", exceptionLog.getHandleStatus());
        verify(eventPublisher, times(1)).publishTaskException(any());

        // 解决异常
        when(exceptionLogMapper.selectById(1L)).thenReturn(exceptionLog);
        exceptionLog.setId(1L);
        PutawayExceptionLog resolved =
                putawayTaskService.resolveException(1L, "已分配新库位 B-01-01", ADMIN, "CLAIMED");

        assertEquals("RESOLVED", resolved.getHandleStatus());
        assertEquals("CLAIMED", task.getStatus());

        // 继续上架
        PutawayRecord record =
                putawayTaskService.confirmPutaway(
                        TASK_NO, DETAIL_NO, "B-01-01", BigDecimal.valueOf(100), BATCH, PICKER);

        assertNotNull(record);
        assertEquals(PutawayStatus.COMPLETED.getCode(), task.getStatus());
    }

    @Test
    @DisplayName("人工覆盖流程：推荐库位不可用 → 覆盖新库位 → 上架")
    void testOverrideFlow() {
        // 领取任务
        putawayTaskService.claimTask(TASK_NO, PICKER);

        // 人工覆盖库位
        when(reasonCodeMapper.selectByCode("LOC_FULL")).thenReturn(new PutawayReasonCode());
        doReturn(1).when(exceptionLogMapper).insert(any(PutawayExceptionLog.class));

        PutawayTaskDetail overridden =
                putawayTaskService.overrideLocation(DETAIL_NO, "B-02-01", "LOC_FULL", PICKER);

        assertEquals("B-02-01", overridden.getRecommendLocation());
        verify(eventPublisher, times(1)).publishTaskOverride(any());

        // 使用新库位上架
        PutawayRecord record =
                putawayTaskService.confirmPutaway(
                        TASK_NO, DETAIL_NO, "B-02-01", BigDecimal.valueOf(100), BATCH, PICKER);

        assertNotNull(record);
        assertEquals("B-02-01", record.getTargetLocation());
    }

    @Test
    @DisplayName("任务释放流程：ASSIGNED → PENDING → 重新领取")
    void testReleaseAndReclaimFlow() {
        // 派发任务
        putawayTaskService.assignTask(TASK_NO, PICKER, ADMIN, "ZONE_A");
        assertEquals("ASSIGNED", task.getStatus());

        // 释放任务
        PutawayTask released = putawayTaskService.releaseTask(TASK_NO, "人员请假", PICKER);
        assertEquals(PutawayStatus.PENDING.getCode(), released.getStatus());
        assertNull(released.getAssignee());

        // 重新领取
        PutawayTask reclaimed = putawayTaskService.claimTask(TASK_NO, "picker002");
        assertEquals("CLAIMED", reclaimed.getStatus());
        assertEquals("picker002", reclaimed.getAssignee());
    }

    @Test
    @DisplayName("源库位=目标库位时不执行库存扣减（直接上架场景）")
    void testSameLocation_NoDeduct() {
        detail.setSourceLocation(TGT_LOC);
        putawayTaskService.claimTask(TASK_NO, PICKER);

        putawayTaskService.confirmPutaway(
                TASK_NO, DETAIL_NO, TGT_LOC, BigDecimal.valueOf(100), BATCH, PICKER);

        // 源库位=目标库位时，不应调用deductInventory
        verify(inventoryService, never())
                .deductInventory(any(), any(), any(), any(), any(), any(), any(), any(), any());
        // 只调用addInventory
        verify(inventoryService, times(1))
                .addInventory(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }
}
