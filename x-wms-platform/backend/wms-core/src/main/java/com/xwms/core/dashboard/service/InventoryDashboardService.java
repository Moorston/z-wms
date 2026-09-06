package com.xwms.core.dashboard.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.core.dashboard.entity.*;
import com.xwms.core.dashboard.mapper.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryDashboardService {

    private final DashboardConfigMapper dashboardConfigMapper;
    private final RealtimeMonitorMapper realtimeMonitorMapper;
    private final DataBoardMapper dataBoardMapper;
    private final VisualComponentMapper visualComponentMapper;

    private static final AtomicInteger SEQ = new AtomicInteger(0);
    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    // ==================== 大屏配置 ====================

    @Transactional(rollbackFor = Exception.class)
    public DashboardConfig createDashboardConfig(
            String configName,
            String configCode,
            String dashboardType,
            String warehouseCode,
            String ownerCode,
            String description,
            String layoutConfig,
            String themeConfig,
            Integer refreshInterval,
            String isDefault,
            Integer sortOrder,
            String createdBy) {
        DashboardConfig config = new DashboardConfig();
        config.setConfigId(generateId("DC"));
        config.setConfigName(configName);
        config.setConfigCode(configCode);
        config.setDashboardType(dashboardType);
        config.setWarehouseCode(warehouseCode);
        config.setOwnerCode(ownerCode);
        config.setDescription(description);
        config.setLayoutConfig(layoutConfig);
        config.setThemeConfig(themeConfig);
        config.setRefreshInterval(refreshInterval != null ? refreshInterval : 30);
        config.setIsDefault(isDefault != null ? isDefault : "N");
        config.setIsActive("Y");
        config.setSortOrder(sortOrder != null ? sortOrder : 100);
        config.setCreatedBy(createdBy);
        dashboardConfigMapper.insert(config);
        log.info(
                "创建大屏配置: id={}, name={}, type={}", config.getConfigId(), configName, dashboardType);
        return config;
    }

    @Transactional(rollbackFor = Exception.class)
    public DashboardConfig updateDashboardConfig(
            String configId,
            String configName,
            String layoutConfig,
            String themeConfig,
            Integer refreshInterval,
            String isActive,
            String updatedBy) {
        DashboardConfig config = dashboardConfigMapper.selectByConfigId(configId);
        if (config == null) throw new RuntimeException("大屏配置不存在: " + configId);
        if (configName != null) config.setConfigName(configName);
        if (layoutConfig != null) config.setLayoutConfig(layoutConfig);
        if (themeConfig != null) config.setThemeConfig(themeConfig);
        if (refreshInterval != null) config.setRefreshInterval(refreshInterval);
        if (isActive != null) config.setIsActive(isActive);
        config.setUpdatedBy(updatedBy);
        dashboardConfigMapper.updateById(config);
        log.info("更新大屏配置: id={}", configId);
        return dashboardConfigMapper.selectByConfigId(configId);
    }

    @Transactional(rollbackFor = Exception.class)
    public DashboardConfig setDefaultDashboard(
            String configId, String warehouseCode, String updatedBy) {
        // 取消该仓库其他默认大屏
        List<DashboardConfig> configs = dashboardConfigMapper.selectActiveByType(null);
        for (DashboardConfig c : configs) {
            if (warehouseCode.equals(c.getWarehouseCode()) && "Y".equals(c.getIsDefault())) {
                c.setIsDefault("N");
                c.setUpdatedBy(updatedBy);
                dashboardConfigMapper.updateById(c);
            }
        }
        // 设置新的默认大屏
        DashboardConfig config = dashboardConfigMapper.selectByConfigId(configId);
        if (config == null) throw new RuntimeException("大屏配置不存在: " + configId);
        config.setIsDefault("Y");
        config.setUpdatedBy(updatedBy);
        dashboardConfigMapper.updateById(config);
        log.info("设置默认大屏: id={}, warehouse={}", configId, warehouseCode);
        return dashboardConfigMapper.selectByConfigId(configId);
    }

    public DashboardConfig getDefaultDashboard(String warehouseCode) {
        return dashboardConfigMapper.selectDefaultByWarehouse(warehouseCode);
    }

    public List<DashboardConfig> getActiveDashboardsByType(String dashboardType) {
        return dashboardConfigMapper.selectActiveByType(dashboardType);
    }

    public Page<DashboardConfig> pageDashboardConfig(
            Page<DashboardConfig> page,
            String dashboardType,
            String warehouseCode,
            String isActive) {
        LambdaQueryWrapper<DashboardConfig> wrapper = new LambdaQueryWrapper<>();
        if (dashboardType != null) wrapper.eq(DashboardConfig::getDashboardType, dashboardType);
        if (warehouseCode != null) wrapper.eq(DashboardConfig::getWarehouseCode, warehouseCode);
        if (isActive != null) wrapper.eq(DashboardConfig::getIsActive, isActive);
        wrapper.orderByAsc(DashboardConfig::getSortOrder);
        return dashboardConfigMapper.selectPage(page, wrapper);
    }

    // ==================== 实时监控 ====================

    @Transactional(rollbackFor = Exception.class)
    public RealtimeMonitor createRealtimeMonitor(
            String monitorName,
            String monitorType,
            String warehouseCode,
            String ownerCode,
            String description,
            String monitorConfig,
            BigDecimal targetValue,
            BigDecimal thresholdWarning,
            BigDecimal thresholdCritical,
            String unit,
            Integer refreshInterval,
            String operator) {
        RealtimeMonitor monitor = new RealtimeMonitor();
        monitor.setMonitorId(generateId("RM"));
        monitor.setMonitorName(monitorName);
        monitor.setMonitorType(monitorType);
        monitor.setWarehouseCode(warehouseCode);
        monitor.setOwnerCode(ownerCode);
        monitor.setDescription(description);
        monitor.setMonitorConfig(monitorConfig);
        monitor.setTargetValue(targetValue);
        monitor.setThresholdWarning(thresholdWarning);
        monitor.setThresholdCritical(thresholdCritical);
        monitor.setUnit(unit);
        monitor.setStatus("NORMAL");
        monitor.setRefreshInterval(refreshInterval != null ? refreshInterval : 10);
        monitor.setIsActive("Y");
        monitor.setOperator(operator);
        realtimeMonitorMapper.insert(monitor);
        log.info(
                "创建实时监控: id={}, name={}, type={}",
                monitor.getMonitorId(),
                monitorName,
                monitorType);
        return monitor;
    }

    @Transactional(rollbackFor = Exception.class)
    public RealtimeMonitor updateMonitorValue(
            String monitorId, BigDecimal currentValue, String operator) {
        RealtimeMonitor monitor = realtimeMonitorMapper.selectByMonitorId(monitorId);
        if (monitor == null) throw new RuntimeException("监控不存在: " + monitorId);
        // 计算变化率
        if (monitor.getCurrentValue() != null
                && monitor.getCurrentValue().compareTo(BigDecimal.ZERO) != 0) {
            BigDecimal changeRate =
                    currentValue
                            .subtract(monitor.getCurrentValue())
                            .divide(monitor.getCurrentValue(), 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100));
            monitor.setChangeRate(changeRate);
            monitor.setTrend(
                    changeRate.compareTo(BigDecimal.ZERO) > 0
                            ? "UP"
                            : changeRate.compareTo(BigDecimal.ZERO) < 0 ? "DOWN" : "STABLE");
        }
        monitor.setCurrentValue(currentValue);
        monitor.setLastUpdateTime(LocalDateTime.now());
        // 判断状态
        String status = "NORMAL";
        if (monitor.getThresholdCritical() != null
                && currentValue.compareTo(monitor.getThresholdCritical()) >= 0) {
            status = "CRITICAL";
        } else if (monitor.getThresholdWarning() != null
                && currentValue.compareTo(monitor.getThresholdWarning()) >= 0) {
            status = "WARNING";
        }
        monitor.setStatus(status);
        monitor.setOperator(operator);
        realtimeMonitorMapper.updateById(monitor);
        log.info("更新监控值: id={}, value={}, status={}", monitorId, currentValue, status);
        return realtimeMonitorMapper.selectByMonitorId(monitorId);
    }

    public List<RealtimeMonitor> getActiveMonitorsByWarehouse(String warehouseCode) {
        return realtimeMonitorMapper.selectActiveByWarehouse(warehouseCode);
    }

    public List<RealtimeMonitor> getActiveMonitorsByWarehouseAndType(
            String warehouseCode, String monitorType) {
        return realtimeMonitorMapper.selectActiveByWarehouseAndType(warehouseCode, monitorType);
    }

    public List<RealtimeMonitor> getMonitorsByWarehouseAndStatus(
            String warehouseCode, String status) {
        return realtimeMonitorMapper.selectByWarehouseAndStatus(warehouseCode, status);
    }

    public Page<RealtimeMonitor> pageRealtimeMonitor(
            Page<RealtimeMonitor> page,
            String warehouseCode,
            String monitorType,
            String status,
            String isActive) {
        LambdaQueryWrapper<RealtimeMonitor> wrapper = new LambdaQueryWrapper<>();
        if (warehouseCode != null) wrapper.eq(RealtimeMonitor::getWarehouseCode, warehouseCode);
        if (monitorType != null) wrapper.eq(RealtimeMonitor::getMonitorType, monitorType);
        if (status != null) wrapper.eq(RealtimeMonitor::getStatus, status);
        if (isActive != null) wrapper.eq(RealtimeMonitor::getIsActive, isActive);
        wrapper.orderByAsc(RealtimeMonitor::getMonitorType);
        return realtimeMonitorMapper.selectPage(page, wrapper);
    }

    // ==================== 数据看板 ====================

    @Transactional(rollbackFor = Exception.class)
    public DataBoard createDataBoard(
            String boardName,
            String boardCode,
            String boardType,
            String warehouseCode,
            String ownerCode,
            String description,
            String boardConfig,
            String dataSource,
            String chartConfig,
            Integer refreshInterval,
            String periodType,
            Integer sortOrder,
            String createdBy) {
        DataBoard board = new DataBoard();
        board.setBoardId(generateId("DB"));
        board.setBoardName(boardName);
        board.setBoardCode(boardCode);
        board.setBoardType(boardType);
        board.setWarehouseCode(warehouseCode);
        board.setOwnerCode(ownerCode);
        board.setDescription(description);
        board.setBoardConfig(boardConfig);
        board.setDataSource(dataSource);
        board.setChartConfig(chartConfig);
        board.setRefreshInterval(refreshInterval != null ? refreshInterval : 60);
        board.setPeriodType(periodType != null ? periodType : "REAL_TIME");
        board.setIsActive("Y");
        board.setSortOrder(sortOrder != null ? sortOrder : 100);
        board.setCreatedBy(createdBy);
        dataBoardMapper.insert(board);
        log.info("创建数据看板: id={}, name={}, type={}", board.getBoardId(), boardName, boardType);
        return board;
    }

    public List<DataBoard> getActiveBoardsByType(String boardType) {
        return dataBoardMapper.selectActiveByType(boardType);
    }

    public List<DataBoard> getActiveBoardsByWarehouse(String warehouseCode) {
        return dataBoardMapper.selectActiveByWarehouse(warehouseCode);
    }

    public Page<DataBoard> pageDataBoard(
            Page<DataBoard> page, String boardType, String warehouseCode, String isActive) {
        LambdaQueryWrapper<DataBoard> wrapper = new LambdaQueryWrapper<>();
        if (boardType != null) wrapper.eq(DataBoard::getBoardType, boardType);
        if (warehouseCode != null) wrapper.eq(DataBoard::getWarehouseCode, warehouseCode);
        if (isActive != null) wrapper.eq(DataBoard::getIsActive, isActive);
        wrapper.orderByAsc(DataBoard::getSortOrder);
        return dataBoardMapper.selectPage(page, wrapper);
    }

    // ==================== 可视化组件 ====================

    @Transactional(rollbackFor = Exception.class)
    public VisualComponent createVisualComponent(
            String componentName,
            String componentCode,
            String componentType,
            String description,
            String componentConfig,
            String dataConfig,
            String styleConfig,
            String interactionConfig,
            String isBuiltin,
            Integer sortOrder,
            String createdBy) {
        VisualComponent component = new VisualComponent();
        component.setComponentId(generateId("VC"));
        component.setComponentName(componentName);
        component.setComponentCode(componentCode);
        component.setComponentType(componentType);
        component.setDescription(description);
        component.setComponentConfig(componentConfig);
        component.setDataConfig(dataConfig);
        component.setStyleConfig(styleConfig);
        component.setInteractionConfig(interactionConfig);
        component.setIsBuiltin(isBuiltin != null ? isBuiltin : "N");
        component.setIsActive("Y");
        component.setSortOrder(sortOrder != null ? sortOrder : 100);
        component.setCreatedBy(createdBy);
        visualComponentMapper.insert(component);
        log.info(
                "创建可视化组件: id={}, name={}, type={}",
                component.getComponentId(),
                componentName,
                componentType);
        return component;
    }

    public List<VisualComponent> getActiveComponentsByType(String componentType) {
        return visualComponentMapper.selectActiveByType(componentType);
    }

    public List<VisualComponent> getBuiltinActiveComponents() {
        return visualComponentMapper.selectBuiltinActive();
    }

    public Page<VisualComponent> pageVisualComponent(
            Page<VisualComponent> page, String componentType, String isBuiltin, String isActive) {
        LambdaQueryWrapper<VisualComponent> wrapper = new LambdaQueryWrapper<>();
        if (componentType != null) wrapper.eq(VisualComponent::getComponentType, componentType);
        if (isBuiltin != null) wrapper.eq(VisualComponent::getIsBuiltin, isBuiltin);
        if (isActive != null) wrapper.eq(VisualComponent::getIsActive, isActive);
        wrapper.orderByAsc(VisualComponent::getComponentType)
                .orderByAsc(VisualComponent::getSortOrder);
        return visualComponentMapper.selectPage(page, wrapper);
    }

    private String generateId(String prefix) {
        return prefix
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }
}
