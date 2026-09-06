package com.xwms.base.batch.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.base.batch.entity.*;
import com.xwms.base.batch.mapper.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 批次属性管理核心服务 包含: 批次属性定义/批次属性值/批次追踪规则/批次追踪日志 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BatchAttributeService {

    private final BatchAttributeMapper batchAttributeMapper;
    private final BatchValueMapper batchValueMapper;
    private final BatchTraceRuleMapper batchTraceRuleMapper;
    private final BatchTraceLogMapper batchTraceLogMapper;

    private static final AtomicInteger TRACE_SEQ = new AtomicInteger(0);
    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    // ============================================================
    // 1. 批次属性定义
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public BatchAttribute createAttribute(BatchAttribute attribute) {
        batchAttributeMapper.insert(attribute);
        log.info("创建批次属性: {}={}", attribute.getAttrCode(), attribute.getAttrName());
        return attribute;
    }

    @Transactional(rollbackFor = Exception.class)
    public BatchAttribute updateAttribute(BatchAttribute attribute) {
        batchAttributeMapper.updateById(attribute);
        return attribute;
    }

    public Page<BatchAttribute> pageAttributes(
            Page<BatchAttribute> page, String attrCategory, String attrType, Integer enabled) {
        LambdaQueryWrapper<BatchAttribute> wrapper = new LambdaQueryWrapper<>();
        if (attrCategory != null) wrapper.eq(BatchAttribute::getAttrCategory, attrCategory);
        if (attrType != null) wrapper.eq(BatchAttribute::getAttrType, attrType);
        if (enabled != null) wrapper.eq(BatchAttribute::getEnabled, enabled);
        wrapper.orderByAsc(BatchAttribute::getAttrCategory)
                .orderByAsc(BatchAttribute::getSortOrder);
        return batchAttributeMapper.selectPage(page, wrapper);
    }

    public List<BatchAttribute> getAttributesByCategory(String category) {
        return batchAttributeMapper.selectByCategory(category);
    }

    public List<BatchAttribute> getAllEnabledAttributes() {
        return batchAttributeMapper.selectAllEnabled();
    }

    // ============================================================
    // 2. 批次属性值
    // ============================================================

    /** 保存批次属性值（批量） */
    @Transactional(rollbackFor = Exception.class)
    public int saveBatchValues(
            String batchNo,
            String skuCode,
            String warehouseCode,
            Map<String, String> attrValues,
            String sourceType,
            String sourceRef) {
        int count = 0;
        for (Map.Entry<String, String> entry : attrValues.entrySet()) {
            BatchAttribute attr =
                    batchAttributeMapper.selectOne(
                            new LambdaQueryWrapper<BatchAttribute>()
                                    .eq(BatchAttribute::getAttrCode, entry.getKey()));
            if (attr == null) continue;

            BatchValue existing =
                    batchValueMapper.selectOne(
                            new LambdaQueryWrapper<BatchValue>()
                                    .eq(BatchValue::getBatchNo, batchNo)
                                    .eq(BatchValue::getSkuCode, skuCode)
                                    .eq(BatchValue::getAttrCode, entry.getKey()));

            if (existing != null) {
                existing.setAttrValue(entry.getValue());
                existing.setSourceType(sourceType);
                existing.setSourceRef(sourceRef);
                batchValueMapper.updateById(existing);
            } else {
                BatchValue value = new BatchValue();
                value.setBatchNo(batchNo);
                value.setSkuCode(skuCode);
                value.setWarehouseCode(warehouseCode);
                value.setAttrCode(entry.getKey());
                value.setAttrName(attr.getAttrName());
                value.setAttrValue(entry.getValue());
                value.setSourceType(sourceType);
                value.setSourceRef(sourceRef);
                batchValueMapper.insert(value);
            }
            count++;
        }
        log.info("保存批次属性值: batch={}, sku={}, 数量={}", batchNo, skuCode, count);
        return count;
    }

    /** 获取批次所有属性值 */
    public Map<String, String> getBatchValues(String batchNo, String skuCode) {
        List<BatchValue> values = batchValueMapper.selectByBatchAndSku(batchNo, skuCode);
        return values.stream()
                .collect(
                        Collectors.toMap(
                                BatchValue::getAttrCode,
                                v -> v.getAttrValue() != null ? v.getAttrValue() : "",
                                (a, b) -> a));
    }

    public List<BatchValue> getBatchValueList(String batchNo, String skuCode) {
        return batchValueMapper.selectByBatchAndSku(batchNo, skuCode);
    }

    public List<BatchValue> getBatchAllValues(String batchNo) {
        return batchValueMapper.selectByBatch(batchNo);
    }

    /** 校验必填属性 */
    public List<String> validateRequiredAttributes(String batchNo, String skuCode) {
        List<BatchAttribute> requiredAttrs =
                batchAttributeMapper.selectList(
                        new LambdaQueryWrapper<BatchAttribute>()
                                .eq(BatchAttribute::getIsRequired, 1)
                                .eq(BatchAttribute::getEnabled, 1));
        Map<String, String> values = getBatchValues(batchNo, skuCode);
        return requiredAttrs.stream()
                .filter(
                        attr ->
                                values.get(attr.getAttrCode()) == null
                                        || values.get(attr.getAttrCode()).trim().isEmpty())
                .map(BatchAttribute::getAttrName)
                .collect(Collectors.toList());
    }

    // ============================================================
    // 3. 批次追踪规则
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public BatchTraceRule createTraceRule(BatchTraceRule rule) {
        batchTraceRuleMapper.insert(rule);
        log.info("创建批次追踪规则: {}={}", rule.getRuleCode(), rule.getRuleName());
        return rule;
    }

    public Page<BatchTraceRule> pageTraceRules(
            Page<BatchTraceRule> page, String ruleType, Integer enabled) {
        LambdaQueryWrapper<BatchTraceRule> wrapper = new LambdaQueryWrapper<>();
        if (ruleType != null) wrapper.eq(BatchTraceRule::getRuleType, ruleType);
        if (enabled != null) wrapper.eq(BatchTraceRule::getEnabled, enabled);
        wrapper.orderByAsc(BatchTraceRule::getRuleCode);
        return batchTraceRuleMapper.selectPage(page, wrapper);
    }

    public List<BatchTraceRule> getTraceRulesByType(String ruleType) {
        return batchTraceRuleMapper.selectByType(ruleType);
    }

    // ============================================================
    // 4. 批次追踪日志
    // ============================================================

    /** 记录批次追踪日志 */
    @Transactional(rollbackFor = Exception.class)
    public BatchTraceLog recordTraceLog(
            String batchNo,
            String skuCode,
            String operationType,
            String operationNo,
            String fromLocation,
            String toLocation,
            BigDecimal quantity,
            String operator,
            String traceData,
            String traceId) {
        BatchTraceLog traceLog = new BatchTraceLog();
        traceLog.setTraceNo(generateTraceNo());
        traceLog.setBatchNo(batchNo);
        traceLog.setSkuCode(skuCode);
        traceLog.setOperationType(operationType);
        traceLog.setOperationNo(operationNo);
        traceLog.setFromLocation(fromLocation);
        traceLog.setToLocation(toLocation);
        traceLog.setQuantity(quantity);
        traceLog.setOperator(operator);
        traceLog.setOperationTime(LocalDateTime.now());
        traceLog.setTraceData(traceData);
        traceLog.setTraceId(traceId);
        batchTraceLogMapper.insert(traceLog);
        log.info("记录批次追踪: {} {} {} -> {}", batchNo, operationType, fromLocation, toLocation);
        return traceLog;
    }

    public Page<BatchTraceLog> pageTraceLogs(
            Page<BatchTraceLog> page,
            String batchNo,
            String skuCode,
            String operationType,
            String operationNo) {
        LambdaQueryWrapper<BatchTraceLog> wrapper = new LambdaQueryWrapper<>();
        if (batchNo != null) wrapper.eq(BatchTraceLog::getBatchNo, batchNo);
        if (skuCode != null) wrapper.eq(BatchTraceLog::getSkuCode, skuCode);
        if (operationType != null) wrapper.eq(BatchTraceLog::getOperationType, operationType);
        if (operationNo != null) wrapper.eq(BatchTraceLog::getOperationNo, operationNo);
        wrapper.orderByDesc(BatchTraceLog::getOperationTime);
        return batchTraceLogMapper.selectPage(page, wrapper);
    }

    /** 正向追踪：从入库到当前的完整链路 */
    public List<BatchTraceLog> forwardTrace(String batchNo, String skuCode) {
        return batchTraceLogMapper.selectByBatchAndSku(batchNo, skuCode);
    }

    /** 反向追踪：从当前追溯到源头 */
    public List<BatchTraceLog> backwardTrace(String batchNo, String skuCode) {
        List<BatchTraceLog> logs = batchTraceLogMapper.selectByBatchAndSku(batchNo, skuCode);
        // 反向排序
        return logs.stream()
                .sorted((a, b) -> b.getOperationTime().compareTo(a.getOperationTime()))
                .collect(Collectors.toList());
    }

    // ============================================================
    // 工具方法
    // ============================================================

    private String generateTraceNo() {
        return "BTR"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", TRACE_SEQ.incrementAndGet() % 1000);
    }
}
