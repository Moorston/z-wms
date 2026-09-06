package com.xwms.core.yard.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.common.core.Result;
import com.xwms.core.yard.dto.AppointmentCreateRequest;
import com.xwms.core.yard.entity.*;
import com.xwms.core.yard.service.YardService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/** 预约与月台管理 Controller */
@Tag(name = "预约与月台", description = "月台管理/预约管理/车辆管理/签到签退")
@RestController
@RequestMapping("/api/yard")
@RequiredArgsConstructor
public class YardController {

    private final YardService yardService;

    // ============================================================

    // 月台管理
    // ============================================================

    @Operation(summary = "创建月台")
    @PostMapping("/docks")
    public Result<Dock> createDock(@RequestBody Dock dock) {
        return Result.success(yardService.createDock(dock));
    }

    @Operation(summary = "查询月台详情")
    @GetMapping("/docks/{id}")
    public Result<Dock> getDock(@PathVariable Long id) {
        return Result.success(yardService.getDock(id));
    }

    @Operation(summary = "查询仓库月台列表")
    @GetMapping("/docks")
    public Result<List<Dock>> getDocks(@RequestParam String warehouse) {
        return Result.success(yardService.getDocksByWarehouse(warehouse));
    }

    @Operation(summary = "查询可用月台")
    @GetMapping("/docks/available")
    public Result<List<Dock>> getAvailableDocks(
            @RequestParam String warehouse, @RequestParam String type) {
        return Result.success(yardService.getAvailableDocks(warehouse, type));
    }

    @Operation(summary = "更新月台状态")
    @PutMapping("/docks/{id}/status")
    public Result<Dock> updateDockStatus(@PathVariable Long id, @RequestParam String status) {
        return Result.success(yardService.updateDockStatus(id, status));
    }

    @Operation(summary = "查询月台使用记录")
    @GetMapping("/docks/{id}/usage")
    public Result<List<DockUsage>> getDockUsage(@PathVariable Long id) {
        return Result.success(yardService.getDockUsage(id));
    }

    // ============================================================

    // 预约管理
    // ============================================================

    @Operation(summary = "创建预约")
    @PostMapping("/appointments")
    public Result<Appointment> createAppointment(@RequestBody AppointmentCreateRequest request) {
        return Result.success(yardService.createAppointment(request));
    }

    @Operation(summary = "确认预约")
    @PutMapping("/appointments/{id}/confirm")
    public Result<Appointment> confirmAppointment(
            @PathVariable Long id, @RequestParam String confirmBy) {
        return Result.success(yardService.confirmAppointment(id, confirmBy));
    }

    @Operation(summary = "取消预约")
    @PutMapping("/appointments/{id}/cancel")
    public Result<Appointment> cancelAppointment(
            @PathVariable Long id, @RequestParam String reason) {
        return Result.success(yardService.cancelAppointment(id, reason));
    }

    @Operation(summary = "到车签到")
    @PutMapping("/appointments/{id}/check-in")
    public Result<Appointment> checkIn(@PathVariable Long id) {
        return Result.success(yardService.checkIn(id));
    }

    @Operation(summary = "开始装卸")
    @PutMapping("/appointments/{id}/start-loading")
    public Result<Appointment> startLoading(@PathVariable Long id) {
        return Result.success(yardService.startLoading(id));
    }

    @Operation(summary = "离场签退")
    @PutMapping("/appointments/{id}/check-out")
    public Result<Appointment> checkOut(@PathVariable Long id) {
        return Result.success(yardService.checkOut(id));
    }

    @Operation(summary = "查询预约详情")
    @GetMapping("/appointments/{id}")
    public Result<Appointment> getAppointment(@PathVariable Long id) {
        return Result.success(yardService.getAppointment(id));
    }

    @Operation(summary = "分页查询预约")
    @GetMapping("/appointments")
    public Result<Page<Appointment>> pageAppointments(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String warehouse,
            @RequestParam(required = false) String type) {
        return Result.success(
                yardService.pageAppointments(new Page<>(page, size), status, warehouse, type));
    }

    @Operation(summary = "按日期查询预约")
    @GetMapping("/appointments/by-date")
    public Result<List<Appointment>> getAppointmentsByDate(
            @RequestParam String warehouse,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return Result.success(yardService.getAppointmentsByDate(warehouse, start, end));
    }

    // ============================================================

    // 车辆管理
    // ============================================================

    @Operation(summary = "按车牌查询车辆")
    @GetMapping("/vehicles/{plateNo}")
    public Result<Vehicle> getVehicle(@PathVariable String plateNo) {
        return Result.success(yardService.getVehicleByPlate(plateNo));
    }
}
