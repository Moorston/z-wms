package com.xwms.analytics.kpi;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.xwms.analytics.olap.OlapQueryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * KPI计算服务 基于ClickHouse OLAP数据计算仓储运营核心KPI
 *
 * <p>KPI分类： 1. 入库KPI：入库及时率/收货效率/上架效率 2. 出库KPI：出库及时率/拣货效率/订单履约率 3. 库存KPI：库存准确率/周转率/库位利用率 4.
 * 人员KPI：拣货员/复核员/收货员绩效
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KpiCalculationService {

    private final OlapQueryService olapQueryService;

    /** 入库KPI */
    public Map<String, Object> calculateInboundKpi(
            String warehouse, LocalDate start, LocalDate end) {
        Map<String, Object> kpi = new HashMap<>();
        kpi.put("warehouse", warehouse);
        kpi.put("period", start + " ~ " + end);

        try {
            // 入库单总数
            List<Map<String, Object>> total =
                    olapQueryService.aggregate(
                            "wms_inbound_order_olap",
                            "count(*) as total, sum(received_qty) as total_qty",
                            null,
                            "warehouse = '" + warehouse + "' AND created_at >= '" + start + "'",
                            null,
                            null);
            if (!total.isEmpty()) {
                kpi.put("inboundCount", total.get(0).get("total"));
                kpi.put("inboundQty", total.get(0).get("total_qty"));
            }

            // 入库及时率（24小时内完成收货）
            List<Map<String, Object>> onTime =
                    olapQueryService.aggregate(
                            "wms_inbound_order_olap",
                            "count(*) as on_time_count",
                            null,
                            "warehouse = '"
                                    + warehouse
                                    + "' AND status = 'COMPLETED' "
                                    + "AND dateDiff('hour', created_at, updated_at) <= 24",
                            null,
                            null);
            if (!onTime.isEmpty() && total.get(0).get("total") != null) {
                long totalCount = ((Number) total.get(0).get("total")).longValue();
                long onTimeCount = ((Number) onTime.get(0).get("on_time_count")).longValue();
                kpi.put(
                        "onTimeRate",
                        totalCount > 0
                                ? BigDecimal.valueOf(onTimeCount * 100.0 / totalCount)
                                        .setScale(2, BigDecimal.ROUND_HALF_UP)
                                : BigDecimal.ZERO);
            }

        } catch (Exception e) {
            log.error("入库KPI计算失败", e);
            kpi.put("error", e.getMessage());
        }

        return kpi;
    }

    /** 出库KPI */
    public Map<String, Object> calculateOutboundKpi(
            String warehouse, LocalDate start, LocalDate end) {
        Map<String, Object> kpi = new HashMap<>();
        kpi.put("warehouse", warehouse);
        kpi.put("period", start + " ~ " + end);

        try {
            // 出库单统计
            List<Map<String, Object>> stats =
                    olapQueryService.aggregate(
                            "wms_outbound_order_olap",
                            "count(*) as total, "
                                    + "sumIf(shipped_qty, status='SHIPPED') as shipped_qty, "
                                    + "countIf(status='SHIPPED') as shipped_count",
                            null,
                            "warehouse = '" + warehouse + "' AND created_at >= '" + start + "'",
                            null,
                            null);

            if (!stats.isEmpty()) {
                Map<String, Object> row = stats.get(0);
                kpi.put("orderCount", row.get("total"));
                kpi.put("shippedCount", row.get("shipped_count"));
                kpi.put("shippedQty", row.get("shipped_qty"));

                long total = ((Number) row.get("total")).longValue();
                long shipped = ((Number) row.get("shipped_count")).longValue();
                kpi.put(
                        "fulfillmentRate",
                        total > 0
                                ? BigDecimal.valueOf(shipped * 100.0 / total)
                                        .setScale(2, BigDecimal.ROUND_HALF_UP)
                                : BigDecimal.ZERO);
            }

            // 按快递商统计
            List<Map<String, Object>> byExpress =
                    olapQueryService.aggregate(
                            "wms_outbound_order_olap",
                            "count(*) as cnt",
                            List.of("express_code"),
                            "warehouse = '" + warehouse + "' AND status='SHIPPED'",
                            "cnt DESC",
                            10);
            kpi.put("byExpress", byExpress);

        } catch (Exception e) {
            log.error("出库KPI计算失败", e);
            kpi.put("error", e.getMessage());
        }

        return kpi;
    }

    /** 人员绩效KPI */
    public Map<String, Object> calculateStaffKpi(String warehouse, LocalDate start, LocalDate end) {
        Map<String, Object> kpi = new HashMap<>();

        try {
            // 按操作员统计作业量
            List<Map<String, Object>> byOperator =
                    olapQueryService.aggregate(
                            "wms_work_task_olap",
                            "count(*) as task_count, sum(actual_qty) as total_qty, "
                                    + "avg(dateDiff('minute', actual_start_time, completed_time)) as avg_minutes",
                            List.of("operator"),
                            "warehouse = '"
                                    + warehouse
                                    + "' AND status='COMPLETED' "
                                    + "AND completed_time >= '"
                                    + start
                                    + "'",
                            "task_count DESC",
                            20);
            kpi.put("staffRanking", byOperator);

            // 按任务类型统计
            List<Map<String, Object>> byType =
                    olapQueryService.aggregate(
                            "wms_work_task_olap",
                            "count(*) as cnt, avg(dateDiff('minute', actual_start_time, completed_time)) as avg_min",
                            List.of("task_type"),
                            "warehouse = '" + warehouse + "' AND status='COMPLETED'",
                            "cnt DESC",
                            null);
            kpi.put("byTaskType", byType);

        } catch (Exception e) {
            log.error("人员KPI计算失败", e);
            kpi.put("error", e.getMessage());
        }

        return kpi;
    }

    /** 综合看板KPI */
    public Map<String, Object> calculateDashboard(String warehouse) {
        Map<String, Object> dashboard = new HashMap<>();
        LocalDate today = LocalDate.now();

        try {
            // 今日待入库
            List<Map<String, Object>> pendingIn =
                    olapQueryService.aggregate(
                            "wms_inbound_order_olap",
                            "count(*) as cnt",
                            null,
                            "warehouse = '" + warehouse + "' AND status IN ('CREATED','RECEIVING')",
                            null,
                            null);
            dashboard.put("pendingInbound", pendingIn.isEmpty() ? 0 : pendingIn.get(0).get("cnt"));

            // 今日待出库
            List<Map<String, Object>> pendingOut =
                    olapQueryService.aggregate(
                            "wms_outbound_order_olap",
                            "count(*) as cnt",
                            null,
                            "warehouse = '"
                                    + warehouse
                                    + "' AND status IN ('CREATED','ALLOCATED','PICKING')",
                            null,
                            null);
            dashboard.put(
                    "pendingOutbound", pendingOut.isEmpty() ? 0 : pendingOut.get(0).get("cnt"));

            // 今日已发运
            List<Map<String, Object>> shipped =
                    olapQueryService.aggregate(
                            "wms_outbound_order_olap",
                            "count(*) as cnt",
                            null,
                            "warehouse = '"
                                    + warehouse
                                    + "' AND status='SHIPPED' AND toDate(updated_at) = today()",
                            null,
                            null);
            dashboard.put("todayShipped", shipped.isEmpty() ? 0 : shipped.get(0).get("cnt"));

            dashboard.put("date", today.toString());
            dashboard.put("warehouse", warehouse);

        } catch (Exception e) {
            log.error("看板KPI计算失败", e);
            dashboard.put("error", e.getMessage());
        }

        return dashboard;
    }
}
