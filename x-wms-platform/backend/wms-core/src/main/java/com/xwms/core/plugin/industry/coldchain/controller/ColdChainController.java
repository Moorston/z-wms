package com.xwms.core.plugin.industry.coldchain.controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.common.core.Result;
import com.xwms.core.plugin.industry.coldchain.entity.*;
import com.xwms.core.plugin.industry.coldchain.service.ColdChainService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/** 冷链行业控制器 */
@Tag(name = "冷链行业管理", description = "温度监控/报警处理/设备管理/温区管理")
@RestController
@RequestMapping("/api/coldchain")
@RequiredArgsConstructor
public class ColdChainController {

    private final ColdChainService coldChainService;

    // ============================================================

    // 1. 温度监控记录
    // ============================================================

    @Operation(summary = "采集温度数据")
    @PostMapping("/temperature/collect")
    public Result<TemperatureRecord> collectTemperature(
            @RequestParam String warehouseCode,
            @RequestParam(required = false) String areaCode,
            @RequestParam(required = false) String locationCode,
            @RequestParam(required = false) String equipmentCode,
            @RequestParam BigDecimal temperature,
            @RequestParam(required = false) BigDecimal humidity,
            @RequestParam(defaultValue = "AUTO") String collectType,
            @RequestParam(required = false) String collector) {
        return Result.success(
                coldChainService.collectTemperature(
                        warehouseCode,
                        areaCode,
                        locationCode,
                        equipmentCode,
                        temperature,
                        humidity,
                        collectType,
                        collector));
    }

    @Operation(summary = "查询温度记录详情")
    @GetMapping("/temperature/{id}")
    public Result<TemperatureRecord> getRecord(@PathVariable Long id) {
        return Result.success(coldChainService.getRecord(id));
    }

    @Operation(summary = "根据编号查询温度记录")
    @GetMapping("/temperature/no/{recordNo}")
    public Result<TemperatureRecord> getRecordByNo(@PathVariable String recordNo) {
        return Result.success(coldChainService.getRecordByNo(recordNo));
    }

    @Operation(summary = "分页查询温度记录")
    @GetMapping("/temperature")
    public Result<Page<TemperatureRecord>> pageRecords(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String warehouseCode,
            @RequestParam(required = false) String locationCode,
            @RequestParam(required = false) String equipmentCode,
            @RequestParam(required = false) String status) {
        return Result.success(
                coldChainService.pageRecords(
                        new Page<>(page, size),
                        warehouseCode,
                        locationCode,
                        equipmentCode,
                        status));
    }

    // ============================================================

    // 2. 温度报警处理
    // ============================================================

    @Operation(summary = "处理报警")
    @PutMapping("/alert/{id}/handle")
    public Result<TemperatureAlert> handleAlert(
            @PathVariable Long id,
            @RequestParam String handleResult,
            @RequestParam(required = false) String handleRemark,
            @RequestParam String handledBy) {
        return Result.success(
                coldChainService.handleAlert(id, handleResult, handleRemark, handledBy));
    }

    @Operation(summary = "忽略报警")
    @PutMapping("/alert/{id}/ignore")
    public Result<TemperatureAlert> ignoreAlert(
            @PathVariable Long id,
            @RequestParam(required = false) String handleRemark,
            @RequestParam String handledBy) {
        return Result.success(coldChainService.ignoreAlert(id, handleRemark, handledBy));
    }

    @Operation(summary = "查询报警详情")
    @GetMapping("/alert/{id}")
    public Result<TemperatureAlert> getAlert(@PathVariable Long id) {
        return Result.success(coldChainService.getAlert(id));
    }

    @Operation(summary = "根据编号查询报警")
    @GetMapping("/alert/no/{alertNo}")
    public Result<TemperatureAlert> getAlertByNo(@PathVariable String alertNo) {
        return Result.success(coldChainService.getAlertByNo(alertNo));
    }

    @Operation(summary = "查询待处理报警列表")
    @GetMapping("/alert/pending")
    public Result<List<TemperatureAlert>> getPendingAlerts() {
        return Result.success(coldChainService.getPendingAlerts());
    }

