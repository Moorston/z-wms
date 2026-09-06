package com.xwms.core.yard.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.common.exception.BizException;
import com.xwms.core.yard.dto.AppointmentCreateRequest;
import com.xwms.core.yard.entity.*;
import com.xwms.core.yard.enums.AppointmentStatus;
import com.xwms.core.yard.enums.DockStatus;
import com.xwms.core.yard.mapper.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 预约与月台核心服务 包含: 月台管理/预约创建/月台分配/到车签到/装卸开始/离场/车辆管理 */
@Slf4j
@Service
@RequiredArgsConstructor
public class YardService {

    private final DockMapper dockMapper;
    private final AppointmentMapper appointmentMapper;
    private final VehicleMapper vehicleMapper;
    private final DockUsageMapper dockUsageMapper;

    private static final AtomicInteger APPOINT_SEQ = new AtomicInteger(0);
    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    // ============================================================

    // 1. 月台管理
    // ============================================================

    public Dock createDock(Dock dock) {
        dock.setStatus(DockStatus.IDLE.getCode());
        dockMapper.insert(dock);
        log.info("创建月台: {}", dock.getDockCode());
        return dock;
    }

    public Dock getDock(Long id) {
        Dock dock = dockMapper.selectById(id);
        if (dock == null) throw new BizException("月台不存在: " + id);
        return dock;
    }

    public List<Dock> getDocksByWarehouse(String warehouse) {
        return dockMapper.selectByWarehouse(warehouse);
    }

    public List<Dock> getAvailableDocks(String warehouse, String type) {
        return dockMapper.selectAvailableDocks(warehouse, type);
    }

    @Transactional(rollbackFor = Exception.class)
    public Dock updateDockStatus(Long dockId, String status) {
        Dock dock = getDock(dockId);
        dock.setStatus(status);
        dockMapper.updateById(dock);
        log.info("月台{}状态更新为: {}", dock.getDockCode(), status);
        return dock;
    }

    // ============================================================

    // 2. 预约创建
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public Appointment createAppointment(AppointmentCreateRequest request) {
        String appointNo = generateAppointNo();

        Appointment appoint = new Appointment();
        appoint.setAppointNo(appointNo);
        appoint.setAppointType(request.getAppointType());
        appoint.setWarehouseCode(request.getWarehouseCode());
        appoint.setCarrierCode(request.getCarrierCode());
        appoint.setCarrierName(request.getCarrierName());
        appoint.setDriverName(request.getDriverName());
        appoint.setDriverPhone(request.getDriverPhone());
        appoint.setPlateNo(request.getPlateNo());
        appoint.setVehicleType(request.getVehicleType());
        appoint.setVehicleLength(request.getVehicleLength());
        appoint.setVehicleWeight(request.getVehicleWeight());
        appoint.setContactName(request.getContactName());
        appoint.setContactPhone(request.getContactPhone());
        appoint.setPlanArriveTime(request.getPlanArriveTime());
        appoint.setPlanLeaveTime(request.getPlanLeaveTime());
        appoint.setStatus(AppointmentStatus.PENDING.getCode());
        appoint.setSourceOrderNo(request.getSourceOrderNo());
        appoint.setSourceOrderType(request.getSourceOrderType());
        appoint.setPalletCount(request.getPalletCount());
        appoint.setPackageCount(request.getPackageCount());
        appoint.setWeight(request.getWeight());
        appoint.setVolume(request.getVolume());
        appoint.setPriority(request.getPriority() != null ? request.getPriority() : 5);
        appoint.setAppointBy(request.getAppointBy());
        appoint.setRemark(request.getRemark());
        appoint.setOwnerCodeCol(request.getOwnerCode());
        appoint.setWarehouseCodeCol(request.getWarehouseCode());

        // 自动分配月台
        if (request.getAutoAssignDock() != null && request.getAutoAssignDock()) {
            Dock dock = assignDock(appoint);
            if (dock != null) {
                appoint.setDockId(dock.getId());
                appoint.setDockCode(dock.getDockCode());
            }
        }

        appointmentMapper.insert(appoint);

        // 登记车辆(如果不存在)
        registerVehicleIfAbsent(request);

        log.info(
                "创建预约: {}, 类型={}, 月台={}",
                appointNo,
                request.getAppointType(),
                appoint.getDockCode());
        return appoint;
    }

