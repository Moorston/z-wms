package com.xwms.core.putaway.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.xwms.common.lock.DistributedLock;
import com.xwms.core.inventory.service.InventoryService;
import com.xwms.core.putaway.entity.*;
import com.xwms.core.putaway.enums.PutawayStatus;
import com.xwms.core.putaway.event.PutawayEventPublisher;
import com.xwms.core.putaway.mapper.*;
import com.xwms.core.support.BaseTest;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** 上架任务服务单元测试（Sprint 3） 核心测试：任务派发/领取/释放/上架确认（含库存扣减）/人工覆盖/异常处理 */
class PutawayTaskServiceTest extends BaseTest {

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
    private static final String OPERATOR = "user001";

    private PutawayTask buildTask(String status) {
        PutawayTask task = new PutawayTask();
        task.setId(1L);
        task.setTaskNo(TASK_NO);
        task.setWarehouseCode(WAREHOUSE);
        task.setOwnerCode(OWNER);
        task.setStatus(status);
        task.setExpectedQty(BigDecimal.valueOf(100));
        task.setPutawayQty(BigDecimal.ZERO);
        task.setAllowLocationChange("Y");
        task.setAllowQtyChange("Y");
        return task;
    }

    private PutawayTaskDetail buildDetail(String status) {
        PutawayTaskDetail detail = new PutawayTaskDetail();
        detail.setId(1L);
        detail.setDetailNo(DETAIL_NO);
        detail.setTaskNo(TASK_NO);
        detail.setSkuCode(SKU);
        detail.setBatchNo(BATCH);
        detail.setSourceLocation(SRC_LOC);
        detail.setRecommendLocation(TGT_LOC);
        detail.setExpectedQty(BigDecimal.valueOf(100));
        detail.setPutawayQty(BigDecimal.ZERO);
        detail.setStatus(status);
        return detail;
    }

    // ============================================================
    // 任务派发测试
    // ============================================================

    @Test
    @DisplayName("任务派发 - 正常派发成功")
    void testAssignTask_Success() {
        PutawayTask task = buildTask(PutawayStatus.PENDING.getCode());
        when(taskMapper.selectByTaskNo(TASK_NO)).thenReturn(task);
        doReturn(1).when(taskMapper).updateById(any(PutawayTask.class));

        PutawayTask result = putawayTaskService.assignTask(TASK_NO, "picker01", "admin", "ZONE_A");

        assertNotNull(result);
        assertEquals("ASSIGNED", result.getStatus());
        assertEquals("picker01", result.getAssignee());
        assertEquals("admin", result.getAssigner());
        assertEquals("ZONE_A", result.getWorkZone());
        assertNotNull(result.getAssignTime());
    }

    @Test
    @DisplayName("任务派发 - 任务不存在抛异常")
    void testAssignTask_NotFound() {
        when(taskMapper.selectByTaskNo(TASK_NO)).thenReturn(null);
        assertExceptionMessage(
                () -> putawayTaskService.assignTask(TASK_NO, "picker01", "admin", null), "上架任务不存在");
    }

    @Test
    @DisplayName("任务派发 - 非PENDING状态拒绝")
    void testAssignTask_InvalidStatus() {
        PutawayTask task = buildTask("COMPLETED");
        when(taskMapper.selectByTaskNo(TASK_NO)).thenReturn(task);

        assertExceptionMessage(
                () -> putawayTaskService.assignTask(TASK_NO, "picker01", "admin", null),
                "任务状态不允许派发");
    }

    // ============================================================
    // 任务领取测试
    // ============================================================

    @Test
    @DisplayName("任务领取 - PENDING状态领取成功")
    void testClaimTask_FromPending() {
        PutawayTask task = buildTask(PutawayStatus.PENDING.getCode());
        when(taskMapper.selectByTaskNo(TASK_NO)).thenReturn(task);
        doReturn(1).when(taskMapper).updateById(any(PutawayTask.class));

        PutawayTask result = putawayTaskService.claimTask(TASK_NO, OPERATOR);

        assertEquals("CLAIMED", result.getStatus());
        assertEquals(OPERATOR, result.getAssignee());
        assertNotNull(result.getClaimTime());
        assertNotNull(result.getStartTime());
    }

    @Test
    @DisplayName("任务领取 - ASSIGNED状态领取成功")
    void testClaimTask_FromAssigned() {
        PutawayTask task = buildTask("ASSIGNED");
        task.setAssignee("otherPicker");
        when(taskMapper.selectByTaskNo(TASK_NO)).thenReturn(task);
        doReturn(1).when(taskMapper).updateById(any(PutawayTask.class));

        PutawayTask result = putawayTaskService.claimTask(TASK_NO, OPERATOR);
        assertEquals("CLAIMED", result.getStatus());
    }

    @Test
    @DisplayName("任务领取 - COMPLETED状态拒绝")
    void testClaimTask_CompletedStatus() {
        PutawayTask task = buildTask("COMPLETED");
        when(taskMapper.selectByTaskNo(TASK_NO)).thenReturn(task);

        assertExceptionMessage(() -> putawayTaskService.claimTask(TASK_NO, OPERATOR), "任务状态不允许领取");
    }

