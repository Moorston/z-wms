package com.xwms.core.batch.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import com.xwms.common.exception.BizException;
import com.xwms.core.batch.entity.Batch;
import com.xwms.core.batch.mapper.BatchMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 批次服务 批次全链路追踪核心： 1. 批次创建（入库时） 2. 批次属性管理 3. 批次状态变更（事件溯源） 4. 批次追踪查询 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BatchService {

    private final BatchMapper batchMapper;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    /** 创建批次 */
    @Transactional(rollbackFor = Exception.class)
    public Batch create(Batch batch) {
        // 批号唯一性校验
        Batch exist =
                batchMapper.selectOne(
                        new LambdaQueryWrapper<Batch>()
                                .eq(Batch::getSku, batch.getSku())
                                .eq(Batch::getBatchNo, batch.getBatchNo())
                                .eq(Batch::getWarehouse, batch.getWarehouse()));
        if (exist != null) {
            throw new BizException("批次已存在: " + batch.getBatchNo());
        }
        batch.setStatus("ACTIVE");
        batchMapper.insert(batch);
        // 发布批次创建事件（事件溯源）
        publishBatchEvent("BATCH_CREATED", batch);
        log.info("批次创建: sku={}, batchNo={}", batch.getSku(), batch.getBatchNo());
        return batch;
    }

    /** 批次质检完成 */
    @Transactional(rollbackFor = Exception.class)
    public void completeQc(
            String batchNo, String sku, String warehouse, boolean passed, String qcNo) {
        Batch batch = getBatch(batchNo, sku, warehouse);
        batch.setQcStatus(passed ? "PASSED" : "FAILED");
        batch.setQcNo(qcNo);
        if (!passed) {
            batch.setStatus("QUARANTINE"); // 不合格隔离
        }
        batchMapper.updateById(batch);
        publishBatchEvent(passed ? "BATCH_QC_PASSED" : "BATCH_QC_FAILED", batch);
    }

    /** 批次冻结/解冻 */
    @Transactional(rollbackFor = Exception.class)
    public void freeze(String batchNo, String sku, String warehouse, boolean freeze) {
        Batch batch = getBatch(batchNo, sku, warehouse);
        batch.setStatus(freeze ? "FROZEN" : "ACTIVE");
        batchMapper.updateById(batch);
        publishBatchEvent(freeze ? "BATCH_FROZEN" : "BATCH_UNFROZEN", batch);
    }

    /** 查询SKU的所有批次（按效期升序，FEFO） */
    public List<Batch> listBySku(String sku, String warehouse) {
        return batchMapper.selectList(
                new LambdaQueryWrapper<Batch>()
                        .eq(Batch::getSku, sku)
                        .eq(Batch::getWarehouse, warehouse)
                        .eq(Batch::getStatus, "ACTIVE")
                        .orderByAsc(Batch::getExpireDate));
    }

    /** 近效期批次查询 */
    public List<Batch> listNearExpire(String warehouse, int days) {
        LocalDate threshold = LocalDate.now().plusDays(days);
        return batchMapper.selectList(
                new LambdaQueryWrapper<Batch>()
                        .eq(Batch::getWarehouse, warehouse)
                        .le(Batch::getExpireDate, threshold)
                        .eq(Batch::getStatus, "ACTIVE")
                        .orderByAsc(Batch::getExpireDate));
    }

    private Batch getBatch(String batchNo, String sku, String warehouse) {
        Batch batch =
                batchMapper.selectOne(
                        new LambdaQueryWrapper<Batch>()
                                .eq(Batch::getBatchNo, batchNo)
                                .eq(Batch::getSku, sku)
                                .eq(Batch::getWarehouse, warehouse));
        if (batch == null) {
            throw new BizException("批次不存在: " + batchNo);
        }
        return batch;
    }

    /** 发布批次事件到Kafka（事件溯源） */
    private void publishBatchEvent(String eventType, Batch batch) {
        kafkaTemplate.send(
                "wms-batch-events",
                batch.getBatchNo(),
                java.util.Map.of(
                        "eventType", eventType,
                        "batchNo", batch.getBatchNo(),
                        "sku", batch.getSku(),
                        "warehouse", batch.getWarehouse(),
                        "timestamp", java.time.LocalDateTime.now().toString()));
    }
}