    @Operation(summary = "分页查询报警")
    @GetMapping("/alert")
    public Result<Page<TemperatureAlert>> pageAlerts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String warehouseCode,
            @RequestParam(required = false) String locationCode,
            @RequestParam(required = false) String alertType,
            @RequestParam(required = false) String alertLevel,
            @RequestParam(required = false) String status) {
        return Result.success(
                coldChainService.pageAlerts(
                        new Page<>(page, size),
                        warehouseCode,
                        locationCode,
                        alertType,
                        alertLevel,
                        status));
    }

    @Operation(summary = "统计待处理报警数量")
    @GetMapping("/alert/pending/count")
    public Result<Integer> countPendingAlerts() {
        return Result.success(coldChainService.countPendingAlerts());
    }

    // ============================================================

    // 3. 冷链设备管理
    // ============================================================

    @Operation(summary = "创建设备")
    @PostMapping("/equipment")
    public Result<ColdChainEquipment> createEquipment(@RequestBody ColdChainEquipment equipment) {
        return Result.success(coldChainService.createEquipment(equipment));
    }

    @Operation(summary = "更新设备")
    @PutMapping("/equipment/{id}")
    public Result<ColdChainEquipment> updateEquipment(
            @PathVariable Long id, @RequestBody ColdChainEquipment equipment) {
        equipment.setId(id);
        return Result.success(coldChainService.updateEquipment(equipment));
    }

    @Operation(summary = "查询设备详情")
    @GetMapping("/equipment/{id}")
    public Result<ColdChainEquipment> getEquipment(@PathVariable Long id) {
        return Result.success(coldChainService.getEquipment(id));
    }

    @Operation(summary = "根据编号查询设备")
    @GetMapping("/equipment/code/{equipmentCode}")
    public Result<ColdChainEquipment> getEquipmentByCode(@PathVariable String equipmentCode) {
        return Result.success(coldChainService.getEquipmentByCode(equipmentCode));
    }

    @Operation(summary = "分页查询设备")
    @GetMapping("/equipment")
    public Result<Page<ColdChainEquipment>> pageEquipments(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String warehouseCode,
            @RequestParam(required = false) String equipmentType,
            @RequestParam(required = false) String runStatus,
            @RequestParam(required = false) String status) {
        return Result.success(
                coldChainService.pageEquipments(
                        new Page<>(page, size), warehouseCode, equipmentType, runStatus, status));
    }

    @Operation(summary = "查询故障设备列表")
    @GetMapping("/equipment/fault")
    public Result<List<ColdChainEquipment>> getFaultEquipments() {
        return Result.success(coldChainService.getFaultEquipments());
    }

    @Operation(summary = "查询需要维护的设备")
    @GetMapping("/equipment/maintain")
    public Result<List<ColdChainEquipment>> getNeedMaintainEquipments() {
        return Result.success(coldChainService.getNeedMaintainEquipments());
    }

    @Operation(summary = "开始设备维护")
    @PutMapping("/equipment/{id}/maintenance/start")
    public Result<ColdChainEquipment> startMaintenance(
            @PathVariable Long id, @RequestParam(required = false) String remark) {
        return Result.success(coldChainService.startMaintenance(id, remark));
    }

    @Operation(summary = "完成设备维护")
    @PutMapping("/equipment/{id}/maintenance/finish")
    public Result<ColdChainEquipment> finishMaintenance(@PathVariable Long id) {
        return Result.success(coldChainService.finishMaintenance(id));
    }

    // ============================================================

    // 4. 温区管理
    // ============================================================

    @Operation(summary = "创建温区")
    @PostMapping("/zone")
    public Result<TemperatureZone> createZone(@RequestBody TemperatureZone zone) {
        return Result.success(coldChainService.createZone(zone));
    }

    @Operation(summary = "更新温区")
    @PutMapping("/zone/{id}")
    public Result<TemperatureZone> updateZone(
            @PathVariable Long id, @RequestBody TemperatureZone zone) {
        zone.setId(id);
        return Result.success(coldChainService.updateZone(zone));
    }

    @Operation(summary = "查询温区详情")
    @GetMapping("/zone/{id}")
    public Result<TemperatureZone> getZone(@PathVariable Long id) {
        return Result.success(coldChainService.getZone(id));
    }

    @Operation(summary = "根据编号查询温区")
    @GetMapping("/zone/code/{zoneCode}")
    public Result<TemperatureZone> getZoneByCode(@PathVariable String zoneCode) {
        return Result.success(coldChainService.getZoneByCode(zoneCode));
    }

    @Operation(summary = "根据仓库查询温区列表")
    @GetMapping("/zone/warehouse/{warehouseCode}")
    public Result<List<TemperatureZone>> getZonesByWarehouse(@PathVariable String warehouseCode) {
        return Result.success(coldChainService.getZonesByWarehouse(warehouseCode));
    }

    @Operation(summary = "分页查询温区")
    @GetMapping("/zone")
    public Result<Page<TemperatureZone>> pageZones(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String warehouseCode,
            @RequestParam(required = false) String temperatureType,
            @RequestParam(required = false) String status) {
        return Result.success(
                coldChainService.pageZones(
                        new Page<>(page, size), warehouseCode, temperatureType, status));
    }

    @Operation(summary = "根据库区编码查询所属温区")
    @GetMapping("/zone/area/{areaCode}")
    public Result<TemperatureZone> getZoneByAreaCode(@PathVariable String areaCode) {
        return Result.success(coldChainService.getZoneByAreaCode(areaCode));
    }
}
