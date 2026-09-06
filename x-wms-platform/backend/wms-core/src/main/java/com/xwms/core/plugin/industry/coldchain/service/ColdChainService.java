package com.xwms.core.plugin.industry.coldchain.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.core.plugin.industry.coldchain.entity.*;
import com.xwms.core.plugin.industry.coldchain.mapper.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 冷链行业服务 包含温度监控、报警处理、设备管理、温区管理 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ColdChainService {

    private final TemperatureRecordMapper recordMapper;
    private final TemperatureAlertMapper alertMapper;
    private final ColdChainEquipmentMapper equipmentMapper;
    private final TemperatureZoneMapper zoneMapper;

    private static final AtomicInteger SEQ = new AtomicInteger(0);
    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    // ============================================================

    // 1. 温度监控记录
    // ============================================================

    /** 采集温度数据 */
    @Transactional(rollbackFor = Exception.class)
    public TemperatureRecord collectTemperature(
            String warehouseCode,
            String areaCode,
            String locationCode,
            String equipmentCode,
            BigDecimal temperature,
            BigDecimal humidity,
            String collectType,
            String collector) {
        log.info(
                "采集温度数据: warehouse={}, location={}, temp={}, humidity={}",
                warehouseCode,
                locationCode,
                temperature,
                humidity);

        TemperatureRecord record = new TemperatureRecord();
        record.setRecordNo(generateRecordNo());
        record.setWarehouseCode(warehouseCode);
        record.setAreaCode(areaCode);
        record.setLocationCode(locationCode);
        record.setEquipmentCode(equipmentCode);
        record.setTemperature(temperature);
        record.setHumidity(humidity);
        record.setCollectTime(LocalDateTime.now());
        record.setCollectType(collectType);
        record.setCollector(collector);
        record.setStatus("NORMAL");
        record.setCreatedTime(LocalDateTime.now());

        // 检查温度是否超标
        TemperatureZone zone = zoneMapper.selectByAreaCode(areaCode);
        if (zone != null) {
            record.setTemperatureType(zone.getTemperatureType());
            boolean isExceeded = checkTemperatureExceeded(temperature, humidity, zone);
            if (isExceeded) {
                record.setStatus("ABNORMAL");
                // 触发报警
                createTemperatureAlert(record, zone);
            }
        }

        recordMapper.insert(record);

        // 更新设备当前温度
        if (equipmentCode != null) {
            ColdChainEquipment equipment = equipmentMapper.selectByEquipmentCode(equipmentCode);
            if (equipment != null) {
                equipment.setCurrentTemp(temperature);
                equipment.setCurrentHumidity(humidity);
                equipmentMapper.updateById(equipment);
            }
        }

        return record;
    }

    /** 检查温度是否超标 */
    private boolean checkTemperatureExceeded(
            BigDecimal temperature, BigDecimal humidity, TemperatureZone zone) {
        if (zone.getTempUpperLimit() != null
                && temperature.compareTo(zone.getTempUpperLimit()) > 0) {
            return true;
        }
        if (zone.getTempLowerLimit() != null
                && temperature.compareTo(zone.getTempLowerLimit()) < 0) {
            return true;
        }
        if (zone.getHumidityUpperLimit() != null
                && humidity.compareTo(zone.getHumidityUpperLimit()) > 0) {
            return true;
        }
        if (zone.getHumidityLowerLimit() != null
                && humidity.compareTo(zone.getHumidityLowerLimit()) < 0) {
            return true;
        }
        return false;
    }

    /** 创建温度报警 */
    private void createTemperatureAlert(TemperatureRecord record, TemperatureZone zone) {
        String alertType = determineAlertType(record.getTemperature(), record.getHumidity(), zone);
        String alertLevel =
                determineAlertLevel(record.getTemperature(), record.getHumidity(), zone);

        TemperatureAlert alert = new TemperatureAlert();
        alert.setAlertNo(generateAlertNo());
        alert.setRecordNo(record.getRecordNo());
        alert.setWarehouseCode(record.getWarehouseCode());
        alert.setAreaCode(record.getAreaCode());
        alert.setLocationCode(record.getLocationCode());
        alert.setEquipmentCode(record.getEquipmentCode());
        alert.setAlertType(alertType);
        alert.setAlertLevel(alertLevel);
        alert.setCurrentTemp(record.getTemperature());
        alert.setTempUpperLimit(zone.getTempUpperLimit());
        alert.setTempLowerLimit(zone.getTempLowerLimit());
        alert.setCurrentHumidity(record.getHumidity());
        alert.setAlertTime(LocalDateTime.now());
        alert.setStatus("PENDING");
        alert.setNotifyStatus("NOT_NOTIFIED");
        alert.setCreatedTime(LocalDateTime.now());

        alertMapper.insert(alert);
        log.warn(
                "温度报警: alertNo={}, type={}, level={}, temp={}",
                alert.getAlertNo(),
                alertType,
                alertLevel,
                record.getTemperature());
    }

    /** 确定报警类型 */
    private String determineAlertType(
            BigDecimal temperature, BigDecimal humidity, TemperatureZone zone) {
        if (zone.getTempUpperLimit() != null
                && temperature.compareTo(zone.getTempUpperLimit()) > 0) {
            return "HIGH_TEMP";
        }
        if (zone.getTempLowerLimit() != null
                && temperature.compareTo(zone.getTempLowerLimit()) < 0) {
            return "LOW_TEMP";
        }
        if (zone.getHumidityUpperLimit() != null
                && humidity.compareTo(zone.getHumidityUpperLimit()) > 0) {
            return "HIGH_HUMIDITY";
        }
        if (zone.getHumidityLowerLimit() != null
                && humidity.compareTo(zone.getHumidityLowerLimit()) < 0) {
            return "LOW_HUMIDITY";
        }
        return "OTHER";
    }

    /** 确定报警级别 */
    private String determineAlertLevel(
            BigDecimal temperature, BigDecimal humidity, TemperatureZone zone) {
        BigDecimal exceedAmount = BigDecimal.ZERO;
        if (zone.getTempUpperLimit() != null
                && temperature.compareTo(zone.getTempUpperLimit()) > 0) {
            exceedAmount = temperature.subtract(zone.getTempUpperLimit()).abs();
        } else if (zone.getTempLowerLimit() != null
                && temperature.compareTo(zone.getTempLowerLimit()) < 0) {
            exceedAmount = zone.getTempLowerLimit().subtract(temperature).abs();
        }

        if (exceedAmount.compareTo(new BigDecimal("5")) >= 0) {
            return "CRITICAL";
        } else if (exceedAmount.compareTo(new BigDecimal("2")) >= 0) {
            return "WARNING";
        } else {
            return "INFO";
        }
    }

    public TemperatureRecord getRecord(Long id) {
        return recordMapper.selectById(id);
    }

    public TemperatureRecord getRecordByNo(String recordNo) {
        return recordMapper.selectByRecordNo(recordNo);
    }

    public Page<TemperatureRecord> pageRecords(
            Page<TemperatureRecord> page,
            String warehouseCode,
            String locationCode,
            String equipmentCode,
            String status) {
        LambdaQueryWrapper<TemperatureRecord> wrapper = new LambdaQueryWrapper<>();
        if (warehouseCode != null) wrapper.eq(TemperatureRecord::getWarehouseCode, warehouseCode);
        if (locationCode != null) wrapper.eq(TemperatureRecord::getLocationCode, locationCode);
        if (equipmentCode != null) wrapper.eq(TemperatureRecord::getEquipmentCode, equipmentCode);
        if (status != null) wrapper.eq(TemperatureRecord::getStatus, status);
        wrapper.orderByDesc(TemperatureRecord::getCollectTime);
        return recordMapper.selectPage(page, wrapper);
    }

    // ============================================================

    // 2. 温度报警处理
    // ============================================================

    /** 处理报警 */
    @Transactional(rollbackFor = Exception.class)
    public TemperatureAlert handleAlert(
            Long alertId, String handleResult, String handleRemark, String handledBy) {
        TemperatureAlert alert = alertMapper.selectById(alertId);
        if (alert == null) {
            throw new RuntimeException("报警记录不存在: " + alertId);
        }

        alert.setStatus("RESOLVED");
        alert.setHandleResult(handleResult);
        alert.setHandleRemark(handleRemark);
        alert.setHandledBy(handledBy);
        alert.setHandledTime(LocalDateTime.now());
        alert.setUpdatedTime(LocalDateTime.now());

        alertMapper.updateById(alert);
        log.info("处理报警: alertNo={}, result={}", alert.getAlertNo(), handleResult);
        return alert;
    }

    /** 忽略报警 */
    @Transactional(rollbackFor = Exception.class)
    public TemperatureAlert ignoreAlert(Long alertId, String handleRemark, String handledBy) {
        TemperatureAlert alert = alertMapper.selectById(alertId);
        if (alert == null) {
            throw new RuntimeException("报警记录不存在: " + alertId);
        }

        alert.setStatus("IGNORED");
        alert.setHandleRemark(handleRemark);
        alert.setHandledBy(handledBy);
        alert.setHandledTime(LocalDateTime.now());
        alert.setUpdatedTime(LocalDateTime.now());

        alertMapper.updateById(alert);
        return alert;
    }

    public TemperatureAlert getAlert(Long id) {
        return alertMapper.selectById(id);
    }

    public TemperatureAlert getAlertByNo(String alertNo) {
        return alertMapper.selectByAlertNo(alertNo);
    }

    public List<TemperatureAlert> getPendingAlerts() {
        return alertMapper.selectPendingAlerts();
    }

    public Page<TemperatureAlert> pageAlerts(
            Page<TemperatureAlert> page,
            String warehouseCode,
            String locationCode,
            String alertType,
            String alertLevel,
            String status) {
        LambdaQueryWrapper<TemperatureAlert> wrapper = new LambdaQueryWrapper<>();
        if (warehouseCode != null) wrapper.eq(TemperatureAlert::getWarehouseCode, warehouseCode);
        if (locationCode != null) wrapper.eq(TemperatureAlert::getLocationCode, locationCode);
        if (alertType != null) wrapper.eq(TemperatureAlert::getAlertType, alertType);
        if (alertLevel != null) wrapper.eq(TemperatureAlert::getAlertLevel, alertLevel);
        if (status != null) wrapper.eq(TemperatureAlert::getStatus, status);
        wrapper.orderByDesc(TemperatureAlert::getAlertTime);
        return alertMapper.selectPage(page, wrapper);
    }

    public int countPendingAlerts() {
        return alertMapper.countPendingAlerts();
    }

    // ============================================================

    // 3. 冷链设备管理
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public ColdChainEquipment createEquipment(ColdChainEquipment equipment) {
        equipment.setEquipmentCode(generateEquipmentCode());
        equipment.setStatus("ACTIVE");
        equipment.setRunStatus("RUNNING");
        equipment.setCreatedTime(LocalDateTime.now());
        equipmentMapper.insert(equipment);
        return equipment;
    }

    @Transactional(rollbackFor = Exception.class)
    public ColdChainEquipment updateEquipment(ColdChainEquipment equipment) {
        equipment.setUpdatedTime(LocalDateTime.now());
        equipmentMapper.updateById(equipment);
        return equipmentMapper.selectById(equipment.getId());
    }

    public ColdChainEquipment getEquipment(Long id) {
        return equipmentMapper.selectById(id);
    }

    public ColdChainEquipment getEquipmentByCode(String equipmentCode) {
        return equipmentMapper.selectByEquipmentCode(equipmentCode);
    }

    public Page<ColdChainEquipment> pageEquipments(
            Page<ColdChainEquipment> page,
            String warehouseCode,
            String equipmentType,
            String runStatus,
            String status) {
        LambdaQueryWrapper<ColdChainEquipment> wrapper = new LambdaQueryWrapper<>();
        if (warehouseCode != null) wrapper.eq(ColdChainEquipment::getWarehouseCode, warehouseCode);
        if (equipmentType != null) wrapper.eq(ColdChainEquipment::getEquipmentType, equipmentType);
        if (runStatus != null) wrapper.eq(ColdChainEquipment::getRunStatus, runStatus);
        if (status != null) wrapper.eq(ColdChainEquipment::getStatus, status);
        wrapper.orderByAsc(ColdChainEquipment::getEquipmentCode);
        return equipmentMapper.selectPage(page, wrapper);
    }

    public List<ColdChainEquipment> getFaultEquipments() {
        return equipmentMapper.selectFaultEquipments();
    }

    public List<ColdChainEquipment> getNeedMaintainEquipments() {
        return equipmentMapper.selectNeedMaintainEquipments();
    }

    @Transactional(rollbackFor = Exception.class)
    public ColdChainEquipment startMaintenance(Long equipmentId, String remark) {
        ColdChainEquipment equipment = equipmentMapper.selectById(equipmentId);
        if (equipment == null) {
            throw new RuntimeException("设备不存在: " + equipmentId);
        }
        equipment.setRunStatus("MAINTENANCE");
        equipment.setRemark(remark);
        equipment.setUpdatedTime(LocalDateTime.now());
        equipmentMapper.updateById(equipment);
        return equipment;
    }

    @Transactional(rollbackFor = Exception.class)
    public ColdChainEquipment finishMaintenance(Long equipmentId) {
        ColdChainEquipment equipment = equipmentMapper.selectById(equipmentId);
        if (equipment == null) {
            throw new RuntimeException("设备不存在: " + equipmentId);
        }
        equipment.setRunStatus("RUNNING");
        equipment.setLastMaintainDate(LocalDateTime.now());
        equipment.setUpdatedTime(LocalDateTime.now());
        equipmentMapper.updateById(equipment);
        return equipment;
    }

    // ============================================================

    // 4. 温区管理
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public TemperatureZone createZone(TemperatureZone zone) {
        zone.setZoneCode(generateZoneCode());
        zone.setStatus("ACTIVE");
        zone.setCreatedTime(LocalDateTime.now());
        zoneMapper.insert(zone);
        return zone;
    }

    @Transactional(rollbackFor = Exception.class)
    public TemperatureZone updateZone(TemperatureZone zone) {
        zone.setUpdatedTime(LocalDateTime.now());
        zoneMapper.updateById(zone);
        return zoneMapper.selectById(zone.getId());
    }

    public TemperatureZone getZone(Long id) {
        return zoneMapper.selectById(id);
    }

    public TemperatureZone getZoneByCode(String zoneCode) {
        return zoneMapper.selectByZoneCode(zoneCode);
    }

    public List<TemperatureZone> getZonesByWarehouse(String warehouseCode) {
        return zoneMapper.selectByWarehouse(warehouseCode);
    }

    public Page<TemperatureZone> pageZones(
            Page<TemperatureZone> page,
            String warehouseCode,
            String temperatureType,
            String status) {
        LambdaQueryWrapper<TemperatureZone> wrapper = new LambdaQueryWrapper<>();
        if (warehouseCode != null) wrapper.eq(TemperatureZone::getWarehouseCode, warehouseCode);
        if (temperatureType != null)
            wrapper.eq(TemperatureZone::getTemperatureType, temperatureType);
        if (status != null) wrapper.eq(TemperatureZone::getStatus, status);
        wrapper.orderByAsc(TemperatureZone::getZoneCode);
        return zoneMapper.selectPage(page, wrapper);
    }

    public TemperatureZone getZoneByAreaCode(String areaCode) {
        return zoneMapper.selectByAreaCode(areaCode);
    }

    // ============================================================

    // 5. 编号生成
    // ============================================================

    private String generateRecordNo() {
        return "CTR"
                + NO_FMT.format(LocalDateTime.now())
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }

    private String generateAlertNo() {
        return "CTA"
                + NO_FMT.format(LocalDateTime.now())
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }

    private String generateEquipmentCode() {
        return "CCE"
                + NO_FMT.format(LocalDateTime.now())
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }

    private String generateZoneCode() {
        return "CTZ"
                + NO_FMT.format(LocalDateTime.now())
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }
}
