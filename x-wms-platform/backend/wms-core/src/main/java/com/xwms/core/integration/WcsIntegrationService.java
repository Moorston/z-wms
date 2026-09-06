package com.xwms.core.integration;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * WCS（仓库控制系统）集成服务 负责WMS与WCS/自动化设备之间的通信，包括： 1. 设备管理（堆垛机/输送线/AGV/分拣机/升降机） 2. 任务下发（入库/出库/移库/盘点任务） 3.
 * 状态回传（设备状态/任务进度/异常报警） 4. 库存同步（实时库位库存）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WcsIntegrationService {

    private static final AtomicInteger SEQ = new AtomicInteger(0);
    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    // ============================================================

    // 1. 设备管理
    // ============================================================

    /**
     * 查询设备状态
     *
     * @param deviceType 设备类型（null表示所有类型）
     * @return 设备状态列表
     */
    public List<WcsDeviceStatus> queryDeviceStatus(String deviceType) {
        log.info("查询WCS设备状态: deviceType={}", deviceType);

        // TODO: 实际项目中通过Feign调用WCS接口或wms-integration的WCS适配器
        List<WcsDeviceStatus> devices = mockQueryDeviceStatus(deviceType);

        // 更新本地设备状态
        for (WcsDeviceStatus device : devices) {
            updateLocalDeviceStatus(device);
        }

        return devices;
    }

    /**
     * 设备启停控制
     *
     * @param deviceCode 设备编码
     * @param action 操作（START启动/STOP停止/PAUSE暂停/RESUME恢复）
     * @return 是否成功
     */
    public boolean controlDevice(String deviceCode, String action) {
        log.info("WCS设备控制: deviceCode={}, action={}", deviceCode, action);

        try {
            // TODO: 实际项目中调用WCS设备控制接口
            log.info("WCS设备控制成功: deviceCode={}, action={}", deviceCode, action);
            return true;
        } catch (Exception e) {
            log.error(
                    "WCS设备控制失败: deviceCode={}, action={}, error={}",
                    deviceCode,
                    action,
                    e.getMessage());
            return false;
        }
    }

    /** 设备故障复位 */
    public boolean resetDeviceFault(String deviceCode, String faultCode) {
        log.info("WCS设备故障复位: deviceCode={}, faultCode={}", deviceCode, faultCode);

        try {
            // TODO: 实际项目中调用WCS故障复位接口
            log.info("WCS设备故障复位成功: deviceCode={}", deviceCode);
            return true;
        } catch (Exception e) {
            log.error("WCS设备故障复位失败: deviceCode={}, error={}", deviceCode, e.getMessage());
            return false;
        }
    }

    // ============================================================

    // 2. 任务下发
    // ============================================================

    /**
     * 下发入库任务 将商品从收货区搬运到指定库位
     *
     * @param taskNo 任务编号
     * @param sourceLocation 源库位（收货区）
     * @param targetLocation 目标库位
     * @param skuCode 商品编码
     * @param batchNo 批次号
     * @param quantity 数量
     * @return WCS任务编号
     */
    public String sendInboundTask(
            String taskNo,
            String sourceLocation,
            String targetLocation,
            String skuCode,
            String batchNo,
            BigDecimal quantity) {
        log.info(
                "下发WCS入库任务: taskNo={}, from={}, to={}, sku={}, qty={}",
                taskNo,
                sourceLocation,
                targetLocation,
                skuCode,
                quantity);

        try {
            // 构建WCS任务
            WcsTask wcsTask = new WcsTask();
            wcsTask.setWcsTaskNo(generateWcsTaskNo());
            wcsTask.setTaskType("INBOUND");
            wcsTask.setSourceLocation(sourceLocation);
            wcsTask.setTargetLocation(targetLocation);
            wcsTask.setSkuCode(skuCode);
            wcsTask.setBatchNo(batchNo);
            wcsTask.setQuantity(quantity);
            wcsTask.setPriority(5);
            wcsTask.setStatus("PENDING");
            wcsTask.setCreateTime(LocalDateTime.now());

            // TODO: 实际项目中调用WCS任务下发接口
            log.info("WCS入库任务下发成功: wcsTaskNo={}", wcsTask.getWcsTaskNo());

            // 记录任务映射关系
            recordTaskMapping(taskNo, wcsTask.getWcsTaskNo(), "INBOUND");

            return wcsTask.getWcsTaskNo();
        } catch (Exception e) {
            log.error("WCS入库任务下发失败: taskNo={}, error={}", taskNo, e.getMessage());
            throw new RuntimeException("WCS入库任务下发失败: " + e.getMessage(), e);
        }
    }

    /** 下发出库任务 将商品从库位搬运到发货区 */
    public String sendOutboundTask(
            String taskNo,
            String sourceLocation,
            String targetLocation,
            String skuCode,
            String batchNo,
            BigDecimal quantity) {
        log.info(
                "下发WCS出库任务: taskNo={}, from={}, to={}, sku={}, qty={}",
                taskNo,
                sourceLocation,
                targetLocation,
                skuCode,
                quantity);

        try {
            WcsTask wcsTask = new WcsTask();
            wcsTask.setWcsTaskNo(generateWcsTaskNo());
            wcsTask.setTaskType("OUTBOUND");
            wcsTask.setSourceLocation(sourceLocation);
            wcsTask.setTargetLocation(targetLocation);
            wcsTask.setSkuCode(skuCode);
            wcsTask.setBatchNo(batchNo);
            wcsTask.setQuantity(quantity);
            wcsTask.setPriority(5);
            wcsTask.setStatus("PENDING");
            wcsTask.setCreateTime(LocalDateTime.now());

            // TODO: 实际项目中调用WCS任务下发接口
            log.info("WCS出库任务下发成功: wcsTaskNo={}", wcsTask.getWcsTaskNo());

            recordTaskMapping(taskNo, wcsTask.getWcsTaskNo(), "OUTBOUND");
            return wcsTask.getWcsTaskNo();
        } catch (Exception e) {
            log.error("WCS出库任务下发失败: taskNo={}, error={}", taskNo, e.getMessage());
            throw new RuntimeException("WCS出库任务下发失败: " + e.getMessage(), e);
        }
    }

    /** 下发移库任务 */
    public String sendMoveTask(
            String taskNo,
            String sourceLocation,
            String targetLocation,
            String skuCode,
            String batchNo,
            BigDecimal quantity,
            String reason) {
        log.info(
                "下发WCS移库任务: taskNo={}, from={}, to={}, sku={}, qty={}, reason={}",
                taskNo,
                sourceLocation,
                targetLocation,
                skuCode,
                quantity,
                reason);

        try {
            WcsTask wcsTask = new WcsTask();
            wcsTask.setWcsTaskNo(generateWcsTaskNo());
            wcsTask.setTaskType("MOVE");
            wcsTask.setSourceLocation(sourceLocation);
            wcsTask.setTargetLocation(targetLocation);
            wcsTask.setSkuCode(skuCode);
            wcsTask.setBatchNo(batchNo);
            wcsTask.setQuantity(quantity);
            wcsTask.setPriority(3);
            wcsTask.setStatus("PENDING");
            wcsTask.setCreateTime(LocalDateTime.now());
            wcsTask.setRemark(reason);

            // TODO: 实际项目中调用WCS任务下发接口
            log.info("WCS移库任务下发成功: wcsTaskNo={}", wcsTask.getWcsTaskNo());

            recordTaskMapping(taskNo, wcsTask.getWcsTaskNo(), "MOVE");
            return wcsTask.getWcsTaskNo();
        } catch (Exception e) {
            log.error("WCS移库任务下发失败: taskNo={}, error={}", taskNo, e.getMessage());
            throw new RuntimeException("WCS移库任务下发失败: " + e.getMessage(), e);
        }
    }

    /** 取消WCS任务 */
    public boolean cancelTask(String wcsTaskNo, String reason) {
        log.info("取消WCS任务: wcsTaskNo={}, reason={}", wcsTaskNo, reason);

        try {
            // TODO: 实际项目中调用WCS任务取消接口
            log.info("WCS任务取消成功: wcsTaskNo={}", wcsTaskNo);
            return true;
        } catch (Exception e) {
            log.error("WCS任务取消失败: wcsTaskNo={}, error={}", wcsTaskNo, e.getMessage());
            return false;
        }
    }

    // ============================================================

    // 3. 状态回传
    // ============================================================

    /**
     * 接收WCS任务状态回传 WCS回调接口，更新任务状态
     *
     * @param wcsTaskNo WCS任务编号
     * @param status 任务状态（PENDING/RUNNING/COMPLETED/FAILED/ABORTED）
     * @param progress 进度（0-100）
     * @param deviceCode 执行设备
     * @param errorMessage 错误信息
     */
    public boolean receiveTaskStatus(
            String wcsTaskNo,
            String status,
            Integer progress,
            String deviceCode,
            String errorMessage) {
        log.info("接收WCS任务状态回传: wcsTaskNo={}, status={}, progress={}%", wcsTaskNo, status, progress);

        try {
            // 1. 查询任务映射关系
            WcsTaskMapping mapping = queryTaskMapping(wcsTaskNo);
            if (mapping == null) {
                log.warn("未找到WCS任务映射: wcsTaskNo={}", wcsTaskNo);
                return false;
            }

            // 2. 更新WMS任务状态
            updateWmsTaskStatus(mapping.getWmsTaskNo(), status, progress, deviceCode, errorMessage);

            // 3. 如果任务完成，触发后续流程
            if ("COMPLETED".equals(status)) {
                handleTaskCompleted(mapping);
            }

            // 4. 如果任务失败，触发异常处理
            if ("FAILED".equals(status)) {
                handleTaskFailed(mapping, errorMessage);
            }

            log.info(
                    "WCS任务状态回传处理成功: wcsTaskNo={}, wmsTaskNo={}", wcsTaskNo, mapping.getWmsTaskNo());
            return true;
        } catch (Exception e) {
            log.error("WCS任务状态回传处理失败: wcsTaskNo={}, error={}", wcsTaskNo, e.getMessage());
            return false;
        }
    }

    /** 接收WCS设备异常报警 */
    public boolean receiveDeviceAlarm(
            String deviceCode,
            String alarmType,
            String alarmLevel,
            String alarmMessage,
            LocalDateTime alarmTime) {
        log.warn(
                "接收WCS设备异常报警: deviceCode={}, type={}, level={}, msg={}",
                deviceCode,
                alarmType,
                alarmLevel,
                alarmMessage);

        try {
            // 1. 记录设备报警
            recordDeviceAlarm(deviceCode, alarmType, alarmLevel, alarmMessage, alarmTime);

            // 2. 更新设备状态
            updateDeviceStatusToFault(deviceCode, alarmType);

            // 3. 触发报警通知
            triggerAlarmNotification(deviceCode, alarmType, alarmLevel, alarmMessage);

            return true;
        } catch (Exception e) {
            log.error("处理WCS设备报警失败: deviceCode={}, error={}", deviceCode, e.getMessage());
            return false;
        }
    }

    // ============================================================

    // 4. 库存同步
    // ============================================================

    /** 从WCS同步实时库位库存 定时任务调用，确保WMS与WCS库存一致 */
    public int syncLocationInventoryFromWcs(String warehouseCode) {
        log.info("从WCS同步库位库存: warehouse={}", warehouseCode);

        // TODO: 实际项目中调用WCS库存查询接口
        List<WcsLocationInventory> inventories = mockQueryLocationInventory(warehouseCode);

        int syncCount = 0;
        for (WcsLocationInventory inv : inventories) {
            try {
                // 比对WMS与WCS库存，不一致时调整
                reconcileLocationInventory(inv);
                syncCount++;
            } catch (Exception e) {
                log.error("同步库位库存失败: location={}, error={}", inv.getLocationCode(), e.getMessage());
            }
        }

        log.info("从WCS同步库位库存完成: 总数={}, 成功={}", inventories.size(), syncCount);
        return syncCount;
    }

    // ============================================================

    // 5. 辅助方法
    // ============================================================

    private void updateLocalDeviceStatus(WcsDeviceStatus device) {
        // TODO: 实际项目中更新本地设备状态表
        log.debug("更新本地设备状态: deviceCode={}, status={}", device.getDeviceCode(), device.getStatus());
    }

    private void recordTaskMapping(String wmsTaskNo, String wcsTaskNo, String taskType) {
        // TODO: 实际项目中保存任务映射关系
        log.debug("记录任务映射: wmsTaskNo={}, wcsTaskNo={}, type={}", wmsTaskNo, wcsTaskNo, taskType);
    }

    private WcsTaskMapping queryTaskMapping(String wcsTaskNo) {
        // TODO: 实际项目中查询任务映射表
        WcsTaskMapping mapping = new WcsTaskMapping();
        mapping.setWcsTaskNo(wcsTaskNo);
        mapping.setWmsTaskNo("WMS" + wcsTaskNo.substring(3));
        mapping.setTaskType("INBOUND");
        return mapping;
    }

    private void updateWmsTaskStatus(
            String wmsTaskNo,
            String status,
            Integer progress,
            String deviceCode,
            String errorMessage) {
        // TODO: 实际项目中更新WMS任务状态
        log.debug("更新WMS任务状态: wmsTaskNo={}, status={}, progress={}%", wmsTaskNo, status, progress);
    }

    private void handleTaskCompleted(WcsTaskMapping mapping) {
        // TODO: 实际项目中触发后续流程（如入库完成/出库完成）
        log.info("处理WCS任务完成: wmsTaskNo={}, type={}", mapping.getWmsTaskNo(), mapping.getTaskType());
    }

    private void handleTaskFailed(WcsTaskMapping mapping, String errorMessage) {
        // TODO: 实际项目中触发异常处理（如任务重试/人工介入）
        log.error("处理WCS任务失败: wmsTaskNo={}, error={}", mapping.getWmsTaskNo(), errorMessage);
    }

    private void recordDeviceAlarm(
            String deviceCode,
            String alarmType,
            String alarmLevel,
            String alarmMessage,
            LocalDateTime alarmTime) {
        // TODO: 实际项目中保存设备报警记录
        log.warn("记录设备报警: deviceCode={}, type={}, level={}", deviceCode, alarmType, alarmLevel);
    }

    private void updateDeviceStatusToFault(String deviceCode, String alarmType) {
        // TODO: 实际项目中更新设备状态为故障
        log.warn("更新设备状态为故障: deviceCode={}, alarmType={}", deviceCode, alarmType);
    }

    private void triggerAlarmNotification(
            String deviceCode, String alarmType, String alarmLevel, String alarmMessage) {
        // TODO: 实际项目中触发报警通知（短信/邮件/系统消息）
        log.info("触发报警通知: deviceCode={}, level={}", deviceCode, alarmLevel);
    }

    private void reconcileLocationInventory(WcsLocationInventory inv) {
        // TODO: 实际项目中比对并调整库存
        log.debug("比对库位库存: location={}, wcsQty={}", inv.getLocationCode(), inv.getQuantity());
    }

    private String generateWcsTaskNo() {
        return "WCS"
                + NO_FMT.format(LocalDateTime.now())
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }

    // ============================================================

    // 6. 模拟数据
    // ============================================================

    private List<WcsDeviceStatus> mockQueryDeviceStatus(String deviceType) {
        List<WcsDeviceStatus> devices = new ArrayList<>();
        String[] types = {"STACKER", "CONVEYOR", "AGV", "SORTER", "LIFTER"};
        for (int i = 0; i < 5; i++) {
            if (deviceType != null && !deviceType.equals(types[i])) continue;
            WcsDeviceStatus device = new WcsDeviceStatus();
            device.setDeviceCode(types[i] + String.format("%03d", i + 1));
            device.setDeviceType(types[i]);
            device.setStatus(i % 3 == 0 ? "RUNNING" : (i % 3 == 1 ? "IDLE" : "FAULT"));
            device.setCurrentTask(i % 3 == 0 ? "WCS" + NO_FMT.format(LocalDateTime.now()) : null);
            device.setFaultCode(i % 3 == 2 ? "E00" + i : null);
            device.setLastHeartbeat(LocalDateTime.now());
            devices.add(device);
        }
        return devices;
    }

    private List<WcsLocationInventory> mockQueryLocationInventory(String warehouseCode) {
        List<WcsLocationInventory> inventories = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            WcsLocationInventory inv = new WcsLocationInventory();
            inv.setLocationCode("A-01-0" + (i + 1));
            inv.setWarehouseCode(warehouseCode != null ? warehouseCode : "WH01");
            inv.setSkuCode("SKU" + String.format("%03d", i + 1));
            inv.setBatchNo("BATCH20260101");
            inv.setQuantity(new BigDecimal(50 + i * 5));
            inventories.add(inv);
        }
        return inventories;
    }

    // ============================================================

    // 7. 数据模型
    // ============================================================

    @Data
    public static class WcsDeviceStatus {
        private String deviceCode;
        private String deviceType; // STACKER堆垛机/CONVEYOR输送线/AGV/SORTER分拣机/LIFTER升降机
        private String status; // RUNNING运行中/IDLE空闲/FAULT故障/MAINTENANCE维护中/OFFLINE离线
        private String currentTask;
        private String faultCode;
        private String faultMessage;
        private LocalDateTime lastHeartbeat;
    }

    @Data
    public static class WcsTask {
        private String wcsTaskNo;
        private String taskType; // INBOUND入库/OUTBOUND出库/MOVE移库/COUNT盘点
        private String sourceLocation;
        private String targetLocation;
        private String skuCode;
        private String batchNo;
        private BigDecimal quantity;
        private Integer priority;
        private String status; // PENDING待执行/RUNNING执行中/COMPLETED完成/FAILED失败/ABORTED取消
        private String deviceCode;
        private Integer progress;
        private String errorMessage;
        private LocalDateTime createTime;
        private LocalDateTime startTime;
        private LocalDateTime finishTime;
        private String remark;
    }

    @Data
    public static class WcsTaskMapping {
        private String wmsTaskNo;
        private String wcsTaskNo;
        private String taskType;
    }

    @Data
    public static class WcsLocationInventory {
        private String locationCode;
        private String warehouseCode;
        private String skuCode;
        private String batchNo;
        private BigDecimal quantity;
    }
}
