package com.xwms.core.equipment.controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.common.core.Result;
import com.xwms.core.equipment.entity.*;
import com.xwms.core.equipment.service.EquipmentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/** 设备管理 Controller */
@Tag(name = "设备管理", description = "设备档案/设备分配/设备维护/状态上报")
@RestController
@RequestMapping("/api/equipment")
@RequiredArgsConstructor
public class EquipmentController {

    private final EquipmentService equipmentService;

    // ============================================================

    // 设备档案
    // ============================================================

    @Operation(summary = "创建设备")
    @PostMapping
    public Result<Equipment> createEquipment(@RequestBody Equipment equipment) {
        return Result.success(equipmentService.createEquipment(equipment));
    }

    @Operation(summary = "查询设备详情")
    @GetMapping("/{id}")
    public Result<Equipment> getEquipment(@PathVariable Long id) {
        return Result.success(equipmentService.getEquipment(id));
    }

    @Operation(summary = "分页查询设备")
    @GetMapping
    public Result<Page<Equipment>> pageEquipments(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String warehouse) {
        return Result.success(
                equipmentService.pageEquipments(new Page<>(page, size), type, status, warehouse));
    }

    @Operation(summary = "查询可用设备")
    @GetMapping("/available")
    public Result<List<Equipment>> getAvailableEquipments(
            @RequestParam String warehouse, @RequestParam String type) {
        return Result.success(equipmentService.getAvailableEquipments(warehouse, type));
    }

    // ============================================================

    // 设备分配与释放
    // ============================================================

    @Operation(summary = "分配设备")
    @PutMapping("/{id}/assign")
    public Result<Equipment> assignEquipment(
            @PathVariable Long id, @RequestParam String taskNo, @RequestParam String operator) {
        return Result.success(equipmentService.assignEquipment(id, taskNo, operator));
    }

    @Operation(summary = "释放设备")
    @PutMapping("/{id}/release")
    public Result<Equipment> releaseEquipment(
            @PathVariable Long id,
            @RequestParam String location,
            @RequestParam(required = false) BigDecimal endBattery) {
        return Result.success(equipmentService.releaseEquipment(id, location, endBattery));
    }

    // ============================================================

    // 设备维护
    // ============================================================

    @Operation(summary = "创建设备维护单")
    @PostMapping("/{id}/maintain")
    public Result<EquipmentMaintain> createMaintain(
            @PathVariable Long id,
            @RequestParam String type,
            @RequestParam(required = false) String faultDesc,
            @RequestParam String createdBy) {
        return Result.success(equipmentService.createMaintain(id, type, faultDesc, createdBy));
    }

    @Operation(summary = "开始维护")
    @PutMapping("/maintain/{maintainId}/start")
    public Result<EquipmentMaintain> startMaintain(
            @PathVariable Long maintainId, @RequestParam String maintainBy) {
        return Result.success(equipmentService.startMaintain(maintainId, maintainBy));
    }

    @Operation(summary = "完成维护")
    @PutMapping("/maintain/{maintainId}/complete")
    public Result<EquipmentMaintain> completeMaintain(
            @PathVariable Long maintainId,
            @RequestParam String maintainDesc,
            @RequestParam(required = false) BigDecimal cost,
            @RequestParam String result,
            @RequestParam(required = false) String parts) {
        return Result.success(
                equipmentService.completeMaintain(maintainId, maintainDesc, cost, result, parts));
    }

    @Operation(summary = "查询设备维护记录")
    @GetMapping("/{id}/maintain")
    public Result<List<EquipmentMaintain>> getMaintainRecords(@PathVariable Long id) {
        return Result.success(equipmentService.getMaintainRecords(id));
    }

    // ============================================================

    // 设备故障
    // ============================================================

    @Operation(summary = "上报设备故障")
    @PutMapping("/{id}/fault")
    public Result<Equipment> reportFault(
            @PathVariable Long id,
            @RequestParam(required = false) String errorCode,
            @RequestParam(required = false) String errorMsg) {
        return Result.success(equipmentService.reportFault(id, errorCode, errorMsg));
    }

    // ============================================================

    // 设备状态
    // ============================================================

    @Operation(summary = "查询设备实时状态")
    @GetMapping("/{id}/status")
    public Result<EquipmentStatus> getEquipmentStatus(@PathVariable Long id) {
        return Result.success(equipmentService.getEquipmentStatus(id));
    }

    @Operation(summary = "设备状态上报(心跳)")
    @PutMapping("/{id}/status")
    public Result<EquipmentStatus> reportStatus(
            @PathVariable Long id,
            @RequestParam(required = false) String runStatus,
            @RequestParam(required = false) String workStatus,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) BigDecimal battery,
            @RequestParam(required = false) BigDecimal speed,
            @RequestParam(required = false) BigDecimal loadWeight,
            @RequestParam(required = false) String errorCode,
            @RequestParam(required = false) String errorMsg) {
        return Result.success(
                equipmentService.reportStatus(
                        id,
                        runStatus,
                        workStatus,
                        location,
                        battery,
                        speed,
                        loadWeight,
                        errorCode,
                        errorMsg));
    }

    // ============================================================

    // 设备使用记录
    // ============================================================

    @Operation(summary = "查询设备使用记录")
    @GetMapping("/{id}/usage")
    public Result<List<EquipmentUsage>> getUsageRecords(@PathVariable Long id) {
        return Result.success(equipmentService.getUsageRecords(id));
    }
}