    // ============================================================
    // 任务释放测试
    // ============================================================

    @Test
    @DisplayName("任务释放 - CLAIMED状态释放成功")
    void testReleaseTask_FromClaimed() {
        PutawayTask task = buildTask("CLAIMED");
        task.setAssignee(OPERATOR);
        task.setPutawayQty(BigDecimal.ZERO);
        when(taskMapper.selectByTaskNo(TASK_NO)).thenReturn(task);
        doReturn(1).when(taskMapper).updateById(any(PutawayTask.class));

        PutawayTask result = putawayTaskService.releaseTask(TASK_NO, "临时有事", OPERATOR);

        assertEquals(PutawayStatus.PENDING.getCode(), result.getStatus());
        assertNull(result.getAssignee());
        assertNull(result.getClaimTime());
    }

    @Test
    @DisplayName("任务释放 - 已开始上架拒绝释放")
    void testReleaseTask_AlreadyStarted() {
        PutawayTask task = buildTask("CLAIMED");
        task.setPutawayQty(BigDecimal.TEN);
        when(taskMapper.selectByTaskNo(TASK_NO)).thenReturn(task);

        assertExceptionMessage(
                () -> putawayTaskService.releaseTask(TASK_NO, "原因", OPERATOR), "已开始上架的任务不允许释放");
    }

    // ============================================================
    // 上架确认测试（含库存扣减）
    // ============================================================

