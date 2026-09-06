package com.xwms.analytics.olap;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * OLAP查询服务 基于ClickHouse的多维分析查询
 *
 * <p>核心能力： 1. 同步时间管理（增量同步水位线） 2. 通用聚合查询（按维度分组统计） 3. 趋势查询（按时间维度） 4. TopN查询
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OlapQueryService {

    private final JdbcTemplate clickHouseJdbcTemplate;

    /** 内存中的同步水位线（生产环境应持久化到ClickHouse系统表） */
    private final Map<String, LocalDateTime> lastSyncMap = new HashMap<>();

    /** 获取表的最后同步时间 */
    public LocalDateTime getLastSyncTime(String tableName) {
        return lastSyncMap.getOrDefault(tableName, LocalDateTime.now().minusDays(1));
    }

    /** 更新表的最后同步时间 */
    public void updateLastSyncTime(String tableName, LocalDateTime time) {
        lastSyncMap.put(tableName, time);
    }

    /** 重置同步时间（全量同步用） */
    public void resetLastSyncTime(String tableName) {
        lastSyncMap.put(tableName, LocalDateTime.of(2020, 1, 1, 0, 0));
    }

    /**
     * 通用聚合查询
     *
     * @param table 表名
     * @param metrics 指标字段（如 count(*), sum(qty)）
     * @param dims 维度字段（如 warehouse, status）
     * @param where 条件
     * @param orderBy 排序
     * @param limit 限制
     */
    public List<Map<String, Object>> aggregate(
            String table,
            String metrics,
            List<String> dims,
            String where,
            String orderBy,
            Integer limit) {
        StringBuilder sql = new StringBuilder("SELECT ");
        sql.append(metrics);
        if (dims != null && !dims.isEmpty()) {
            sql.append(", ").append(String.join(", ", dims));
        }
        sql.append(" FROM ").append(table);
        if (where != null && !where.isEmpty()) {
            sql.append(" WHERE ").append(where);
        }
        if (dims != null && !dims.isEmpty()) {
            sql.append(" GROUP BY ").append(String.join(", ", dims));
        }
        if (orderBy != null && !orderBy.isEmpty()) {
            sql.append(" ORDER BY ").append(orderBy);
        }
        if (limit != null) {
            sql.append(" LIMIT ").append(limit);
        }

        log.debug("OLAP查询: {}", sql);
        return clickHouseJdbcTemplate.queryForList(sql.toString());
    }

    /** 按天趋势查询 */
    public List<Map<String, Object>> dailyTrend(
            String table, String metric, String dateField, LocalDate start, LocalDate end) {
        String sql =
                "SELECT toDate("
                        + dateField
                        + ") as date, "
                        + metric
                        + " FROM "
                        + table
                        + " WHERE "
                        + dateField
                        + " >= ? AND "
                        + dateField
                        + " < ?"
                        + " GROUP BY date ORDER BY date";
        return clickHouseJdbcTemplate.queryForList(sql, start, end.plusDays(1));
    }

    /** TopN查询 */
    public List<Map<String, Object>> topN(
            String table, String dimension, String metric, int n, String where) {
        String sql = "SELECT " + dimension + ", " + metric + " as value" + " FROM " + table;
        if (where != null && !where.isEmpty()) {
            sql += " WHERE " + where;
        }
        sql += " GROUP BY " + dimension + " ORDER BY value DESC LIMIT " + n;
        return clickHouseJdbcTemplate.queryForList(sql);
    }
}
