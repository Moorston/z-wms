package com.xwms.core.equipment.service;

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

import com.xwms.common.exception.BizException;
import com.xwms.core.equipment.entity.*;
import com.xwms.core.equipment.mapper.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 设备管理核心服务 包含: 设备档案/设备分配/设备使用/设备维护/状态上报/WCS集成 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EquipmentService {

    private final EquipmentMapper equipmentMapper;
    private final EquipmentStatusMapper statusMapper;
    private final EquipmentMaintainMapper maintainMapper;
    private final EquipmentUsageMapper usageMapper;

    private static final AtomicInteger MAINTAIN_SEQ = new AtomicInteger(0);
    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    // ============================================================

    // 1. 设备档案管理
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public Equipment createEquipment(Equipment equipment) {
        equipment.setStatus(com.xwms.core.equipment.enums.EquipmentStatus.IDLE.getCode());
        equipmentMapper.insert(equipment);

        // 创建设备状态记录
        EquipmentStatus status = new EquipmentStatus();
        status.setEquipmentId(equipment.getId());
        status.setEquipmentCode(equipment.getEquipmentCode());
        status.setRunStatus("STOPPED");
        status.setWorkStatus("IDLE");
        status.setBatteryLevel(equipment.getBatteryLevel());
        status.setLastHeartbeat(LocalDateTime.now());
        statusMapper.insert(status);

        log.info("创建设备: {}", equipment.getEquipmentCode());
        return equipment;
    }

    public Equipment getEquipment(Long id) {
        Equipment equip = equipmentMapper.selectById(id);
        if (equip == null) throw new BizException("设备不存在: " + id);
        return equip;
    }

    public Page<Equipment> pageEquipments(
            Page<Equipment> page, String type, String status, String warehouse) {
        LambdaQueryWrapper<Equipment> wrapper = new LambdaQueryWrapper<>();
        if (type != null) wrapper.eq(Equipment::getEquipmentType, type);
        if (status != null) wrapper.eq(Equipment::getStatus, status);
        if (warehouse != null) wrapper.eq(Equipment::getWarehouseCode, warehouse);
        wrapper.orderByAsc(Equipment::getEquipmentCode);
        return equipmentMapper.selectPage(page, wrapper);
    }

    public List<Equipment> getAvailableEquipments(String warehouse, String type) {
        return equipmentMapper.selectAvailableByType(type, warehouse);
    }

    // ============================================================

    // 2. 设备分配 (作业前分配设备)
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public Equipment assignEquipment(Long equipmentId, String taskNo, String operator) {
        Equipment equip = getEquipment(equipmentId);
        if (!com.xwms.core.equipment.enums.EquipmentStatus.IDLE
                .getCode()
                .equals(equip.getStatus())) {
            throw new BizException("设备状态不允许分配: " + equip.getStatus());
        }

        equip.setStatus(com.xwms.core.equipment.enums.EquipmentStatus.IN_USE.getCode());
        equipmentMapper.updateById(equip);

        // 更新设备状态
        EquipmentStatus status =
                statusMapper.selectOne(
                        new LambdaQueryWrapper<EquipmentStatus>()
                                .eq(EquipmentStatus::getEquipmentId, equipmentId));
        if (status != null) {
            status.setWorkStatus("WORKING");
            status.setCurrentTaskNo(taskNo);
            statusMapper.updateById(status);
        }

        // 创建设备使用记录
        EquipmentUsage usage = new EquipmentUsage();
        usage.setEquipmentId(equipmentId);
        usage.setEquipmentCode(equip.getEquipmentCode());
        usage.setTaskNo(taskNo);
        usage.setOperator(operator);
        usage.setStartTime(LocalDateTime.now());
        usage.setStartLocation(equip.getLocationCode());
        usage.setStartBattery(equip.getBatteryLevel());
        usage.setOwnerCodeCol(equip.getOwnerCodeCol());
        usage.setWarehouseCodeCol(equip.getWarehouseCodeCol());
        usageMapper.insert(usage);

        log.info("设备{}分配: 任务={}, 操作人={}", equip.getEquipmentCode(), taskNo, operator);
        return equip;
    }

    // ============================================================

    // 3. 设备释放 (作业完成后释放)
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public Equipment releaseEquipment(Long equipmentId, String location, BigDecimal endBattery) {
        Equipment equip = getEquipment(equipmentId);

        equip.setStatus(com.xwms.core.equipment.enums.EquipmentStatus.IDLE.getCode());
        equip.setLocationCode(location);
        if (endBattery != null) {
            equip.setBatteryLevel(endBattery);
        }
        equipmentMapper.updateById(equip);

        // 更新设备状态
        EquipmentStatus status =
                statusMapper.selectOne(
                        new LambdaQueryWrapper<EquipmentStatus>()
                                .eq(EquipmentStatus::getEquipmentId, equipmentId));
        if (status != null) {
            status.setWorkStatus("IDLE");
            status.setCurrentTaskNo(null);
            status.setCurrentLocation(location);
            status.setBatteryLevel(endBattery);
            statusMapper.updateById(status);
        }

        // 结束使用记录
        EquipmentUsage usage =
                usageMapper.selectOne(
                        new LambdaQueryWrapper<EquipmentUsage>()
                                .eq(EquipmentUsage::getEquipmentId, equipmentId)
                                .isNull(EquipmentUsage::getEndTime)
                                .orderByDesc(EquipmentUsage::getStartTime)
                                .last("LIMIT 1"));
        if (usage != null) {
            usage.setEndTime(LocalDateTime.now());
            usage.setEndLocation(location);
            usage.setEndBattery(endBattery);
            long minutes =
                    java.time.Duration.between(usage.getStartTime(), LocalDateTime.now())
                            .toMinutes();
            usage.setDurationMin((int) minutes);
            usageMapper.updateById(usage);
        }

        // 累计运行小时和作业次数
        if (usage != null) {
            equip.setTotalRunHours(
                    equip.getTotalRunHours() != null
                            ? equip.getTotalRunHours()
                                    .add(
                                            new BigDecimal(usage.getDurationMin())
                                                    .divide(
                                                            new BigDecimal(60),
                                                            2,
                                                            BigDecimal.ROUND_HALF_UP))
                            : BigDecimal.ZERO);
            equip.setTotalOperations(
                    equip.getTotalOperations() != null ? equip.getTotalOperations() + 1 : 1);
            equipmentMapper.updateById(equip);
        }

        log.info("设备{}释放: 位置={}", equip.getEquipmentCode(), location);
        return equip;
    }

    // ============================================================

    // 4. 设备维护
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public EquipmentMaintain createMaintain(
            Long equipmentId, String type, String faultDesc, String createdBy) {
        Equipment equip = getEquipment(equipmentId);

        String maintainNo = generateMaintainNo();
        EquipmentMaintain maintain = new EquipmentMaintain();
        maintain.setMaintainNo(maintainNo);
        maintain.setEquipmentId(equipmentId);
        maintain.setEquipmentCode(equip.getEquipmentCode());
        maintain.setMaintainType(type);
        maintain.setMaintainStatus("PENDING");
        maintain.setFaultDesc(faultDesc);
        maintain.setCreatedBy(createdBy);
        maintain.setOwnerCodeCol(equip.getOwnerCodeCol());
        maintain.setWarehouseCodeCol(equip.getWarehouseCodeCol());
        maintainMapper.insert(maintain);

        // 设备状态改为维护中
        equip.setStatus(com.xwms.core.equipment.enums.EquipmentStatus.MAINTENANCE.getCode());
        equipmentMapper.updateById(equip);

        log.info("创建设备维护单: {}, 设备={}, 类型={}", maintainNo, equip.getEquipmentCode(), type);
        return maintain;
    }

    @Transactional(rollbackFor = Exception.class)
    public EquipmentMaintain startMaintain(Long maintainId, String maintainBy) {
        EquipmentMaintain maintain = maintainMapper.selectById(maintainId);
        if (maintain == null) throw new BizException("维护单不存在");
        if (!"PENDING".equals(maintain.getMaintainStatus())) {
            throw new BizException("维护单状态不允许开始: " + maintain.getMaintainStatus());
        }
        maintain.setMaintainStatus("PROCESSING");
        maintain.setMaintainBy(maintainBy);
        maintain.setStartTime(LocalDateTime.now());
        maintainMapper.updateById(maintain);
        log.info("维护单{}开始", maintain.getMaintainNo());
        return maintain;
    }

    @Transactional(rollbackFor = Exception.class)
    public EquipmentMaintain completeMaintain(
            Long maintainId, String maintainDesc, BigDecimal cost, String result, String parts) {
        EquipmentMaintain maintain = maintainMapper.selectById(maintainId);
        if (maintain == null) throw new BizException("维护单不存在");
        if (!"PROCESSING".equals(maintain.getMaintainStatus())) {
            throw new BizException("维护单状态不允许完成: " + maintain.getMaintainStatus());
        }

        maintain.setMaintainDesc(maintainDesc);
        maintain.setCostAmount(cost);
        maintain.setResult(result);
        maintain.setPartsReplaced(parts);
        maintain.setEndTime(LocalDateTime.now());
        maintain.setMaintainStatus("COMPLETED");
        long minutes =
                java.time.Duration.between(maintain.getStartTime(), LocalDateTime.now())
                        .toMinutes();
        maintain.setDurationMin((int) minutes);
        maintainMapper.updateById(maintain);

        // 恢复设备状态
        Equipment equip = equipmentMapper.selectById(maintain.getEquipmentId());
        if (equip != null) {
            equip.setStatus(com.xwms.core.equipment.enums.EquipmentStatus.IDLE.getCode());
            equip.setLastMaintain(LocalDate.now());
            if (equip.getMaintainCycleDays() != null) {
                equip.setNextMaintain(LocalDate.now().plusDays(equip.getMaintainCycleDays()));
            }
            equipmentMapper.updateById(equip);
        }

        log.info("维护单{}完成: 结果={}", maintain.getMaintainNo(), result);
        return maintain;
    }

    // ============================================================

    // 5. 设备故障
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public Equipment reportFault(Long equipmentId, String errorCode, String errorMsg) {
        Equipment equip = getEquipment(equipmentId);
        equip.setStatus(com.xwms.core.equipment.enums.EquipmentStatus.FAULT.getCode());
        equipmentMapper.updateById(equip);

        // 更新状态
        EquipmentStatus status =
                statusMapper.selectOne(
                        new LambdaQueryWrapper<EquipmentStatus>()
                                .eq(EquipmentStatus::getEquipmentId, equipmentId));
        if (status != null) {
            status.setRunStatus("ERROR");
            status.setErrorCode(errorCode);
            status.setErrorMsg(errorMsg);
            statusMapper.updateById(status);
        }

        log.warn("设备{}故障: code={}, msg={}", equip.getEquipmentCode(), errorCode, errorMsg);
        return equip;
    }

    // ============================================================

    // 6. 设备状态上报 (WCS/设备心跳)
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public EquipmentStatus reportStatus(
            Long equipmentId,
            String runStatus,
            String workStatus,
            String location,
            BigDecimal battery,
            BigDecimal speed,
            BigDecimal loadWeight,
            String errorCode,
            String errorMsg) {
        EquipmentStatus status =
                statusMapper.selectOne(
                        new LambdaQueryWrapper<EquipmentStatus>()
                                .eq(EquipmentStatus::getEquipmentId, equipmentId));
        if (status == null) throw new BizException("设备状态记录不存在");

        status.setRunStatus(runStatus);
        status.setWorkStatus(workStatus);
        status.setCurrentLocation(location);
        status.setBatteryLevel(battery);
        status.setSpeed(speed);
        status.setLoadWeight(loadWeight);
        status.setErrorCode(errorCode);
        status.setErrorMsg(errorMsg);
        status.setLastHeartbeat(LocalDateTime.now());
        statusMapper.updateById(status);

        // 同步设备档案的位置和电量
        Equipment equip = equipmentMapper.selectById(equipmentId);
        if (equip != null) {
            if (location != null) equip.setLocationCode(location);
            if (battery != null) equip.setBatteryLevel(battery);
            equipmentMapper.updateById(equip);
        }

        return status;
    }

    // ============================================================

    // 7. 查询
    // ============================================================

    public EquipmentStatus getEquipmentStatus(Long equipmentId) {
        return statusMapper.selectOne(
                new LambdaQueryWrapper<EquipmentStatus>()
                        .eq(EquipmentStatus::getEquipmentId, equipmentId));
    }

    public List<EquipmentMaintain> getMaintainRecords(Long equipmentId) {
        return maintainMapper.selectByEquipmentId(equipmentId);
    }

    public List<EquipmentUsage> getUsageRecords(Long equipmentId) {
        return usageMapper.selectByEquipmentId(equipmentId);
    }

    // ============================================================

    // 工具方法
    // ============================================================

    private String generateMaintainNo() {
        return "MT"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", MAINTAIN_SEQ.incrementAndGet() % 1000);
    }
}
