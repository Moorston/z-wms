package com.xwms.core.plugin.industry.gsp.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.core.plugin.industry.gsp.entity.*;
import com.xwms.core.plugin.industry.gsp.mapper.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** GSP医药行业服务 包含批次追溯、质检记录、温度日志、证照管理 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GspService {

    private final GspBatchRecordMapper batchRecordMapper;
    private final GspQualityCheckMapper qualityCheckMapper;
    private final GspTemperatureLogMapper temperatureLogMapper;
    private final GspLicenseMapper licenseMapper;

    private static final AtomicInteger SEQ = new AtomicInteger(0);
    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    // ============================================================

    // 1. GSP批次追溯
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public GspBatchRecord createBatchRecord(GspBatchRecord record) {
        record.setRecordNo(generateBatchRecordNo());
        record.setQualityStatus("WAITING");
        record.setMaintenanceStatus("NORMAL");
        record.setOutboundQty(BigDecimal.ZERO);
        record.setCreatedTime(LocalDateTime.now());
        batchRecordMapper.insert(record);
        log.info("创建GSP批次记录: {}", record.getRecordNo());
        return record;
    }

    @Transactional(rollbackFor = Exception.class)
    public GspBatchRecord updateQualityStatus(
            Long recordId, String qualityStatus, String operator) {
        GspBatchRecord record = batchRecordMapper.selectById(recordId);
        if (record == null) {
            throw new RuntimeException("批次记录不存在: " + recordId);
        }
        record.setQualityStatus(qualityStatus);
        record.setUpdatedBy(operator);
        record.setUpdatedTime(LocalDateTime.now());
        batchRecordMapper.updateById(record);
        log.info("更新GSP批次质量状态: {}, status={}", record.getRecordNo(), qualityStatus);
        return record;
    }

    @Transactional(rollbackFor = Exception.class)
    public GspBatchRecord updateMaintenanceStatus(
            Long recordId, String maintenanceStatus, String operator) {
        GspBatchRecord record = batchRecordMapper.selectById(recordId);
        if (record == null) {
            throw new RuntimeException("批次记录不存在: " + recordId);
        }
        record.setMaintenanceStatus(maintenanceStatus);
        record.setUpdatedBy(operator);
        record.setUpdatedTime(LocalDateTime.now());
        batchRecordMapper.updateById(record);
        return record;
    }

    @Transactional(rollbackFor = Exception.class)
    public GspBatchRecord outboundBatch(Long recordId, BigDecimal outboundQty, String operator) {
        GspBatchRecord record = batchRecordMapper.selectById(recordId);
        if (record == null) {
            throw new RuntimeException("批次记录不存在: " + recordId);
        }
        BigDecimal newOutboundQty = record.getOutboundQty().add(outboundQty);
        if (newOutboundQty.compareTo(record.getInboundQty()) > 0) {
            throw new RuntimeException("出库数量超过入库数量: " + record.getRecordNo());
        }
        record.setOutboundQty(newOutboundQty);
        record.setStockQty(record.getInboundQty().subtract(newOutboundQty));
        record.setUpdatedBy(operator);
        record.setUpdatedTime(LocalDateTime.now());
        batchRecordMapper.updateById(record);
        log.info("GSP批次出库: {}, 出库数量: {}", record.getRecordNo(), outboundQty);
        return record;
    }

    public GspBatchRecord getBatchRecord(Long id) {
        return batchRecordMapper.selectById(id);
    }

    public GspBatchRecord getBatchRecordByNo(String recordNo) {
        return batchRecordMapper.selectByRecordNo(recordNo);
    }

    public List<GspBatchRecord> getBatchRecordsByBatchNo(String batchNo) {
        return batchRecordMapper.selectByBatchNo(batchNo);
    }

    public List<GspBatchRecord> getBatchRecordsBySkuCode(String skuCode) {
        return batchRecordMapper.selectBySkuCode(skuCode);
    }

    public List<GspBatchRecord> getNearExpiryBatches(Integer days) {
        return batchRecordMapper.selectNearExpiryBatches(days);
    }

    public List<GspBatchRecord> getExpiredBatches() {
        return batchRecordMapper.selectExpiredBatches();
    }

    public GspBatchRecord getBatchRecordByTraceCode(String traceCode) {
        return batchRecordMapper.selectByTraceCode(traceCode);
    }

    public Page<GspBatchRecord> pageBatchRecords(
            Page<GspBatchRecord> page,
            String warehouseCode,
            String skuCode,
            String batchNo,
            String qualityStatus,
            String maintenanceStatus) {
        LambdaQueryWrapper<GspBatchRecord> wrapper = new LambdaQueryWrapper<>();
        if (warehouseCode != null) wrapper.eq(GspBatchRecord::getWarehouseCode, warehouseCode);
        if (skuCode != null) wrapper.eq(GspBatchRecord::getSkuCode, skuCode);
        if (batchNo != null) wrapper.eq(GspBatchRecord::getBatchNo, batchNo);
        if (qualityStatus != null) wrapper.eq(GspBatchRecord::getQualityStatus, qualityStatus);
        if (maintenanceStatus != null)
            wrapper.eq(GspBatchRecord::getMaintenanceStatus, maintenanceStatus);
        wrapper.orderByDesc(GspBatchRecord::getCreatedTime);
        return batchRecordMapper.selectPage(page, wrapper);
    }

    // ============================================================

    // 2. GSP质检记录
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public GspQualityCheck createQualityCheck(GspQualityCheck check) {
        check.setCheckNo(generateCheckNo());
        check.setStatus("PENDING");
        check.setCreatedTime(LocalDateTime.now());
        qualityCheckMapper.insert(check);
        log.info("创建GSP质检记录: {}", check.getCheckNo());
        return check;
    }

    @Transactional(rollbackFor = Exception.class)
    public GspQualityCheck startQualityCheck(Long checkId, String checker) {
        GspQualityCheck check = qualityCheckMapper.selectById(checkId);
        if (check == null) {
            throw new RuntimeException("质检记录不存在: " + checkId);
        }
        check.setStatus("CHECKING");
        check.setChecker(checker);
        check.setCheckTime(LocalDateTime.now());
        check.setUpdatedTime(LocalDateTime.now());
        qualityCheckMapper.updateById(check);
        return check;
    }

    @Transactional(rollbackFor = Exception.class)
    public GspQualityCheck completeQualityCheck(
            Long checkId,
            String checkResult,
            BigDecimal qualifiedQty,
            BigDecimal unqualifiedQty,
            String unqualifiedReason,
            String checker) {
        GspQualityCheck check = qualityCheckMapper.selectById(checkId);
        if (check == null) {
            throw new RuntimeException("质检记录不存在: " + checkId);
        }
        check.setCheckResult(checkResult);
        check.setQualifiedQty(qualifiedQty);
        check.setUnqualifiedQty(unqualifiedQty);
        check.setUnqualifiedReason(unqualifiedReason);
        check.setStatus("COMPLETED");
        check.setChecker(checker);
        check.setCheckTime(LocalDateTime.now());
        check.setUpdatedTime(LocalDateTime.now());
        qualityCheckMapper.updateById(check);

        // 更新批次质量状态
        if (check.getBatchNo() != null) {
            List<GspBatchRecord> records = batchRecordMapper.selectByBatchNo(check.getBatchNo());
            for (GspBatchRecord record : records) {
                String qualityStatus =
                        "QUALIFIED".equals(checkResult) ? "QUALIFIED" : "UNQUALIFIED";
                updateQualityStatus(record.getId(), qualityStatus, checker);
            }
        }

        log.info("完成GSP质检: {}, result={}", check.getCheckNo(), checkResult);
        return check;
    }

    @Transactional(rollbackFor = Exception.class)
    public GspQualityCheck reviewQualityCheck(Long checkId, String reviewer, String reviewOpinion) {
        GspQualityCheck check = qualityCheckMapper.selectById(checkId);
        if (check == null) {
            throw new RuntimeException("质检记录不存在: " + checkId);
        }
        check.setReviewer(reviewer);
        check.setHandleOpinion(reviewOpinion);
        check.setReviewTime(LocalDateTime.now());
        check.setUpdatedTime(LocalDateTime.now());
        qualityCheckMapper.updateById(check);
        return check;
    }

    @Transactional(rollbackFor = Exception.class)
    public GspQualityCheck approveQualityCheck(
            Long checkId, String approver, String handleOpinion) {
        GspQualityCheck check = qualityCheckMapper.selectById(checkId);
        if (check == null) {
            throw new RuntimeException("质检记录不存在: " + checkId);
        }
        check.setApprover(approver);
        check.setHandleOpinion(handleOpinion);
        check.setApproveTime(LocalDateTime.now());
        check.setUpdatedTime(LocalDateTime.now());
        qualityCheckMapper.updateById(check);
        return check;
    }

    public GspQualityCheck getQualityCheck(Long id) {
        return qualityCheckMapper.selectById(id);
    }

    public GspQualityCheck getQualityCheckByNo(String checkNo) {
        return qualityCheckMapper.selectByCheckNo(checkNo);
    }

    public List<GspQualityCheck> getQualityChecksByRefNo(String refNo) {
        return qualityCheckMapper.selectByRefNo(refNo);
    }

    public List<GspQualityCheck> getQualityChecksByBatchNo(String batchNo) {
        return qualityCheckMapper.selectByBatchNo(batchNo);
    }

    public List<GspQualityCheck> getPendingQualityChecks() {
        return qualityCheckMapper.selectPendingChecks();
    }

    public Page<GspQualityCheck> pageQualityChecks(
            Page<GspQualityCheck> page,
            String warehouseCode,
            String checkType,
            String batchNo,
            String status,
            String checkResult) {
        LambdaQueryWrapper<GspQualityCheck> wrapper = new LambdaQueryWrapper<>();
        if (warehouseCode != null) wrapper.eq(GspQualityCheck::getWarehouseCode, warehouseCode);
        if (checkType != null) wrapper.eq(GspQualityCheck::getCheckType, checkType);
        if (batchNo != null) wrapper.eq(GspQualityCheck::getBatchNo, batchNo);
        if (status != null) wrapper.eq(GspQualityCheck::getStatus, status);
        if (checkResult != null) wrapper.eq(GspQualityCheck::getCheckResult, checkResult);
        wrapper.orderByDesc(GspQualityCheck::getCreatedTime);
        return qualityCheckMapper.selectPage(page, wrapper);
    }

    // ============================================================

    // 3. GSP温度日志
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public GspTemperatureLog collectTemperature(
            String warehouseCode,
            String areaCode,
            String locationCode,
            String equipmentCode,
            BigDecimal temperature,
            BigDecimal humidity,
            String storageCondition,
            String collectType,
            String collector) {
        GspTemperatureLog logRecord = new GspTemperatureLog();
        logRecord.setLogNo(generateTempLogNo());
        logRecord.setWarehouseCode(warehouseCode);
        logRecord.setAreaCode(areaCode);
        logRecord.setLocationCode(locationCode);
        logRecord.setEquipmentCode(equipmentCode);
        logRecord.setTemperature(temperature);
        logRecord.setHumidity(humidity);
        logRecord.setStorageCondition(storageCondition);
        logRecord.setCollectTime(LocalDateTime.now());
        logRecord.setCollectType(collectType);
        logRecord.setCollector(collector);
        logRecord.setTempStatus("NORMAL");
        logRecord.setHumidityStatus("NORMAL");
        logRecord.setIsAlert("N");
        logRecord.setHandleStatus("PENDING");
        logRecord.setCreatedTime(LocalDateTime.now());

        // 根据储存条件检查温度是否超标
        boolean isExceeded = checkGspTemperatureExceeded(temperature, humidity, storageCondition);
        if (isExceeded) {
            logRecord.setTempStatus("EXCEEDED");
            logRecord.setIsAlert("Y");
            log.warn(
                    "GSP温度超标: warehouse={}, area={}, temp={}, condition={}",
                    warehouseCode,
                    areaCode,
                    temperature,
                    storageCondition);
        }

        temperatureLogMapper.insert(logRecord);
        return logRecord;
    }

    /** 检查GSP温度是否超标 */
    private boolean checkGspTemperatureExceeded(
            BigDecimal temperature, BigDecimal humidity, String storageCondition) {
        if (storageCondition == null) return false;
        switch (storageCondition) {
            case "COOL": // 阴凉: 不超过20℃
                return temperature.compareTo(new BigDecimal("20")) > 0;
            case "COLD": // 冷藏: 2-8℃
                return temperature.compareTo(new BigDecimal("2")) < 0
                        || temperature.compareTo(new BigDecimal("8")) > 0;
            case "FROZEN": // 冷冻: 不超过-10℃
                return temperature.compareTo(new BigDecimal("-10")) > 0;
            case "NORMAL": // 常温: 10-30℃
                return temperature.compareTo(new BigDecimal("10")) < 0
                        || temperature.compareTo(new BigDecimal("30")) > 0;
            default:
                return false;
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public GspTemperatureLog handleTemperatureAlert(
            Long logId, String handleMeasure, String handleResult, String handledBy) {
        GspTemperatureLog logRecord = temperatureLogMapper.selectById(logId);
        if (logRecord == null) {
            throw new RuntimeException("温度日志不存在: " + logId);
        }
        logRecord.setHandleStatus("RESOLVED");
        logRecord.setHandleMeasure(handleMeasure);
        logRecord.setHandleResult(handleResult);
        logRecord.setHandledBy(handledBy);
        logRecord.setHandledTime(LocalDateTime.now());
        temperatureLogMapper.updateById(logRecord);
        return logRecord;
    }

    public GspTemperatureLog getTemperatureLog(Long id) {
        return temperatureLogMapper.selectById(id);
    }

    public GspTemperatureLog getTemperatureLogByNo(String logNo) {
        return temperatureLogMapper.selectByLogNo(logNo);
    }

    public List<GspTemperatureLog> getTemperatureLogsByAreaAndTime(
            String areaCode, LocalDateTime startTime, LocalDateTime endTime) {
        return temperatureLogMapper.selectByAreaAndTime(areaCode, startTime, endTime);
    }

    public List<GspTemperatureLog> getExceededTemperatureLogs() {
        return temperatureLogMapper.selectExceededRecords();
    }

    public List<GspTemperatureLog> getPendingHandleTemperatureLogs() {
        return temperatureLogMapper.selectPendingHandleRecords();
    }

    public Page<GspTemperatureLog> pageTemperatureLogs(
            Page<GspTemperatureLog> page,
            String warehouseCode,
            String areaCode,
            String equipmentCode,
            String tempStatus,
            String handleStatus) {
        LambdaQueryWrapper<GspTemperatureLog> wrapper = new LambdaQueryWrapper<>();
        if (warehouseCode != null) wrapper.eq(GspTemperatureLog::getWarehouseCode, warehouseCode);
        if (areaCode != null) wrapper.eq(GspTemperatureLog::getAreaCode, areaCode);
        if (equipmentCode != null) wrapper.eq(GspTemperatureLog::getEquipmentCode, equipmentCode);
        if (tempStatus != null) wrapper.eq(GspTemperatureLog::getTempStatus, tempStatus);
        if (handleStatus != null) wrapper.eq(GspTemperatureLog::getHandleStatus, handleStatus);
        wrapper.orderByDesc(GspTemperatureLog::getCollectTime);
        return temperatureLogMapper.selectPage(page, wrapper);
    }

    // ============================================================

    // 4. GSP证照管理
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public GspLicense createLicense(GspLicense license) {
        license.setLicenseNo(generateLicenseNo());
        license.setStatus("ACTIVE");
        license.setIsWarned("N");
        license.setCreatedTime(LocalDateTime.now());
        licenseMapper.insert(license);
        log.info("创建GSP证照: {}", license.getLicenseNo());
        return license;
    }

    @Transactional(rollbackFor = Exception.class)
    public GspLicense updateLicense(GspLicense license) {
        license.setUpdatedTime(LocalDateTime.now());
        licenseMapper.updateById(license);
        return licenseMapper.selectById(license.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public GspLicense renewLicense(
            Long licenseId, LocalDate newExpiryDate, String renewalDate, String operator) {
        GspLicense license = licenseMapper.selectById(licenseId);
        if (license == null) {
            throw new RuntimeException("证照不存在: " + licenseId);
        }
        license.setExpiryDate(newExpiryDate);
        license.setRenewalDate(LocalDate.parse(renewalDate));
        license.setStatus("ACTIVE");
        license.setIsWarned("N");
        license.setWarnTime(null);
        license.setUpdatedBy(operator);
        license.setUpdatedTime(LocalDateTime.now());
        licenseMapper.updateById(license);
        log.info("GSP证照换证: {}, 新有效期至: {}", license.getLicenseNo(), newExpiryDate);
        return license;
    }

    @Transactional(rollbackFor = Exception.class)
    public GspLicense warnLicenseExpiry(Long licenseId) {
        GspLicense license = licenseMapper.selectById(licenseId);
        if (license == null) {
            throw new RuntimeException("证照不存在: " + licenseId);
        }
        license.setIsWarned("Y");
        license.setWarnTime(LocalDateTime.now());
        license.setStatus("EXPIRING");
        license.setUpdatedTime(LocalDateTime.now());
        licenseMapper.updateById(license);
        log.warn("GSP证照即将到期预警: {}", license.getLicenseNo());
        return license;
    }

    public GspLicense getLicense(Long id) {
        return licenseMapper.selectById(id);
    }

    public GspLicense getLicenseByNo(String licenseNo) {
        return licenseMapper.selectByLicenseNo(licenseNo);
    }

    public List<GspLicense> getLicensesByType(String licenseType) {
        return licenseMapper.selectByLicenseType(licenseType);
    }

    public List<GspLicense> getExpiringLicenses(LocalDate expiryDate) {
        return licenseMapper.selectExpiringLicenses(expiryDate);
    }

    public List<GspLicense> getExpiredLicenses() {
        return licenseMapper.selectExpiredLicenses();
    }

    public List<GspLicense> getLicensesByWarehouse(String warehouseCode) {
        return licenseMapper.selectByWarehouse(warehouseCode);
    }

    public List<GspLicense> getLicensesByOwner(String ownerCode) {
        return licenseMapper.selectByOwner(ownerCode);
    }

    public Page<GspLicense> pageLicenses(
            Page<GspLicense> page,
            String licenseType,
            String warehouseCode,
            String ownerCode,
            String status) {
        LambdaQueryWrapper<GspLicense> wrapper = new LambdaQueryWrapper<>();
        if (licenseType != null) wrapper.eq(GspLicense::getLicenseType, licenseType);
        if (warehouseCode != null) wrapper.eq(GspLicense::getWarehouseCode, warehouseCode);
        if (ownerCode != null) wrapper.eq(GspLicense::getOwnerCode, ownerCode);
        if (status != null) wrapper.eq(GspLicense::getStatus, status);
        wrapper.orderByDesc(GspLicense::getCreatedTime);
        return licenseMapper.selectPage(page, wrapper);
    }

    // ============================================================

    // 5. 编号生成
    // ============================================================

    private String generateBatchRecordNo() {
        return "GBR"
                + NO_FMT.format(LocalDateTime.now())
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }

    private String generateCheckNo() {
        return "GQC"
                + NO_FMT.format(LocalDateTime.now())
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }

    private String generateTempLogNo() {
        return "GTL"
                + NO_FMT.format(LocalDateTime.now())
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }

    private String generateLicenseNo() {
        return "GLI"
                + NO_FMT.format(LocalDateTime.now())
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }
}
