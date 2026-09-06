package com.xwms.analytics.sync;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.xwms.analytics.olap.OlapQueryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 数据同步服务 从业务数据库（Oracle）增量同步到ClickHouse
 *
 * <p>同步策略： 1. 增量同步：基于updated_at时间戳，每5分钟同步一次 2. 全量同步：每天凌晨2点全量同步一次 3. 同步表：入库单/出库单/库存流水/作业任务/批次追踪
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataSyncService {

    private final JdbcTemplate businessJdbcTemplate; // Oracle业务库
    private final JdbcTemplate clickHouseJdbcTemplate; // ClickHouse分析库
    private final OlapQueryService olapQueryService;

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 增量同步入库单（每5分钟） */
    @Scheduled(fixedDelay = 300000, initialDelay = 60000)
    public int syncInboundOrders() {
        LocalDateTime lastSync = olapQueryService.getLastSyncTime("inbound_order");
        String lastSyncStr = lastSync.format(FORMATTER);

        try {
            // 1. 从Oracle查询增量数据
            List<Map<String, Object>> newData =
                    businessJdbcTemplate.queryForList(
                            "SELECT id, order_no, inbound_type, warehouse, owner_code, status, "
                                    + "expected_qty, received_qty, putaway_qty, created_at, updated_at "
                                    + "FROM wms_inbound_order WHERE updated_at > ? AND deleted = 0",
                            lastSyncStr);

            if (newData.isEmpty()) {
                log.debug("入库单无增量数据");
                return 0;
            }

            // 2. 写入ClickHouse（ReplacingMergeTree自动去重）
            for (Map<String, Object> row : newData) {
                clickHouseJdbcTemplate.update(
                        "INSERT INTO wms_inbound_order_olap "
                                + "(id, order_no, inbound_type, warehouse, owner_code, status, "
                                + "expected_qty, received_qty, putaway_qty, created_at, updated_at) "
                                + "VALUES (?,?,?,?,?,?,?,?,?,?,?)",
                        row.get("id"),
                        row.get("order_no"),
                        row.get("inbound_type"),
                        row.get("warehouse"),
                        row.get("owner_code"),
                        row.get("status"),
                        row.get("expected_qty"),
                        row.get("received_qty"),
                        row.get("putaway_qty"),
                        row.get("created_at"),
                        row.get("updated_at"));
            }

            // 3. 更新同步时间
            olapQueryService.updateLastSyncTime("inbound_order", LocalDateTime.now());
            log.info("入库单增量同步完成: count={}", newData.size());
            return newData.size();

        } catch (Exception e) {
            log.error("入库单同步失败", e);
            return 0;
        }
    }

    /** 增量同步出库单（每5分钟） */
    @Scheduled(fixedDelay = 300000, initialDelay = 90000)
    public int syncOutboundOrders() {
        LocalDateTime lastSync = olapQueryService.getLastSyncTime("outbound_order");
        String lastSyncStr = lastSync.format(FORMATTER);

        try {
            List<Map<String, Object>> newData =
                    businessJdbcTemplate.queryForList(
                            "SELECT id, order_no, outbound_type, warehouse, owner_code, customer_code, "
                                    + "status, wave_no, sku, expected_qty, allocated_qty, picked_qty, shipped_qty, "
                                    + "express_code, tracking_no, created_at, updated_at "
                                    + "FROM wms_outbound_order WHERE updated_at > ? AND deleted = 0",
                            lastSyncStr);

            if (newData.isEmpty()) return 0;

            for (Map<String, Object> row : newData) {
                clickHouseJdbcTemplate.update(
                        "INSERT INTO wms_outbound_order_olap "
                                + "(id, order_no, outbound_type, warehouse, owner_code, customer_code, "
                                + "status, wave_no, sku, expected_qty, allocated_qty, picked_qty, shipped_qty, "
                                + "express_code, tracking_no, created_at, updated_at) "
                                + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                        row.get("id"),
                        row.get("order_no"),
                        row.get("outbound_type"),
                        row.get("warehouse"),
                        row.get("owner_code"),
                        row.get("customer_code"),
                        row.get("status"),
                        row.get("wave_no"),
                        row.get("sku"),
                        row.get("expected_qty"),
                        row.get("allocated_qty"),
                        row.get("picked_qty"),
                        row.get("shipped_qty"),
                        row.get("express_code"),
                        row.get("tracking_no"),
                        row.get("created_at"),
                        row.get("updated_at"));
            }

            olapQueryService.updateLastSyncTime("outbound_order", LocalDateTime.now());
            log.info("出库单增量同步完成: count={}", newData.size());
            return newData.size();

        } catch (Exception e) {
            log.error("出库单同步失败", e);
            return 0;
        }
    }

    /** 增量同步作业任务（每5分钟） */
    @Scheduled(fixedDelay = 300000, initialDelay = 120000)
    public int syncWorkTasks() {
        LocalDateTime lastSync = olapQueryService.getLastSyncTime("work_task");
        String lastSyncStr = lastSync.format(FORMATTER);

        try {
            List<Map<String, Object>> newData =
                    businessJdbcTemplate.queryForList(
                            "SELECT id, task_no, task_type, warehouse, status, priority, "
                                    + "order_no, sku, location_code, expected_qty, actual_qty, operator, "
                                    + "actual_start_time, completed_time, created_at, updated_at "
                                    + "FROM wms_work_task WHERE updated_at > ? AND deleted = 0",
                            lastSyncStr);

            if (newData.isEmpty()) return 0;

            for (Map<String, Object> row : newData) {
                clickHouseJdbcTemplate.update(
                        "INSERT INTO wms_work_task_olap "
                                + "(id, task_no, task_type, warehouse, status, priority, "
                                + "order_no, sku, location_code, expected_qty, actual_qty, operator, "
                                + "actual_start_time, completed_time, created_at, updated_at) "
                                + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                        row.get("id"),
                        row.get("task_no"),
                        row.get("task_type"),
                        row.get("warehouse"),
                        row.get("status"),
                        row.get("priority"),
                        row.get("order_no"),
                        row.get("sku"),
                        row.get("location_code"),
                        row.get("expected_qty"),
                        row.get("actual_qty"),
                        row.get("operator"),
                        row.get("actual_start_time"),
                        row.get("completed_time"),
                        row.get("created_at"),
                        row.get("updated_at"));
            }

            olapQueryService.updateLastSyncTime("work_task", LocalDateTime.now());
            log.info("作业任务增量同步完成: count={}", newData.size());
            return newData.size();

        } catch (Exception e) {
            log.error("作业任务同步失败", e);
            return 0;
        }
    }

    /** 全量同步（每天凌晨2点） */
    @Scheduled(cron = "0 0 2 * * ?")
    public void fullSync() {
        log.info("开始全量数据同步...");
        olapQueryService.resetLastSyncTime("inbound_order");
        olapQueryService.resetLastSyncTime("outbound_order");
        olapQueryService.resetLastSyncTime("work_task");
        syncInboundOrders();
        syncOutboundOrders();
        syncWorkTasks();
        log.info("全量数据同步完成");
    }

    /** 增量同步（不重置同步时间点，按上次同步时间增量拉取三表，返回总记录数） */
    public int incrementalSync() {
        log.info("开始增量数据同步...");
        int count = syncInboundOrders() + syncOutboundOrders() + syncWorkTasks();
        log.info("增量数据同步完成: totalCount={}", count);
        return count;
    }
}
