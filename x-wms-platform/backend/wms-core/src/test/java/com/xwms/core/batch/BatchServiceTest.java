package com.xwms.core.batch;

import com.xwms.common.exception.BizException;
import com.xwms.core.batch.entity.Batch;
import com.xwms.core.batch.mapper.BatchMapper;
import com.xwms.core.batch.service.BatchService;
import com.xwms.core.support.BaseTest;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 批次服务单元测试
 * 核心测试：创建/质检/冻结/近效期查询/FEFO排序
 */
class BatchServiceTest extends BaseTest {

    @Mock
    private BatchMapper batchMapper;
    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private BatchService batchService;

    @Test
    void testCreate_success()
    {
        when(batchMapper.selectOne(any())).thenReturn(null);
        when(batchMapper.insert(any(Batch.class))).thenReturn(1);
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(null);

        Batch batch = new Batch();
        batch.setSku("SKU001");
        batch.setBatchNo("B20260818");
        batch.setWarehouse("WH01");
        Batch result = batchService.create(batch);

        assertEquals("ACTIVE", result.getStatus());
        verify(kafkaTemplate, times(1)).send(eq("wms-batch-events"), anyString(), any());
    }

    @Test
    void testCreate_duplicate()
    {
        Batch exist = new Batch();
        when(batchMapper.selectOne(any())).thenReturn(exist);

        Batch batch = new Batch();
        batch.setSku("SKU001");
        batch.setBatchNo("B001");
        assertThrows(BizException.class, () -> batchService.create(batch));
    }

    @Test
    void testCompleteQc_passed()
    {
        Batch batch = new Batch();
        batch.setBatchNo("B001");
        batch.setSku("SKU001");
        batch.setWarehouse("WH01");
        batch.setStatus("ACTIVE");
        when(batchMapper.selectOne(any())).thenReturn(batch);
        when(batchMapper.updateById(any(Batch.class))).thenReturn(1);
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(null);

        batchService.completeQc("B001", "SKU001", "WH01", true, "QC001");

        assertEquals("PASSED", batch.getQcStatus());
        assertEquals("QC001", batch.getQcNo());
    }

    @Test
    void testCompleteQc_failed()
    {
        Batch batch = new Batch();
        batch.setBatchNo("B001");
        batch.setSku("SKU001");
        batch.setWarehouse("WH01");
        when(batchMapper.selectOne(any())).thenReturn(batch);
        when(batchMapper.updateById(any(Batch.class))).thenReturn(1);
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(null);

        batchService.completeQc("B001", "SKU001", "WH01", false, "QC001");

        assertEquals("FAILED", batch.getQcStatus());
        assertEquals("QUARANTINE", batch.getStatus());
    }

    @Test
    void testFreeze()
    {
        Batch batch = new Batch();
        batch.setBatchNo("B001");
        batch.setSku("SKU001");
        batch.setWarehouse("WH01");
        when(batchMapper.selectOne(any())).thenReturn(batch);
        when(batchMapper.updateById(any(Batch.class))).thenReturn(1);
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(null);

        batchService.freeze("B001", "SKU001", "WH01", true);
        assertEquals("FROZEN", batch.getStatus());

        batchService.freeze("B001", "SKU001", "WH01", false);
        assertEquals("ACTIVE", batch.getStatus());
    }

    @Test
    void testListBySku_fefoOrder()
    {
        Batch b1 = new Batch();
        b1.setBatchNo("B001");
        b1.setExpireDate(LocalDate.now().plusDays(30));
        Batch b2 = new Batch();
        b2.setBatchNo("B002");
        b2.setExpireDate(LocalDate.now().plusDays(90));
        when(batchMapper.selectList(any())).thenReturn(List.of(b1, b2));

        List<Batch> result = batchService.listBySku("SKU001", "WH01");

        // 按效期升序，近效期在前
        assertEquals("B001", result.get(0).getBatchNo());
        assertEquals("B002", result.get(1).getBatchNo());
    }

    @Test
    void testListNearExpire()
    {
        Batch b1 = new Batch();
        b1.setExpireDate(LocalDate.now().plusDays(20));
        when(batchMapper.selectList(any())).thenReturn(List.of(b1));

        List<Batch> result = batchService.listNearExpire("WH01", 30);
        assertFalse(result.isEmpty());
    }
}
