package com.xwms.core.batch;

import com.xwms.core.batch.entity.Batch;
import com.xwms.core.batch.mapper.BatchMapper;
import com.xwms.core.batch.service.BatchService;
import com.xwms.core.support.IntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 批次全链路追踪集成测试
 * 验证：
 * 1. 批次创建唯一性
 * 2. 质检状态流转
 * 3. 冻结/解冻
 * 4. FEFO近效期先出排序
 * 5. 近效期预警查询
 */
class BatchTraceIntegrationTest extends IntegrationTestBase {

    @Autowired
    private BatchService batchService;
    @Autowired
    private BatchMapper batchMapper;

    @BeforeEach
    void setUp()
    {
        batchMapper.delete(null);
    }

    @Test
    void testBatchLifecycle()
    {
        // 1. 创建批次
        Batch batch = new Batch();
        batch.setSku("BATCH-SKU-001");
        batch.setBatchNo("B-TRACE-001");
        batch.setWarehouse("WH01");
        batch.setProduceDate(LocalDate.now().minusDays(10));
        batch.setExpireDate(LocalDate.now().plusDays(365));
        Batch created = batchService.create(batch);
        assertEquals("ACTIVE", created.getStatus());
        assertEquals("PENDING", created.getQcStatus());

        // 2. 质检通过
        batchService.completeQc("B-TRACE-001", "BATCH-SKU-001", "WH01", true, "QC-001");
        Batch qcPassed = getBatch("B-TRACE-001");
        assertEquals("PASSED", qcPassed.getQcStatus());
        assertEquals("QC-001", qcPassed.getQcNo());

        // 3. 冻结
        batchService.freeze("B-TRACE-001", "BATCH-SKU-001", "WH01", true);
        Batch frozen = getBatch("B-TRACE-001");
        assertEquals("FROZEN", frozen.getStatus());

        // 4. 解冻
        batchService.freeze("B-TRACE-001", "BATCH-SKU-001", "WH01", false);
        Batch unfrozen = getBatch("B-TRACE-001");
        assertEquals("ACTIVE", unfrozen.getStatus());
    }

    @Test
    void testFefoOrdering()
    {
        // 创建3个批次，效期不同
        createBatch("B-FEFO-01", LocalDate.now().plusDays(90));   // 90天后过期
        createBatch("B-FEFO-02", LocalDate.now().plusDays(30));   // 30天后过期（最近）
        createBatch("B-FEFO-03", LocalDate.now().plusDays(180));  // 180天后过期

        List<Batch> result = batchService.listBySku("FEFO-SKU", "WH01");

        // FEFO：近效期先出，应该按expireDate升序
        assertEquals("B-FEFO-02", result.get(0).getBatchNo(), "最近效期应该排第一");
        assertEquals("B-FEFO-01", result.get(1).getBatchNo());
        assertEquals("B-FEFO-03", result.get(2).getBatchNo());
    }

    @Test
    void testNearExpireQuery()
    {
        createBatch("B-NEAR-01", LocalDate.now().plusDays(20));  // 20天，在30天阈值内
        createBatch("B-NEAR-02", LocalDate.now().plusDays(60));  // 60天，不在阈值内

        List<Batch> nearExpire = batchService.listNearExpire("WH01", 30);
        assertEquals(1, nearExpire.size());
        assertEquals("B-NEAR-01", nearExpire.get(0).getBatchNo());
    }

    @Test
    void testDuplicateBatch()
    {
        createBatch("B-DUP-001", LocalDate.now().plusDays(100));

        Batch dup = new Batch();
        dup.setSku("DUP-SKU");
        dup.setBatchNo("B-DUP-001");
        dup.setWarehouse("WH01");

        assertThrows(Exception.class, () -> batchService.create(dup),
                "重复批次应该创建失败");
    }

    @Test
    void testQcFailed_quarantine()
    {
        createBatch("B-QC-FAIL", LocalDate.now().plusDays(100));
        batchService.completeQc("B-QC-FAIL", "QC-SKU", "WH01", false, "QC-FAIL-001");

        Batch failed = getBatch("B-QC-FAIL");
        assertEquals("FAILED", failed.getQcStatus());
        assertEquals("QUARANTINE", failed.getStatus(), "质检失败应该进入隔离状态");
    }

    private void createBatch(String batchNo, LocalDate expireDate)
    {
        Batch batch = new Batch();
        batch.setSku(batchNo.contains("FEFO") ? "FEFO-SKU" :
                batchNo.contains("NEAR") ? "NEAR-SKU" :
                batchNo.contains("DUP") ? "DUP-SKU" : "QC-SKU");
        batch.setBatchNo(batchNo);
        batch.setWarehouse("WH01");
        batch.setExpireDate(expireDate);
        batchService.create(batch);
    }

    private Batch getBatch(String batchNo)
    {
        return batchMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Batch>()
                        .eq(Batch::getBatchNo, batchNo));
    }
}