    /** 自动分配月台 */
    private Dock assignDock(Appointment appoint) {
        List<Dock> availableDocks =
                dockMapper.selectAvailableDocks(
                        appoint.getWarehouseCode(), appoint.getAppointType());
        if (availableDocks.isEmpty()) {
            log.warn("无可用月台: 仓库={}, 类型={}", appoint.getWarehouseCode(), appoint.getAppointType());
            return null;
        }
        // 简单分配: 取第一个
        return availableDocks.get(0);
    }

    // ============================================================

    // 3. 预约确认/取消
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public Appointment confirmAppointment(Long appointId, String confirmBy) {
        Appointment appoint = getAppointment(appointId);
        if (!AppointmentStatus.PENDING.getCode().equals(appoint.getStatus())) {
            throw new BizException("预约状态不允许确认: " + appoint.getStatus());
        }
        appoint.setStatus(AppointmentStatus.CONFIRMED.getCode());
        appoint.setConfirmBy(confirmBy);
        appoint.setConfirmTime(LocalDateTime.now());
        appointmentMapper.updateById(appoint);

        // 标记月台为已预约
        if (appoint.getDockId() != null) {
            Dock dock = getDock(appoint.getDockId());
            if (DockStatus.IDLE.getCode().equals(dock.getStatus())) {
                dock.setStatus(DockStatus.RESERVED.getCode());
                dockMapper.updateById(dock);
            }
        }

        log.info("预约{}确认", appoint.getAppointNo());
        return appoint;
    }

    @Transactional(rollbackFor = Exception.class)
    public Appointment cancelAppointment(Long appointId, String reason) {
        Appointment appoint = getAppointment(appointId);
        if (AppointmentStatus.COMPLETED.getCode().equals(appoint.getStatus())
                || AppointmentStatus.CANCELLED.getCode().equals(appoint.getStatus())) {
            throw new BizException("预约状态不允许取消: " + appoint.getStatus());
        }
        appoint.setStatus(AppointmentStatus.CANCELLED.getCode());
        appoint.setCancelReason(reason);
        appointmentMapper.updateById(appoint);

        // 释放月台
        releaseDock(appoint.getDockId());

        log.info("预约{}取消: {}", appoint.getAppointNo(), reason);
        return appoint;
    }

    // ============================================================

    // 4. 到车签到
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public Appointment checkIn(Long appointId) {
        Appointment appoint = getAppointment(appointId);
        if (!AppointmentStatus.CONFIRMED.getCode().equals(appoint.getStatus())
                && !AppointmentStatus.ARRIVED.getCode().equals(appoint.getStatus())) {
            throw new BizException("预约状态不允许签到: " + appoint.getStatus());
        }

        appoint.setStatus(AppointmentStatus.CHECKED_IN.getCode());
        appoint.setActualArriveTime(LocalDateTime.now());
        appoint.setCheckInTime(LocalDateTime.now());
        appointmentMapper.updateById(appoint);

        // 标记月台为占用, 创建使用记录
        if (appoint.getDockId() != null) {
            Dock dock = getDock(appoint.getDockId());
            dock.setStatus(DockStatus.OCCUPIED.getCode());
            dockMapper.updateById(dock);

            DockUsage usage = new DockUsage();
            usage.setDockId(dock.getId());
            usage.setDockCode(dock.getDockCode());
            usage.setAppointmentId(appoint.getId());
            usage.setAppointNo(appoint.getAppointNo());
            usage.setWarehouseCode(appoint.getWarehouseCode());
            usage.setPlateNo(appoint.getPlateNo());
            usage.setCarrierName(appoint.getCarrierName());
            usage.setUsageType(appoint.getAppointType());
            usage.setOccupyStart(LocalDateTime.now());
            usage.setStatus("ACTIVE");
            usage.setOwnerCodeCol(appoint.getOwnerCodeCol());
            usage.setWarehouseCodeCol(appoint.getWarehouseCodeCol());
            dockUsageMapper.insert(usage);
        }

        log.info("预约{}签到", appoint.getAppointNo());
        return appoint;
    }

    // ============================================================

    // 5. 开始装卸
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public Appointment startLoading(Long appointId) {
        Appointment appoint = getAppointment(appointId);
        if (!AppointmentStatus.CHECKED_IN.getCode().equals(appoint.getStatus())) {
            throw new BizException("预约状态不允许开始装卸: " + appoint.getStatus());
        }
        appoint.setStatus(AppointmentStatus.LOADING.getCode());
        appointmentMapper.updateById(appoint);
        log.info("预约{}开始装卸", appoint.getAppointNo());
        return appoint;
    }

    // ============================================================