    @Test
    @DisplayName("上架确认 - 正常上架成功，含库存扣减")
    void testConfirmPutaway_Success() {
        PutawayTask task = buildTask("CLAIMED");
        PutawayTaskDetail detail = buildDetail(PutawayStatus.PENDING.getCode());

        when(taskMapper.selectByTaskNo(TASK_NO)).thenReturn(task);
        when(taskDetailMapper.selectByDetailNo(DETAIL_NO)).thenReturn(detail);
        doReturn(1).when(recordMapper).insert(any(PutawayRecord.class));
        doReturn(1).when(taskDetailMapper).updateById(any(PutawayTaskDetail.class));
        doReturn(1).when(taskMapper).updateById(any(PutawayTask.class));
        when(distributedLock.tryLock(anyString(), anyLong())).thenReturn(true);
        TransactionSynchronizationManager.initSynchronization();

        try {
            PutawayRecord result =
                    putawayTaskService.confirmPutaway(
                            TASK_NO, DETAIL_NO, TGT_LOC, BigDecimal.valueOf(50), BATCH, OPERATOR);

            assertNotNull(result);
            assertEquals(TGT_LOC, result.getTargetLocation());
            assertEquals(0, BigDecimal.valueOf(50).compareTo(result.getPutawayQty()));

            // 验证库存扣减和增加
            verify(inventoryService, times(1))
                    .deductInventory(
                            eq(WAREHOUSE),
                            eq(SRC_LOC),
                            eq(SKU),
                            eq(BATCH),
                            eq(OWNER),
                            eq(BigDecimal.valueOf(50)),
                            anyString(),
                            eq(TASK_NO),
                            eq(OPERATOR));
            verify(inventoryService, times(1))
                    .addInventory(
                            eq(WAREHOUSE),
                            eq(TGT_LOC),
                            eq(SKU),
                            eq(BATCH),
                            eq(OWNER),
                            eq(BigDecimal.valueOf(50)),
                            anyString(),
                            eq(TASK_NO),
                            eq(OPERATOR));
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    @DisplayName("上架确认 - 上架数量超过剩余数量拒绝")
    void testConfirmPutaway_ExceedQty() {
        PutawayTask task = buildTask("CLAIMED");
        PutawayTaskDetail detail = buildDetail(PutawayStatus.PENDING.getCode());
        detail.setPutawayQty(BigDecimal.valueOf(80));

        when(taskMapper.selectByTaskNo(TASK_NO)).thenReturn(task);
        when(taskDetailMapper.selectByDetailNo(DETAIL_NO)).thenReturn(detail);
        when(distributedLock.tryLock(anyString(), anyLong())).thenReturn(true);

        assertExceptionMessage(
                () ->
                        putawayTaskService.confirmPutaway(
                                TASK_NO,
                                DETAIL_NO,
                                TGT_LOC,
                                BigDecimal.valueOf(50),
                                BATCH,
                                OPERATOR),
                "上架数量超过剩余数量");
    }

    @Test
    @DisplayName("上架确认 - 不允许修改库位时拒绝")
    void testConfirmPutaway_LocationChangeNotAllowed() {
        PutawayTask task = buildTask("CLAIMED");
        task.setAllowLocationChange("N");
        PutawayTaskDetail detail = buildDetail(PutawayStatus.PENDING.getCode());
        detail.setRecommendLocation(TGT_LOC);

        when(taskMapper.selectByTaskNo(TASK_NO)).thenReturn(task);
        when(taskDetailMapper.selectByDetailNo(DETAIL_NO)).thenReturn(detail);
        when(distributedLock.tryLock(anyString(), anyLong())).thenReturn(true);

        assertExceptionMessage(
                () ->
                        putawayTaskService.confirmPutaway(
                                TASK_NO, DETAIL_NO, "OTHER-LOC", BigDecimal.TEN, BATCH, OPERATOR),
                "不允许修改推荐库位");
    }

    @Test
    @DisplayName("上架确认 - 全部上架后任务状态为COMPLETED")
    void testConfirmPutaway_FullQty_CompletedStatus() {
        PutawayTask task = buildTask("CLAIMED");
        PutawayTaskDetail detail = buildDetail(PutawayStatus.PENDING.getCode());

        when(taskMapper.selectByTaskNo(TASK_NO)).thenReturn(task);
        when(taskDetailMapper.selectByDetailNo(DETAIL_NO)).thenReturn(detail);
        doReturn(1).when(recordMapper).insert(any(PutawayRecord.class));
        doReturn(1).when(taskDetailMapper).updateById(any(PutawayTaskDetail.class));
        doReturn(1).when(taskMapper).updateById(any(PutawayTask.class));
        when(distributedLock.tryLock(anyString(), anyLong())).thenReturn(true);
        TransactionSynchronizationManager.initSynchronization();

        try {
            putawayTaskService.confirmPutaway(
                    TASK_NO, DETAIL_NO, TGT_LOC, BigDecimal.valueOf(100), BATCH, OPERATOR);

            assertEquals(PutawayStatus.COMPLETED.getCode(), task.getStatus());
            assertNotNull(task.getCompleteTime());
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    // ============================================================
    // 人工覆盖测试
    // ============================================================

    @Test
    @DisplayName("人工覆盖 - 正常覆盖成功")
    void testOverrideLocation_Success() {
        PutawayTaskDetail detail = buildDetail(PutawayStatus.PENDING.getCode());
        detail.setRecommendLocation("OLD-LOC");

        when(taskDetailMapper.selectByDetailNo(DETAIL_NO)).thenReturn(detail);
        when(reasonCodeMapper.selectByCode("LOC_FULL")).thenReturn(new PutawayReasonCode());
        doReturn(1).when(taskDetailMapper).updateById(any(PutawayTaskDetail.class));
        doReturn(1).when(exceptionLogMapper).insert(any(PutawayExceptionLog.class));

        PutawayTaskDetail result =
                putawayTaskService.overrideLocation(DETAIL_NO, "NEW-LOC", "LOC_FULL", OPERATOR);

        assertEquals("NEW-LOC", result.getRecommendLocation());
    }

    @Test
    @DisplayName("人工覆盖 - 原因代码不存在抛异常")
    void testOverrideLocation_ReasonCodeNotFound() {
        PutawayTaskDetail detail = buildDetail(PutawayStatus.PENDING.getCode());
        when(taskDetailMapper.selectByDetailNo(DETAIL_NO)).thenReturn(detail);
        when(reasonCodeMapper.selectByCode("INVALID")).thenReturn(null);

        assertExceptionMessage(
                () ->
                        putawayTaskService.overrideLocation(
                                DETAIL_NO, "NEW-LOC", "INVALID", OPERATOR),
                "原因代码不存在");
    }

    // ============================================================
    // 异常处理测试
    // ============================================================

    @Test
    @DisplayName("异常上报 - 正常上报成功")
    void testReportException_Success() {
        PutawayTask task = buildTask("CLAIMED");
        PutawayTaskDetail detail = buildDetail(PutawayStatus.PENDING.getCode());

        when(taskMapper.selectByTaskNo(TASK_NO)).thenReturn(task);
        when(taskDetailMapper.selectByDetailNo(DETAIL_NO)).thenReturn(detail);
        doReturn(1).when(taskMapper).updateById(any(PutawayTask.class));
        doReturn(1).when(exceptionLogMapper).insert(any(PutawayExceptionLog.class));

        PutawayExceptionLog result =
                putawayTaskService.reportException(
                        TASK_NO, DETAIL_NO, "NO_LOCATION", "E001", "无可用库位", OPERATOR);

        assertNotNull(result);
        assertEquals("NO_LOCATION", result.getExceptionType());
        assertEquals("PENDING", result.getHandleStatus());
        assertEquals("EXCEPTION", task.getStatus());
    }

    @Test
    @DisplayName("异常解决 - 正常解决成功")
    void testResolveException_Success() {
        PutawayExceptionLog log = new PutawayExceptionLog();
        log.setId(1L);
        log.setTaskNo(TASK_NO);
        log.setHandleStatus("PENDING");

        PutawayTask task = buildTask("EXCEPTION");

        when(exceptionLogMapper.selectById(1L)).thenReturn(log);
        when(exceptionLogMapper.updateById(any(PutawayExceptionLog.class))).thenReturn(1);
        when(taskMapper.selectByTaskNo(TASK_NO)).thenReturn(task);
        doReturn(1).when(taskMapper).updateById(any(PutawayTask.class));

        PutawayExceptionLog result =
                putawayTaskService.resolveException(1L, "已找到新库位", "admin", "CLAIMED");

        assertEquals("RESOLVED", result.getHandleStatus());
        assertEquals("CLAIMED", task.getStatus());
    }
}
