package com.xwms.analytics.report;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.xwms.analytics.olap.OlapQueryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 报表服务 基于ClickHouse的多维报表查询
 *
 * <p>报表类型： 1. 库存报表（库存快照/库存变动/库龄分析） 2. 入库报表（入库明细/入库汇总/供应商分析） 3. 出库报表（出库明细/出库汇总/客户分析/快递分析） 4.
 * 作业报表（作业效率/人员绩效/异常统计）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private final OlapQueryService olapQueryService;

    /** 入库明细报表 */
    public Map<String, Object> inboundDetailReport(
            String warehouse,
            LocalDate start,
            LocalDate end,
            String inboundType,
            int page,
            int size) {
        Map<String, Object> report = new HashMap<>();
        StringBuilder where = new StringBuilder("warehouse = '" + warehouse + "'");
        where.append(" AND created_at >= '").append(start).append("'");
        where.append(" AND created_at < '").append(end.plusDays(1)).append("'");
        if (inboundType != null) {
            where.append(" AND inbound_type = '").append(inboundType).append("'");
        }

        List<Map<String, Object>> data =
                olapQueryService.aggregate(
                        "wms_inbound_order_olap",
                        "order_no, inbound_type, owner_code, status, expected_qty, received_qty, putaway_qty, created_at",
                        null,
                        where.toString(),
                        "created_at DESC",
                        size);

        report.put("warehouse", warehouse);
        report.put("period", start + " ~ " + end);
        report.put("data", data);
        report.put("page", page);
        report.put("size", size);
        return report;
    }

    /** 出库汇总报表（按天） */
    public Map<String, Object> outboundDailyReport(
            String warehouse, LocalDate start, LocalDate end) {
        Map<String, Object> report = new HashMap<>();

        List<Map<String, Object>> dailyData =
                olapQueryService.dailyTrend(
                        "wms_outbound_order_olap",
                        "count(*) as order_count, sum(shipped_qty) as shipped_qty",
                        "created_at",
                        start,
                        end);

        report.put("warehouse", warehouse);
        report.put("period", start + " ~ " + end);
        report.put("dailyData", dailyData);
        return report;
    }

    /** 快递商分析报表 */
    public Map<String, Object> expressAnalysisReport(
            String warehouse, LocalDate start, LocalDate end) {
        Map<String, Object> report = new HashMap<>();

        List<Map<String, Object>> data =
                olapQueryService.aggregate(
                        "wms_outbound_order_olap",
                        "express_code, count(*) as order_count, sum(shipped_qty) as total_qty",
                        List.of("express_code"),
                        "warehouse = '"
                                + warehouse
                                + "' AND status='SHIPPED' "
                                + "AND updated_at >= '"
                                + start
                                + "' AND updated_at < '"
                                + end.plusDays(1)
                                + "'",
                        "order_count DESC",
                        null);

        report.put("warehouse", warehouse);
        report.put("period", start + " ~ " + end);
        report.put("data", data);
        return report;
    }

    /** 作业效率报表（按天） */
    public Map<String, Object> operationEfficiencyReport(
            String warehouse, LocalDate start, LocalDate end) {
        Map<String, Object> report = new HashMap<>();

        List<Map<String, Object>> data =
                olapQueryService.aggregate(
                        "wms_work_task_olap",
                        "task_type, count(*) as task_count, "
                                + "avg(dateDiff('minute', actual_start_time, completed_time)) as avg_minutes, "
                                + "sum(actual_qty) as total_qty",
                        List.of("task_type"),
                        "warehouse = '"
                                + warehouse
                                + "' AND status='COMPLETED' "
                                + "AND completed_time >= '"
                                + start
                                + "' AND completed_time < '"
                                + end.plusDays(1)
                                + "'",
                        "task_count DESC",
                        null);

        report.put("warehouse", warehouse);
        report.put("period", start + " ~ " + end);
        report.put("data", data);
        return report;
    }

    /** 人员绩效排行榜 */
    public Map<String, Object> staffRankingReport(
            String warehouse, LocalDate start, LocalDate end, int topN) {
        Map<String, Object> report = new HashMap<>();

        List<Map<String, Object>> data =
                olapQueryService.topN(
                        "wms_work_task_olap",
                        "operator",
                        "count(*)",
                        topN,
                        "warehouse = '"
                                + warehouse
                                + "' AND status='COMPLETED' "
                                + "AND completed_time >= '"
                                + start
                                + "'");

        report.put("warehouse", warehouse);
        report.put("period", start + " ~ " + end);
        report.put("ranking", data);
        return report;
    }
}
