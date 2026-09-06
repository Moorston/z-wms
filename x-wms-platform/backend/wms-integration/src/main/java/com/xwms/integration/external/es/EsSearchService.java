package com.xwms.integration.external.es;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.xwms.integration.external.es.document.ApiCallLogDocument;
import com.xwms.integration.external.es.document.CallbackRecordDocument;
import com.xwms.integration.external.es.document.IntegrationMessageDocument;
import com.xwms.integration.external.es.dto.EsSearchRequest;
import com.xwms.integration.external.es.dto.EsSearchResponse;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.json.JsonData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * ES 全文检索服务
 *
 * <p>提供：
 *
 * <ul>
 *   <li>API 调用日志搜索
 *   <li>集成消息流水搜索
 *   <li>回调记录搜索
 *   <li>跨索引聚合搜索
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EsSearchService {

    private final EsSyncClient esSyncClient;

    private static final String INDEX_API_CALL_LOG = "wms-api-call-log";
    private static final String INDEX_INTEGRATION_MESSAGE = "wms-integration-message";
    private static final String INDEX_CALLBACK_RECORD = "wms-callback-record";

    private static final List<String> FULLTEXT_FIELDS =
            List.of(
                    "requestBody",
                    "responseBody",
                    "errorMsg",
                    "businessNo",
                    "payload",
                    "requestHeaders");

    private static final List<String> LOG_FULLTEXT_FIELDS =
            List.of("requestBody", "responseBody", "errorMsg", "businessNo", "requestHeaders");

    // ============================================================
    // API 调用日志搜索
    // ============================================================

    public EsSearchResponse<ApiCallLogDocument> searchApiCallLogs(EsSearchRequest req) {
        try {
            ElasticsearchClient client = esSyncClient.getClient();
            Query query = buildQuery(req, LOG_FULLTEXT_FIELDS);

            SearchResponse<ApiCallLogDocument> resp =
                    client.search(
                            buildSearchRequest(INDEX_API_CALL_LOG, req, query),
                            ApiCallLogDocument.class);

            List<ApiCallLogDocument> items =
                    resp.hits().hits().stream()
                            .map(hit -> hit.source())
                            .collect(Collectors.toList());

            return new EsSearchResponse<>(
                    items,
                    resp.hits().total() != null ? resp.hits().total().value() : 0,
                    req.getPage(),
                    req.getSize());
        } catch (Exception e) {
            log.error("搜索 API 调用日志失败: {}", e.getMessage());
            return new EsSearchResponse<>(List.of(), 0, req.getPage(), req.getSize());
        }
    }

    // ============================================================
    // 集成消息搜索
    // ============================================================

    public EsSearchResponse<IntegrationMessageDocument> searchMessages(EsSearchRequest req) {
        try {
            ElasticsearchClient client = esSyncClient.getClient();
            Query query = buildQuery(req, List.of("payload", "errorMsg", "businessNo"));

            SearchResponse<IntegrationMessageDocument> resp =
                    client.search(
                            buildSearchRequest(INDEX_INTEGRATION_MESSAGE, req, query),
                            IntegrationMessageDocument.class);

            List<IntegrationMessageDocument> items =
                    resp.hits().hits().stream()
                            .map(hit -> hit.source())
                            .collect(Collectors.toList());

            return new EsSearchResponse<>(
                    items,
                    resp.hits().total() != null ? resp.hits().total().value() : 0,
                    req.getPage(),
                    req.getSize());
        } catch (Exception e) {
            log.error("搜索集成消息流水失败: {}", e.getMessage());
            return new EsSearchResponse<>(List.of(), 0, req.getPage(), req.getSize());
        }
    }

    // ============================================================
    // 回调记录搜索
    // ============================================================

    public EsSearchResponse<CallbackRecordDocument> searchCallbacks(EsSearchRequest req) {
        try {
            ElasticsearchClient client = esSyncClient.getClient();
            Query query =
                    buildQuery(
                            req, List.of("requestBody", "responseBody", "errorMsg", "businessNo"));

            SearchResponse<CallbackRecordDocument> resp =
                    client.search(
                            buildSearchRequest(INDEX_CALLBACK_RECORD, req, query),
                            CallbackRecordDocument.class);

            List<CallbackRecordDocument> items =
                    resp.hits().hits().stream()
                            .map(hit -> hit.source())
                            .collect(Collectors.toList());

            return new EsSearchResponse<>(
                    items,
                    resp.hits().total() != null ? resp.hits().total().value() : 0,
                    req.getPage(),
                    req.getSize());
        } catch (Exception e) {
            log.error("搜索回调记录失败: {}", e.getMessage());
            return new EsSearchResponse<>(List.of(), 0, req.getPage(), req.getSize());
        }
    }

    // ============================================================
    // 跨索引聚合搜索
    // ============================================================

    public EsSearchResponse<Map<String, Object>> searchAll(EsSearchRequest req) {
        try {
            ElasticsearchClient client = esSyncClient.getClient();
            Query query = buildQuery(req, FULLTEXT_FIELDS);

            // 日志索引搜索
            Map<String, Object> logResult =
                    searchSingleIndex(client, INDEX_API_CALL_LOG, req, query, "log");

            // 消息索引搜索
            Map<String, Object> msgResult =
                    searchSingleIndex(client, INDEX_INTEGRATION_MESSAGE, req, query, "message");

            // 回调索引搜索
            Map<String, Object> cbResult =
                    searchSingleIndex(client, INDEX_CALLBACK_RECORD, req, query, "callback");

            List<Map<String, Object>> allItems = new ArrayList<>();
            allItems.addAll((List<Map<String, Object>>) logResult.getOrDefault("items", List.of()));
            allItems.addAll((List<Map<String, Object>>) msgResult.getOrDefault("items", List.of()));
            allItems.addAll((List<Map<String, Object>>) cbResult.getOrDefault("items", List.of()));

            long total =
                    (Long) logResult.getOrDefault("total", 0L)
                            + (Long) msgResult.getOrDefault("total", 0L)
                            + (Long) cbResult.getOrDefault("total", 0L);

            return new EsSearchResponse<>(allItems, total, req.getPage(), req.getSize());
        } catch (Exception e) {
            log.error("跨索引聚合搜索失败: {}", e.getMessage());
            return new EsSearchResponse<>(List.of(), 0, req.getPage(), req.getSize());
        }
    }

    // ============================================================
    // 内部方法
    // ============================================================

    /** 构建搜索请求 */
    private <T> SearchRequest buildSearchRequest(String index, EsSearchRequest req, Query query) {
        int from = req.getPage() * req.getSize();
        String sortField = defaultIfBlank(req.getSortField(), "createdTime");
        String sortOrder = defaultIfBlank(req.getSortOrder(), "desc");

        SearchRequest.Builder builder =
                new SearchRequest.Builder()
                        .index(index)
                        .query(query)
                        .from(from)
                        .size(req.getSize())
                        .sort(
                                sort ->
                                        sort.field(
                                                f ->
                                                        f.field(sortField)
                                                                .order(
                                                                        sortOrder.equals("asc")
                                                                                ? SortOrder.Asc
                                                                                : SortOrder.Desc)));
        return builder.build();
    }

    /**
     * 构建布尔查询
     *
     * <p>组合：
     *
     * <ul>
     *   <li>keyword → multi_match（全文模糊匹配）
     *   <li>systemCode / status / businessType / traceId / direction → term（精确匹配）
     *   <li>businessNo → match（全文匹配）
     *   <li>startTime / endTime → range（时间范围）
     * </ul>
     */
    private Query buildQuery(EsSearchRequest req, List<String> textFields) {
        BoolQuery.Builder boolBuilder = new BoolQuery.Builder();

        // 全文关键词
        if (req.getKeyword() != null && !req.getKeyword().isBlank()) {
            String kw = req.getKeyword();
            boolBuilder.must(
                    m ->
                            m.multiMatch(
                                    mm ->
                                            mm.query(kw)
                                                    .fields(textFields)
                                                    .type(TextQueryType.BestFields)));
        }

        // 精确过滤
        if (req.getSystemCode() != null && !req.getSystemCode().isBlank()) {
            boolBuilder.filter(f -> f.term(t -> t.field("systemCode").value(req.getSystemCode())));
        }
        if (req.getStatus() != null && !req.getStatus().isBlank()) {
            boolBuilder.filter(f -> f.term(t -> t.field("status").value(req.getStatus())));
        }
        if (req.getBusinessType() != null && !req.getBusinessType().isBlank()) {
            boolBuilder.filter(
                    f -> f.term(t -> t.field("businessType").value(req.getBusinessType())));
        }
        if (req.getDirection() != null && !req.getDirection().isBlank()) {
            boolBuilder.filter(f -> f.term(t -> t.field("direction").value(req.getDirection())));
        }
        if (req.getTraceId() != null && !req.getTraceId().isBlank()) {
            boolBuilder.filter(f -> f.term(t -> t.field("traceId").value(req.getTraceId())));
        }
        if (req.getBusinessNo() != null && !req.getBusinessNo().isBlank()) {
            boolBuilder.filter(f -> f.match(m -> m.field("businessNo").query(req.getBusinessNo())));
        }

        // 时间范围
        if (req.getStartTime() != null && !req.getStartTime().isBlank()
                || (req.getEndTime() != null && !req.getEndTime().isBlank())) {
            String start = req.getStartTime() != null ? req.getStartTime() : "";
            String end = req.getEndTime() != null ? req.getEndTime() : "";
            boolBuilder.filter(
                    f ->
                            f.range(
                                    r ->
                                            r.field("createdTime")
                                                    .gte(
                                                            start.isEmpty()
                                                                    ? null
                                                                    : JsonData.of(start))
                                                    .lte(end.isEmpty() ? null : JsonData.of(end))));
        }

        return Query.of(q -> q.bool(boolBuilder.build()));
    }

    /** 搜索单个索引，返回带来源标记的结果 */
    private Map<String, Object> searchSingleIndex(
            ElasticsearchClient client,
            String index,
            EsSearchRequest req,
            Query query,
            String source)
            throws Exception {
        @SuppressWarnings("rawtypes")
        SearchResponse<Map> resp =
                client.search(buildSearchRequest(index, req, query), Map.class);

        List<Map<String, Object>> items =
                resp.hits().hits().stream()
                        .map(
                                hit -> {
                                    Map<String, Object> item = new HashMap<>();
                                    item.put("source", source);
                                    item.put("index", index);
                                    if (hit.source() != null) item.put("data", hit.source());
                                    return item;
                                })
                        .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("items", items);
        result.put("total", resp.hits().total() != null ? resp.hits().total().value() : 0);
        return result;
    }

    private String defaultIfBlank(String value, String defaultVal) {
        return (value == null || value.isBlank()) ? defaultVal : value;
    }
}
