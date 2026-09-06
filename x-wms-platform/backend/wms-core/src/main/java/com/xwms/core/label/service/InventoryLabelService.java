package com.xwms.core.label.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.core.label.entity.*;
import com.xwms.core.label.mapper.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 库存标签/条码管理核心服务 核心能力: 标签模板/条码规则/标签任务/条码记录 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryLabelService {

    private final LabelTemplateMapper templateMapper;
    private final BarcodeRuleMapper ruleMapper;
    private final LabelTaskMapper taskMapper;
    private final BarcodeRecordMapper recordMapper;

    private static final AtomicInteger SEQ = new AtomicInteger(0);
    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    // ============================================================

    // 1. 标签模板管理
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public LabelTemplate createTemplate(LabelTemplate template) {
        template.setStatus("ACTIVE");
        if (template.getTemplateFormat() == null) template.setTemplateFormat("ZPL");
        if (template.getBarcodeType() == null) template.setBarcodeType("CODE128");
        if (template.getOrientation() == null) template.setOrientation("PORTRAIT");
        if (template.getPrintCount() == null) template.setPrintCount(1);
        templateMapper.insert(template);
        log.info(
                "创建标签模板: code={}, name={}, type={}",
                template.getTemplateCode(),
                template.getTemplateName(),
                template.getTemplateType());
        return template;
    }

    public LabelTemplate getTemplateByCode(String templateCode) {
        return templateMapper.selectByTemplateCode(templateCode);
    }

    public List<LabelTemplate> getTemplatesByType(String templateType) {
        return templateMapper.selectByTemplateType(templateType);
    }

    public List<LabelTemplate> getTemplatesByWarehouse(String warehouseCode) {
        return templateMapper.selectByWarehouse(warehouseCode);
    }

    public Page<LabelTemplate> pageTemplates(
            Page<LabelTemplate> page, String templateType, String warehouseCode) {
        LambdaQueryWrapper<LabelTemplate> wrapper = new LambdaQueryWrapper<>();
        if (templateType != null) wrapper.eq(LabelTemplate::getTemplateType, templateType);
        if (warehouseCode != null) wrapper.eq(LabelTemplate::getWarehouseCode, warehouseCode);
        wrapper.eq(LabelTemplate::getStatus, "ACTIVE");
        wrapper.orderByDesc(LabelTemplate::getCreatedTime);
        return templateMapper.selectPage(page, wrapper);
    }

    // ============================================================

    // 2. 条码规则管理
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public BarcodeRule createRule(BarcodeRule rule) {
        rule.setStatus("ACTIVE");
        if (rule.getSequenceLength() == null) rule.setSequenceLength(8);
        if (rule.getSequenceStart() == null) rule.setSequenceStart(1L);
        if (rule.getSequenceCurrent() == null) rule.setSequenceCurrent(1L);
        if (rule.getIncludeDate() == null) rule.setIncludeDate("N");
        if (rule.getIncludeOwner() == null) rule.setIncludeOwner("N");
        if (rule.getIncludeWarehouse() == null) rule.setIncludeWarehouse("N");
        if (rule.getBarcodeFormat() == null) rule.setBarcodeFormat("CODE128");
        ruleMapper.insert(rule);
        log.info(
                "创建条码规则: code={}, name={}, type={}",
                rule.getRuleCode(),
                rule.getRuleName(),
                rule.getBarcodeType());
        return rule;
    }

    public BarcodeRule getRuleByCode(String ruleCode) {
        return ruleMapper.selectByRuleCode(ruleCode);
    }

    public List<BarcodeRule> getRulesByType(String barcodeType) {
        return ruleMapper.selectByBarcodeType(barcodeType);
    }

    public BarcodeRule getRuleByWarehouseAndType(String warehouseCode, String barcodeType) {
        return ruleMapper.selectByWarehouseAndType(warehouseCode, barcodeType);
    }

    public Page<BarcodeRule> pageRules(
            Page<BarcodeRule> page, String barcodeType, String warehouseCode) {
        LambdaQueryWrapper<BarcodeRule> wrapper = new LambdaQueryWrapper<>();
        if (barcodeType != null) wrapper.eq(BarcodeRule::getBarcodeType, barcodeType);
        if (warehouseCode != null) wrapper.eq(BarcodeRule::getWarehouseCode, warehouseCode);
        wrapper.eq(BarcodeRule::getStatus, "ACTIVE");
        wrapper.orderByDesc(BarcodeRule::getCreatedTime);
        return ruleMapper.selectPage(page, wrapper);
    }

    // ============================================================

    // 3. 条码生成（核心）
    // ============================================================

    /** 生成条码 */
    @Transactional(rollbackFor = Exception.class)
    public BarcodeRecord generateBarcode(
            String ruleCode,
            String warehouseCode,
            String ownerCode,
            String bizKey,
            String bizType,
            String operator) {
        BarcodeRule rule = ruleMapper.selectByRuleCode(ruleCode);
        if (rule == null) throw new RuntimeException("条码规则不存在: " + ruleCode);

        // 原子递增序列号
        ruleMapper.incrementSequence(ruleCode);
        rule = ruleMapper.selectByRuleCode(ruleCode);
        long sequence = rule.getSequenceCurrent();

        // 构建条码
        StringBuilder barcode = new StringBuilder();
        if (rule.getPrefix() != null) barcode.append(rule.getPrefix());
        if ("Y".equals(rule.getIncludeWarehouse()) && warehouseCode != null) {
            barcode.append(warehouseCode);
        }
        if ("Y".equals(rule.getIncludeOwner()) && ownerCode != null) {
            barcode.append(ownerCode);
        }
        if ("Y".equals(rule.getIncludeDate())) {
            String dateFormat = rule.getDateFormat() != null ? rule.getDateFormat() : "yyyyMMdd";
            barcode.append(LocalDateTime.now().format(DateTimeFormatter.ofPattern(dateFormat)));
        }
        // 序列号补零
        barcode.append(String.format("%0" + rule.getSequenceLength() + "d", sequence));
        if (rule.getSuffix() != null) barcode.append(rule.getSuffix());

        // 校验码
        if (rule.getChecksumType() != null && !"NONE".equals(rule.getChecksumType())) {
            barcode.append(calculateChecksum(barcode.toString(), rule.getChecksumType()));
        }

        String barcodeStr = barcode.toString();

        // 创建条码记录
        BarcodeRecord record = new BarcodeRecord();
        record.setRecordId(generateRecordId());
        record.setBarcode(barcodeStr);
        record.setBarcodeType(rule.getBarcodeType());
        record.setRuleCode(ruleCode);
        record.setWarehouseCode(warehouseCode);
        record.setOwnerCode(ownerCode);
        record.setBizKey(bizKey);
        record.setBizType(bizType);
        record.setSequenceNo(sequence);
        record.setGenerateTime(LocalDateTime.now());
        record.setPrintCount(0);
        record.setScanCount(0);
        record.setStatus("ACTIVE");
        recordMapper.insert(record);

        log.info("生成条码: rule={}, barcode={}, bizKey={}", ruleCode, barcodeStr, bizKey);
        return record;
    }

    /** 计算校验码 */
    private String calculateChecksum(String data, String checksumType) {
        if ("MOD10".equals(checksumType)) {
            int sum = 0;
            for (int i = 0; i < data.length(); i++) {
                char c = data.charAt(i);
                if (Character.isDigit(c)) {
                    int digit = c - '0';
                    sum += (i % 2 == 0) ? digit * 2 : digit;
                }
            }
            int checksum = (10 - (sum % 10)) % 10;
            return String.valueOf(checksum);
        }
        return "";
    }

    /** 解析条码 */
    public BarcodeRecord parseBarcode(String barcode) {
        BarcodeRecord record = recordMapper.selectByBarcode(barcode);
        if (record == null) {
            throw new RuntimeException("条码不存在或无效: " + barcode);
        }
        // 记录扫描
        recordMapper.incrementScanCount(record.getRecordId());
        log.info(
                "解析条码: barcode={}, type={}, bizKey={}",
                barcode,
                record.getBarcodeType(),
                record.getBizKey());
        return recordMapper.selectByBarcode(barcode);
    }

    // ============================================================

    // 4. 标签任务管理
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public LabelTask createTask(
            String taskName,
            String templateCode,
            String warehouseCode,
            String ownerCode,
            String bizType,
            String bizNo,
            int totalCount,
            String printer,
            String printMode,
            String operator) {
        LabelTask task = new LabelTask();
        task.setTaskId(generateTaskId());
        task.setTaskName(taskName);
        task.setTemplateCode(templateCode);
        task.setWarehouseCode(warehouseCode);
        task.setOwnerCode(ownerCode);
        task.setBizType(bizType);
        task.setBizNo(bizNo);
        task.setTotalCount(totalCount);
        task.setPrintedCount(0);
        task.setFailedCount(0);
        task.setPrinter(printer);
        task.setPrintMode(printMode != null ? printMode : "BATCH");
        task.setStatus("PENDING");
        task.setOperator(operator);
        taskMapper.insert(task);
        log.info(
                "创建标签任务: taskId={}, template={}, total={}",
                task.getTaskId(),
                templateCode,
                totalCount);
        return task;
    }

    @Transactional(rollbackFor = Exception.class)
    public LabelTask startTask(String taskId) {
        LabelTask task = taskMapper.selectByTaskId(taskId);
        if (task == null) throw new RuntimeException("标签任务不存在: " + taskId);
        if (!"PENDING".equals(task.getStatus())) {
            throw new RuntimeException("任务状态不正确: " + task.getStatus());
        }
        taskMapper.startTask(taskId, "PRINTING");
        log.info("开始标签任务: taskId={}", taskId);
        return taskMapper.selectByTaskId(taskId);
    }

    @Transactional(rollbackFor = Exception.class)
    public LabelTask completeTask(
            String taskId,
            int printedCount,
            int failedCount,
            Long durationMs,
            String errorMessage) {
        String status = failedCount > 0 && printedCount == 0 ? "FAILED" : "COMPLETED";
        taskMapper.completeTask(
                taskId, status, printedCount, failedCount, durationMs, errorMessage);
        log.info("完成标签任务: taskId={}, printed={}, failed={}", taskId, printedCount, failedCount);
        return taskMapper.selectByTaskId(taskId);
    }

    @Transactional(rollbackFor = Exception.class)
    public LabelTask cancelTask(String taskId, String reason, String operator) {
        LabelTask task = taskMapper.selectByTaskId(taskId);
        if (task == null) throw new RuntimeException("标签任务不存在: " + taskId);
        if (!"PENDING".equals(task.getStatus()) && !"PRINTING".equals(task.getStatus())) {
            throw new RuntimeException("任务状态不正确: " + task.getStatus());
        }
        taskMapper.completeTask(
                taskId, "CANCELLED", task.getPrintedCount(), task.getFailedCount(), 0L, reason);
        log.info("取消标签任务: taskId={}, reason={}", taskId, reason);
        return taskMapper.selectByTaskId(taskId);
    }

    public LabelTask getTaskById(String taskId) {
        return taskMapper.selectByTaskId(taskId);
    }

    public List<LabelTask> getTasksByStatus(String status) {
        return taskMapper.selectByStatus(status);
    }

    public List<LabelTask> getRecentTasksByTemplate(String templateCode, int limit) {
        return taskMapper.selectRecentByTemplate(templateCode, limit);
    }

    public Page<LabelTask> pageTasks(
            Page<LabelTask> page,
            String templateCode,
            String status,
            String warehouseCode,
            String bizType) {
        LambdaQueryWrapper<LabelTask> wrapper = new LambdaQueryWrapper<>();
        if (templateCode != null) wrapper.eq(LabelTask::getTemplateCode, templateCode);
        if (status != null) wrapper.eq(LabelTask::getStatus, status);
        if (warehouseCode != null) wrapper.eq(LabelTask::getWarehouseCode, warehouseCode);
        if (bizType != null) wrapper.eq(LabelTask::getBizType, bizType);
        wrapper.orderByDesc(LabelTask::getCreatedTime);
        return taskMapper.selectPage(page, wrapper);
    }

    // ============================================================

    // 5. 条码记录管理
    // ============================================================

    public BarcodeRecord getRecordByBarcode(String barcode) {
        return recordMapper.selectByBarcode(barcode);
    }

    public List<BarcodeRecord> getRecordsByTypeAndBizKey(String barcodeType, String bizKey) {
        return recordMapper.selectByTypeAndBizKey(barcodeType, bizKey);
    }

    public List<BarcodeRecord> getRecentRecordsByWarehouseAndType(
            String warehouseCode, String barcodeType, int limit) {
        return recordMapper.selectRecentByWarehouseAndType(warehouseCode, barcodeType, limit);
    }

    @Transactional(rollbackFor = Exception.class)
    public int recordPrint(String recordId) {
        return recordMapper.incrementPrintCount(recordId);
    }

    @Transactional(rollbackFor = Exception.class)
    public int recordScan(String recordId) {
        return recordMapper.incrementScanCount(recordId);
    }

    @Transactional(rollbackFor = Exception.class)
    public int updateRecordStatus(String recordId, String status) {
        return recordMapper.updateStatus(recordId, status);
    }

    public Page<BarcodeRecord> pageRecords(
            Page<BarcodeRecord> page,
            String barcodeType,
            String warehouseCode,
            String bizKey,
            String status) {
        LambdaQueryWrapper<BarcodeRecord> wrapper = new LambdaQueryWrapper<>();
        if (barcodeType != null) wrapper.eq(BarcodeRecord::getBarcodeType, barcodeType);
        if (warehouseCode != null) wrapper.eq(BarcodeRecord::getWarehouseCode, warehouseCode);
        if (bizKey != null) wrapper.eq(BarcodeRecord::getBizKey, bizKey);
        if (status != null) wrapper.eq(BarcodeRecord::getStatus, status);
        wrapper.orderByDesc(BarcodeRecord::getCreatedTime);
        return recordMapper.selectPage(page, wrapper);
    }

    // ============================================================

    // 工具方法
    // ============================================================

    private String generateTaskId() {
        return "LT"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }

    private String generateRecordId() {
        return "BR"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }
}
