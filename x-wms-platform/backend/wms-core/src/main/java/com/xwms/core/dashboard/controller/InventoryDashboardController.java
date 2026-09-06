package com.xwms.core.dashboard.controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.common.core.Result;
import com.xwms.core.dashboard.entity.*;
import com.xwms.core.dashboard.service.InventoryDashboardService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "库存可视化大屏管理", description = "大屏配置/实时监控/数据看板/可视化组件")
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class InventoryDashboardController {

    private final InventoryDashboardService dashboardService;

    // ==================== 大屏配置 ====================

    @Operation(summary = "创建大屏配置")
    @PostMapping("/config")
    public Result<DashboardConfig> createDashboardConfig(
            @RequestParam String configName,
            @RequestParam String configCode,
            @RequestParam String dashboardType,
            @RequestParam(required = false) String warehouseCode,
            @RequestParam(required = false) String ownerCode,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String layoutConfig,
            @RequestParam(required = false) String themeConfig,
            @RequestParam(required = false) Integer refreshInterval,
            @RequestParam(required = false) String isDefault,
            @RequestParam(required = false) Integer sortOrder,
            @RequestParam String createdBy) {
        return Result.success(
                dashboardService.createDashboardConfig(
                        configName,
                        configCode,
                        dashboardType,
                        warehouseCode,
                        ownerCode,
                        description,
                        layoutConfig,
                        themeConfig,
                        refreshInterval,
                        isDefault,
                        sortOrder,
                        createdBy));
    }

    @Operation(summary = "更新大屏配置")
    @PutMapping("/config/{configId}")
    public Result<DashboardConfig> updateDashboardConfig(
            @PathVariable String configId,
            @RequestParam(required = false) String configName,
            @RequestParam(required = false) String layoutConfig,
            @RequestParam(required = false) String themeConfig,
            @RequestParam(required = false) Integer refreshInterval,
            @RequestParam(required = false) String isActive,
            @RequestParam String updatedBy) {
        return Result.success(
                dashboardService.updateDashboardConfig(
                        configId,
                        configName,
                        layoutConfig,
                        themeConfig,
                        refreshInterval,
                        isActive,
                        updatedBy));
    }

    @Operation(summary = "设置默认大屏")
    @PostMapping("/config/{configId}/default")
    public Result<DashboardConfig> setDefaultDashboard(
            @PathVariable String configId,
            @RequestParam String warehouseCode,
            @RequestParam String updatedBy) {
        return Result.success(
                dashboardService.setDefaultDashboard(configId, warehouseCode, updatedBy));
    }

    @Operation(summary = "获取默认大屏")
    @GetMapping("/config/default")
    public Result<DashboardConfig> getDefaultDashboard(@RequestParam String warehouseCode) {
        return Result.success(dashboardService.getDefaultDashboard(warehouseCode));
    }

    @Operation(summary = "按类型获取活跃大屏")
    @GetMapping("/config/type/{dashboardType}")
    public Result<List<DashboardConfig>> getActiveDashboardsByType(
            @PathVariable String dashboardType) {
        return Result.success(dashboardService.getActiveDashboardsByType(dashboardType));
    }

    @Operation(summary = "分页查询大屏配置")
    @GetMapping("/config/list")
    public Result<Page<DashboardConfig>> pageDashboardConfig(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String dashboardType,
            @RequestParam(required = false) String warehouseCode,
            @RequestParam(required = false) String isActive) {
        return Result.success(
                dashboardService.pageDashboardConfig(
                        new Page<>(page, size), dashboardType, warehouseCode, isActive));
    }

    // ==================== 实时监控 ====================

    @Operation(summary = "创建实时监控")
    @PostMapping("/monitor")
    public Result<RealtimeMonitor> createRealtimeMonitor(
            @RequestParam String monitorName,
            @RequestParam String monitorType,
            @RequestParam String warehouseCode,
            @RequestParam(required = false) String ownerCode,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String monitorConfig,
            @RequestParam(required = false) BigDecimal targetValue,
            @RequestParam(required = false) BigDecimal thresholdWarning,
            @RequestParam(required = false) BigDecimal thresholdCritical,
            @RequestParam(required = false) String unit,
            @RequestParam(required = false) Integer refreshInterval,
            @RequestParam String operator) {
        return Result.success(
                dashboardService.createRealtimeMonitor(
                        monitorName,
                        monitorType,
                        warehouseCode,
                        ownerCode,
                        description,
                        monitorConfig,
                        targetValue,
                        thresholdWarning,
                        thresholdCritical,
                        unit,
                        refreshInterval,
                        operator));
    }

    @Operation(summary = "更新监控值")
    @PostMapping("/monitor/{monitorId}/value")
    public Result<RealtimeMonitor> updateMonitorValue(
            @PathVariable String monitorId,
            @RequestParam BigDecimal currentValue,
            @RequestParam String operator) {
        return Result.success(
                dashboardService.updateMonitorValue(monitorId, currentValue, operator));
    }

    @Operation(summary = "按仓库获取活跃监控")
    @GetMapping("/monitor/warehouse/{warehouseCode}")
    public Result<List<RealtimeMonitor>> getActiveMonitorsByWarehouse(
            @PathVariable String warehouseCode) {
        return Result.success(dashboardService.getActiveMonitorsByWarehouse(warehouseCode));
    }

    @Operation(summary = "按仓库和类型获取活跃监控")
    @GetMapping("/monitor/warehouse-type")
    public Result<List<RealtimeMonitor>> getActiveMonitorsByWarehouseAndType(
            @RequestParam String warehouseCode, @RequestParam String monitorType) {
        return Result.success(
                dashboardService.getActiveMonitorsByWarehouseAndType(warehouseCode, monitorType));
    }

    @Operation(summary = "按仓库和状态获取监控")
    @GetMapping("/monitor/warehouse-status")
    public Result<List<RealtimeMonitor>> getMonitorsByWarehouseAndStatus(
            @RequestParam String warehouseCode, @RequestParam String status) {
        return Result.success(
                dashboardService.getMonitorsByWarehouseAndStatus(warehouseCode, status));
    }

    @Operation(summary = "分页查询实时监控")
    @GetMapping("/monitor/list")
    public Result<Page<RealtimeMonitor>> pageRealtimeMonitor(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String warehouseCode,
            @RequestParam(required = false) String monitorType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String isActive) {
        return Result.success(
                dashboardService.pageRealtimeMonitor(
                        new Page<>(page, size), warehouseCode, monitorType, status, isActive));
    }

    // ==================== 数据看板 ====================

    @Operation(summary = "创建数据看板")
    @PostMapping("/board")
    public Result<DataBoard> createDataBoard(
            @RequestParam String boardName,
            @RequestParam String boardCode,
            @RequestParam String boardType,
            @RequestParam(required = false) String warehouseCode,
            @RequestParam(required = false) String ownerCode,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String boardConfig,
            @RequestParam(required = false) String dataSource,
            @RequestParam(required = false) String chartConfig,
            @RequestParam(required = false) Integer refreshInterval,
            @RequestParam(required = false) String periodType,
            @RequestParam(required = false) Integer sortOrder,
            @RequestParam String createdBy) {
        return Result.success(
                dashboardService.createDataBoard(
                        boardName,
                        boardCode,
                        boardType,
                        warehouseCode,
                        ownerCode,
                        description,
                        boardConfig,
                        dataSource,
                        chartConfig,
                        refreshInterval,
                        periodType,
                        sortOrder,
                        createdBy));
    }

    @Operation(summary = "按类型获取活跃看板")
    @GetMapping("/board/type/{boardType}")
    public Result<List<DataBoard>> getActiveBoardsByType(@PathVariable String boardType) {
        return Result.success(dashboardService.getActiveBoardsByType(boardType));
    }

    @Operation(summary = "按仓库获取活跃看板")
    @GetMapping("/board/warehouse/{warehouseCode}")
    public Result<List<DataBoard>> getActiveBoardsByWarehouse(@PathVariable String warehouseCode) {
        return Result.success(dashboardService.getActiveBoardsByWarehouse(warehouseCode));
    }

    @Operation(summary = "分页查询数据看板")
    @GetMapping("/board/list")
    public Result<Page<DataBoard>> pageDataBoard(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String boardType,
            @RequestParam(required = false) String warehouseCode,
            @RequestParam(required = false) String isActive) {
        return Result.success(
                dashboardService.pageDataBoard(
                        new Page<>(page, size), boardType, warehouseCode, isActive));
    }

    // ==================== 可视化组件 ====================

    @Operation(summary = "创建可视化组件")
    @PostMapping("/component")
    public Result<VisualComponent> createVisualComponent(
            @RequestParam String componentName,
            @RequestParam String componentCode,
            @RequestParam String componentType,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String componentConfig,
            @RequestParam(required = false) String dataConfig,
            @RequestParam(required = false) String styleConfig,
            @RequestParam(required = false) String interactionConfig,
            @RequestParam(required = false) String isBuiltin,
            @RequestParam(required = false) Integer sortOrder,
            @RequestParam String createdBy) {
        return Result.success(
                dashboardService.createVisualComponent(
                        componentName,
                        componentCode,
                        componentType,
                        description,
                        componentConfig,
                        dataConfig,
                        styleConfig,
                        interactionConfig,
                        isBuiltin,
                        sortOrder,
                        createdBy));
    }

    @Operation(summary = "按类型获取活跃组件")
    @GetMapping("/component/type/{componentType}")
    public Result<List<VisualComponent>> getActiveComponentsByType(
            @PathVariable String componentType) {
        return Result.success(dashboardService.getActiveComponentsByType(componentType));
    }

    @Operation(summary = "获取内置活跃组件")
    @GetMapping("/component/builtin")
    public Result<List<VisualComponent>> getBuiltinActiveComponents() {
        return Result.success(dashboardService.getBuiltinActiveComponents());
    }

    @Operation(summary = "分页查询可视化组件")
    @GetMapping("/component/list")
    public Result<Page<VisualComponent>> pageVisualComponent(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String componentType,
            @RequestParam(required = false) String isBuiltin,
            @RequestParam(required = false) String isActive) {
        return Result.success(
                dashboardService.pageVisualComponent(
                        new Page<>(page, size), componentType, isBuiltin, isActive));
    }
}
