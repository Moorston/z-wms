package com.xwms.integration.api.log;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * API调用日志服务
 *
 * <p>核心能力： 1. 调用日志记录（内存队列批量写入ClickHouse） 2. 日志查询（按时间/API/状态/应用） 3. 错误日志分析 4. 慢调用查询
 *
 * <p>存储：ClickHouse（wms_api_call_log表） 写入策略：内存队列累积，每10秒或100条批量写入
 */
@Slf4j
@Service
public class ApiCallLogService {

    private final JdbcTemplate clickHouseJdbcTemplate;

    /** 内存队列，批量写入 */
    private final ConcurrentLinkedQueue<CallLog> logQueue = new ConcurrentLinkedQueue<>();

    private static final int BATCH_SIZE = 100;

    public ApiCallLogService(JdbcTemplate clickHouseJdbcTemplate) {
        this.clickHouseJdbcTemplate = clickHouseJdbcTemplate;
    }

    @Data
    public static class CallLog {
        private String requestId;
        private String apiId;
        private String apiName;
        private String appId;
        private String method;
        private String path;
        private int status; // HTTP状态码
        private boolean success;
        private long durationMs;
        private String error;
        private String clientIp;
        private LocalDateTime callTime;
    }

    /** 记录调用日志 */
    public void recordLog(
            String requestId,
            String apiId,
            String apiName,
            String appId,
            String method,
            String path,
            int status,
            boolean success,
            long durationMs,
            String error,
            String clientIp) {
        CallLog logEntry = new CallLog();
        logEntry.setRequestId(requestId);
        logEntry.setApiId(apiId);
        logEntry.setApiName(apiName);
        logEntry.setAppId(appId);
        logEntry.setMethod(method);
        logEntry.setPath(path);
        logEntry.setStatus(status);
        logEntry.setSuccess(success);
        logEntry.setDurationMs(durationMs);
        logEntry.setError(error);
        logEntry.setClientIp(clientIp);
        logEntry.setCallTime(LocalDateTime.now());
        logQueue.offer(logEntry);

        // 队列满则立即批量写入
        if (logQueue.size() >= BATCH_SIZE) {
            flushLogs();
        }
    }

    /** 定时批量写入ClickHouse（每10秒） */
    @Scheduled(fixedDelay = 10000)
    public void flushLogs() {
        if (logQueue.isEmpty()) return;

        List<CallLog> batch = new ArrayList<>();
        CallLog logEntry;
        while ((logEntry = logQueue.poll()) != null && batch.size() < BATCH_SIZE) {
            batch.add(logEntry);
        }

        if (batch.isEmpty()) return;

        try {
            // 批量写入ClickHouse
            for (CallLog entry : batch) {
                clickHouseJdbcTemplate.update(
                        "INSERT INTO wms_api_call_log "
                                + "(request_id, api_id, api_name, app_id, method, path, status, "
                                + "success, duration_ms, error, client_ip, call_time) "
                                + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?)",
                        entry.getRequestId(),
                        entry.getApiId(),
                        entry.getApiName(),
                        entry.getAppId(),
                        entry.getMethod(),
                        entry.getPath(),
                        entry.getStatus(),
                        entry.isSuccess() ? 1 : 0,
                        entry.getDurationMs(),
                        entry.getError(),
                        entry.getClientIp(),
                        entry.getCallTime());
            }
            log.debug("API调用日志批量写入: count={}", batch.size());
        } catch (Exception e) {
            log.error("API调用日志写入失败: count={}", batch.size(), e);
        }
    }

    /** 查询调用日志（按时间范围） */
    public List<Map<String, Object>> queryLogs(
            LocalDateTime start,
            LocalDateTime end,
            String apiId,
            String appId,
            Boolean success,
            int limit) {
        StringBuilder sql =
                new StringBuilder(
                        "SELECT request_id, api_id, api_name, app_id, method, path, status, "
                                + "success, duration_ms, error, client_ip, call_time "
                                + "FROM wms_api_call_log WHERE call_time >= ? AND call_time < ?");
        List<Object> params = new ArrayList<>();
        params.add(start);
        params.add(end);

        if (apiId != null) {
            sql.append(" AND api_id = ?");
            params.add(apiId);
        }
        if (appId != null) {
            sql.append(" AND app_id = ?");
            params.add(appId);
        }
        if (success != null) {
            sql.append(" AND success = ?");
            params.add(success ? 1 : 0);
        }

        sql.append(" ORDER BY call_time DESC LIMIT ?");
        params.add(limit);

        return clickHouseJdbcTemplate.queryForList(sql.toString(), params.toArray());
    }

    /** 查询慢调用（耗时超过阈值） */
    public List<Map<String, Object>> querySlowCalls(
            LocalDateTime start, LocalDateTime end, long thresholdMs, int limit) {
        return clickHouseJdbcTemplate.queryForList(
                "SELECT request_id, api_id, api_name, duration_ms, path, call_time "
                        + "FROM wms_api_call_log WHERE call_time >= ? AND call_time < ? "
                        + "AND duration_ms >= ? ORDER BY duration_ms DESC LIMIT ?",
                start,
                end,
                thresholdMs,
                limit);
    }

    /** 查询错误统计 */
    public List<Map<String, Object>> queryErrorStats(LocalDateTime start, LocalDateTime end) {
        return clickHouseJdbcTemplate.queryForList(
                "SELECT api_id, count() as error_count, any(error) as sample_error "
                        + "FROM wms_api_call_log WHERE call_time >= ? AND call_time < ? AND success = 0 "
                        + "GROUP BY api_id ORDER BY error_count DESC LIMIT 20",
                start,
                end);
    }
}