    // 6. 离场签退
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public Appointment checkOut(Long appointId) {
        Appointment appoint = getAppointment(appointId);
        if (!AppointmentStatus.LOADING.getCode().equals(appoint.getStatus())
                && !AppointmentStatus.CHECKED_IN.getCode().equals(appoint.getStatus())) {
            throw new BizException("预约状态不允许签退: " + appoint.getStatus());
        }

        appoint.setStatus(AppointmentStatus.COMPLETED.getCode());
        appoint.setActualLeaveTime(LocalDateTime.now());
        appoint.setCheckOutTime(LocalDateTime.now());
        appointmentMapper.updateById(appoint);

        // 释放月台, 结束使用记录
        if (appoint.getDockId() != null) {
            releaseDock(appoint.getDockId());

            DockUsage usage = dockUsageMapper.selectActiveByDock(appoint.getDockId());
            if (usage != null && usage.getAppointmentId().equals(appointId)) {
                usage.setOccupyEnd(LocalDateTime.now());
                long minutes =
                        java.time.Duration.between(usage.getOccupyStart(), LocalDateTime.now())
                                .toMinutes();
                usage.setDurationMin((int) minutes);
                usage.setStatus("COMPLETED");
                dockUsageMapper.updateById(usage);
            }
        }

        log.info("预约{}离场", appoint.getAppointNo());
        return appoint;
    }

    /** 释放月台 */
    private void releaseDock(Long dockId) {
        if (dockId == null) return;
        int activeCount = appointmentMapper.countActiveByDock(dockId);
        if (activeCount == 0) {
            Dock dock = dockMapper.selectById(dockId);
            if (dock != null
                    && !DockStatus.MAINTENANCE.getCode().equals(dock.getStatus())
                    && !DockStatus.DISABLED.getCode().equals(dock.getStatus())) {
                dock.setStatus(DockStatus.IDLE.getCode());
                dockMapper.updateById(dock);
            }
        }
    }

    // ============================================================

    // 7. 车辆管理
    // ============================================================

    private void registerVehicleIfAbsent(AppointmentCreateRequest request) {
        if (request.getPlateNo() == null) return;
        Vehicle existing =
                vehicleMapper.selectOne(
                        new LambdaQueryWrapper<Vehicle>()
                                .eq(Vehicle::getPlateNo, request.getPlateNo()));
        if (existing == null) {
            Vehicle vehicle = new Vehicle();
            vehicle.setPlateNo(request.getPlateNo());
            vehicle.setVehicleType(request.getVehicleType());
            vehicle.setVehicleLength(request.getVehicleLength());
            vehicle.setVehicleWeight(request.getVehicleWeight());
            vehicle.setCarrierCode(request.getCarrierCode());
            vehicle.setCarrierName(request.getCarrierName());
            vehicle.setDriverName(request.getDriverName());
            vehicle.setDriverPhone(request.getDriverPhone());
            vehicle.setStatus("ACTIVE");
            vehicleMapper.insert(vehicle);
            log.info("登记车辆: {}", request.getPlateNo());
        }
    }

    public Vehicle getVehicleByPlate(String plateNo) {
        return vehicleMapper.selectOne(
                new LambdaQueryWrapper<Vehicle>().eq(Vehicle::getPlateNo, plateNo));
    }

    // ============================================================

    // 8. 查询
    // ============================================================

    public Appointment getAppointment(Long id) {
        Appointment appoint = appointmentMapper.selectById(id);
        if (appoint == null) throw new BizException("预约单不存在: " + id);
        return appoint;
    }

    public Page<Appointment> pageAppointments(
            Page<Appointment> page, String status, String warehouse, String type) {
        LambdaQueryWrapper<Appointment> wrapper = new LambdaQueryWrapper<>();
        if (status != null) wrapper.eq(Appointment::getStatus, status);
        if (warehouse != null) wrapper.eq(Appointment::getWarehouseCode, warehouse);
        if (type != null) wrapper.eq(Appointment::getAppointType, type);
        wrapper.orderByDesc(Appointment::getPlanArriveTime);
        return appointmentMapper.selectPage(page, wrapper);
    }

    public List<Appointment> getAppointmentsByDate(
            String warehouse, LocalDateTime start, LocalDateTime end) {
        return appointmentMapper.selectByDateRange(warehouse, start, end);
    }

    public List<DockUsage> getDockUsage(Long dockId) {
        return dockUsageMapper.selectByDockId(dockId);
    }

    // ============================================================

    // 工具方法
    // ============================================================

    private String generateAppointNo() {
        return "APT"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", APPOINT_SEQ.incrementAndGet() % 1000);
    }
}
